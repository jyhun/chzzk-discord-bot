require('dotenv').config();
const { Client, GatewayIntentBits, REST, Routes, Events, MessageFlags } = require('discord.js');
const commands = require('./commands');
const axios = require('axios');

const client = new Client({ intents: [GatewayIntentBits.Guilds] });

async function registerCommands() {
  const rest = new REST({ version: '10' }).setToken(process.env.DISCORD_BOT_TOKEN);
  try {
    console.info('[Command Registration] 시작...');
    await rest.put(
      Routes.applicationGuildCommands(
        process.env.DISCORD_CLIENT_ID,
        process.env.DISCORD_GUILD_ID
      ),
      { body: commands.map(cmd => cmd.toJSON()) }
    );
    console.info('[Command Registration] 완료');
  } catch (error) {
    console.error('[Command Registration] 실패:', error);
    throw error;
  }
}

// 공통 error handler
async function handleError(interaction, context, error) {
  const errorMessage = error.response?.data || error.message;
  console.error(`[${context}] 오류:`, errorMessage);

  await interaction.reply({
    content: `오류가 발생했습니다: ${errorMessage}`,
    flags: MessageFlags.Ephemeral
  });
}

async function startBot() {
  try {
    await registerCommands();
  } catch (error) {
    console.error('[Startup] 등록 중 오류:', error);
  }

  client.once(Events.ClientReady, () => {
    console.info(`[Bot] 로그인 완료: ${client.user.tag}`);
  });

  client.on(Events.InteractionCreate, async interaction => {
    if (!interaction.isCommand()) return;

    const { commandName, options, channel, channelId } = interaction;
    console.info(`[Command] ${commandName} 요청: 채널 ${channel.name} (${channelId})`);

    // help
    if (commandName === 'help') {
      try {
        await interaction.reply({
          content: '사용 가능한 명령어:\n' +
            '/help - 명령어 도움말\n' +
            '/hot - 전체 방송자 급상승 알림 구독\n' +
            '/hot <방송자 채널ID> - 특정 방송자 급상승 알림 구독\n' +
            '/unsubscribe - 전체 구독 해제\n' +
            '/unsubscribe HOT - HOT 이벤트 전체 구독 해제\n' +
            '/unsubscribe HOT <방송자 채널ID> - HOT 이벤트 특정 방송자 구독 해제\n',
          flags: MessageFlags.Ephemeral
        });
      } catch (error) {
        await handleError(interaction, 'Help Command', error);
      }
    }

    // hot
    if (commandName === 'hot') {
      const target = options.getString('target'); // optional
      const streamerId = target?.toLowerCase() === 'all' || !target ? null : target;

      try {
        await axios.post(process.env.BACKEND_BASE_URL + '/api/subscriptions', {
          discordGuildId: interaction.guildId,
          discordChannelId: interaction.channelId,
          streamerId,
          eventType: 'HOT',
          keyword: null
        });

        const message = streamerId
          ? `방송자 채널 **${streamerId}** 실시간 급상승 알림 구독이 완료되었습니다.`
          : '전체 방송자 실시간 급상승 알림 구독이 완료되었습니다.';

        await interaction.reply({
          content: message,
          flags: MessageFlags.Ephemeral
        });
      } catch (error) {
        const status = error.response?.status;
        if (status === 409) {
          await interaction.reply({
            content: '이미 구독 중인 대상입니다!',
            flags: MessageFlags.Ephemeral
          });
        } else {
          await handleError(interaction, 'Hot Command', error);
        }
      }
    }

    // unsubscribe
    if (commandName === 'unsubscribe') {
      const event = options.getString('event'); // optional
      const target = options.getString('target'); // optional
      const streamerId = target?.toLowerCase() === 'all' || !target ? null : target;

      try {
        await axios.delete(process.env.BACKEND_BASE_URL + '/api/subscriptions', {
          data: {
            discordGuildId: interaction.guildId,
            discordChannelId: interaction.channelId,
            streamerId,
            eventType: event,
            keyword: null
          }
        });

        let message = '전체 구독이 해제되었습니다.';
        if (event && streamerId) {
          message = `방송자 **${streamerId}** 의 ${event} 알림 구독이 해제되었습니다.`;
        } else if (event) {
          message = `${event} 이벤트 전체 구독이 해제되었습니다.`;
        }

        await interaction.reply({
          content: message,
          flags: MessageFlags.Ephemeral
        });
      } catch (error) {
        await handleError(interaction, 'Unsubscribe Command', error);
      }
    }
  });

  try {
    await client.login(process.env.DISCORD_BOT_TOKEN);
  } catch (error) {
    console.error('[Bot Login] 실패:', error);
  }
}

process.on('unhandledRejection', error => {
  console.error('Unhandled promise rejection:', error);
});

module.exports = startBot;
module.exports.client = client;
