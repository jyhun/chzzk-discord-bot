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

    if (commandName === 'help') {
      try {
        await interaction.reply({
          content: '사용 가능한 명령어:\n' +
            '/help - 명령어 도움말\n' +
            '/hot all - 전체 방송자 실시간 급상승 감지 구독\n' +
            '/hot <방송자 채널ID> - 특정 방송자 실시간 급상승 감지 구독\n',
          flags: MessageFlags.Ephemeral
        });
      } catch (error) {
        console.error('[Help Command] 오류:', error);
      }
    }

    if (commandName === 'hot') {
      const target = options.getString('target');

      console.info(`[Hot Command] 요청 채널: ${channel.name} (${channelId}), target: ${target}`);

      if (!target) {
        return interaction.reply({
          content: '구독 대상을 입력해주세요! (예: all 또는 방송자 채널ID)',
          flags: MessageFlags.Ephemeral
        });
      }

      // target 이 all 이면 전체 방송자 구독, 아니면 특정 방송자 구독
      const streamerId = target.toLowerCase() === 'all' ? null : target;

      try {
        await axios.post(process.env.BACKEND_BASE_URL + '/api/subscriptions', {
          discordGuildId: interaction.guildId,
          discordChannelId: interaction.channelId,
          streamerId,
          eventType: 'HOT',
          keyword: null
        });

        const successMessage = streamerId
          ? `채널 **${streamerId}** 실시간 급상승 알림 구독이 완료되었습니다.`
          : '전체 방송자 실시간 급상승 알림 구독이 완료되었습니다.';

        await interaction.reply({
          content: successMessage,
          flags: MessageFlags.Ephemeral
        });
      } catch (error) {
        console.error('[Hot Command] 오류:', error.response?.data || error.message);

        await interaction.reply({
          content: '구독 처리 중 오류가 발생했습니다.',
          flags: MessageFlags.Ephemeral
        });
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
