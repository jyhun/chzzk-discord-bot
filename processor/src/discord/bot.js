require('dotenv').config();
const { Client, GatewayIntentBits, REST, Routes, Events, MessageFlags } = require('discord.js');
const commands = require('./commands');
const axios = require('axios');

const client = new Client({ intents: [GatewayIntentBits.Guilds] });
const VALID_EVENT_TYPES = ['HOT', 'START', 'END', 'CHANGE'];

// 공통 에러 핸들러
async function handleError(interaction, context, error) {
  const errorMessage = error.response?.data || error.message;
  console.error(`[${context}] 오류:`, errorMessage);

  const replyPayload = {
    content: `오류가 발생했습니다: ${errorMessage}`,
    flags: MessageFlags.Ephemeral,
  };

  if (!interaction.replied) {
    await interaction.reply(replyPayload);
  } else {
    await interaction.followUp(replyPayload);
  }
}

// 이벤트 타입 파싱 함수 (명령어 별 required 여부 적용)
function parseEventType(rawEventType, isRequired = false) {
  if (!rawEventType) {
    if (isRequired) {
      throw new Error(`이벤트 타입이 필요합니다. 사용 가능한 이벤트: ${VALID_EVENT_TYPES.join(', ')}`);
    }
    return null;
  }

  const eventType = rawEventType.toUpperCase();
  if (!VALID_EVENT_TYPES.includes(eventType)) {
    throw new Error(`잘못된 이벤트 타입입니다. 사용 가능한 이벤트: ${VALID_EVENT_TYPES.join(', ')}`);
  }

  return eventType;
}

// 명령어 등록
async function registerCommands() {
  const rest = new REST({ version: '10' }).setToken(process.env.DISCORD_BOT_TOKEN);
  try {
    console.info('[Command Registration] 시작...');
    await rest.put(
      Routes.applicationGuildCommands(process.env.DISCORD_CLIENT_ID, process.env.DISCORD_GUILD_ID),
      { body: commands.map(cmd => cmd.toJSON()) }
    );
    console.info('[Command Registration] 완료');
  } catch (error) {
    console.error('[Command Registration] 실패:', error);
    throw error;
  }
}

// 봇 시작
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
    const rawEventType = options.getString('event');
    const target = options.getString('target');
    const streamerId = target || null;

    console.info(`[Command] ${commandName} 요청: 채널 ${channel.name} (${channelId}), eventType: ${rawEventType}, target: ${target}`);

    // eventType 파싱
    let eventType;
    try {
      const isEventRequired = commandName === 'subscribe';
      eventType = parseEventType(rawEventType, isEventRequired);
    } catch (error) {
      return await interaction.reply({
        content: error.message,
        flags: MessageFlags.Ephemeral,
      });
    }

    try {
      // help
      if (commandName === 'help') {
        await interaction.reply({
          content: [
            '사용 가능한 명령어:',
            '/help - 명령어 도움말',
            '/subscribe HOT - 전체 방송자 실시간 급상승 감지 구독',
            '/subscribe HOT <방송자 채널ID> - 특정 방송자 구독',
            '/subscribe START - 전체 방송자 방송 시작 감지 구독',
            '/subscribe START <방송자 채널ID> - 특정 방송자 방송 시작 구독',
            '/subscribe END - 전체 방송자 방송 종료 감지 구독',
            '/subscribe END <방송자 채널ID> - 특정 방송자 방송 종료 구독',
            '/unsubscribe - 전체 구독 해제',
            '/unsubscribe HOT - HOT 이벤트 전체 구독 해제',
            '/unsubscribe HOT <방송자 채널ID> - HOT 이벤트 특정 방송자 구독 해제',
            '/unsubscribe START - START 이벤트 전체 구독 해제',
            '/unsubscribe START <방송자 채널ID> - START 이벤트 특정 방송자 구독 해제',
            '/unsubscribe END - END 이벤트 전체 구독 해제',
            '/unsubscribe END <방송자 채널ID> - END 이벤트 특정 방송자 구독 해제',
            '/subscriptions - 전체 구독 조회',
            '/subscriptions HOT - HOT 이벤트 구독 조회',
            '/subscriptions START - START 이벤트 구독 조회',
            '/subscriptions END - END 이벤트 구독 조회',
            '/subscriptions HOT <방송자 채널ID> - HOT 이벤트 특정 방송자 구독 조회',
            '/subscriptions START <방송자 채널ID> - START 이벤트 특정 방송자 구독 조회',
            '/subscriptions END <방송자 채널ID> - END 이벤트 특정 방송자 구독 조회',
          ].join('\n'),
          flags: MessageFlags.Ephemeral
        });
      }

      // subscribe
      else if (commandName === 'subscribe') {
        await axios.post(`${process.env.BACKEND_BASE_URL}/api/subscriptions`, {
          discordGuildId: interaction.guildId,
          discordChannelId: interaction.channelId,
          streamerId,
          eventType,
          keyword: null,
        });

        const message = streamerId
          ? `방송자 **${streamerId}** 의 ${eventType} 알림 구독이 완료되었습니다.`
          : `전체 방송자의 ${eventType} 알림 구독이 완료되었습니다.`;

        await interaction.reply({ content: message, flags: MessageFlags.Ephemeral });
      }

      // unsubscribe
      else if (commandName === 'unsubscribe') {
        await axios.delete(`${process.env.BACKEND_BASE_URL}/api/subscriptions`, {
          data: {
            discordGuildId: interaction.guildId,
            discordChannelId: interaction.channelId,
            streamerId,
            eventType,
            keyword: null,
          }
        });

        let message;
        if (!eventType && !streamerId) {
          message = `전체 구독이 해제되었습니다.`;
        } else if (eventType && !streamerId) {
          message = `${eventType} 이벤트 전체 구독이 해제되었습니다.`;
        } else {
          message = `방송자 **${streamerId}** 의 ${eventType} 알림 구독이 해제되었습니다.`;
        }

        await interaction.reply({ content: message, flags: MessageFlags.Ephemeral });
      }

      // subscriptions
      else if (commandName === 'subscriptions') {
        const response = await axios.get(`${process.env.BACKEND_BASE_URL}/api/subscriptions`, {
          params: {
            discordChannelId: interaction.channelId,
            eventType,
            streamerId,
          }
        });

        const subscriptions = response.data;

        if (!subscriptions.length) {
          return await interaction.reply({
            content: '현재 구독 중인 목록이 없습니다.',
            flags: MessageFlags.Ephemeral,
          });
        }

        const list = subscriptions.map(sub => {
          const event = sub.eventType || 'Unknown';
          const streamer = sub.streamerId ? `방송자 채널ID: ${sub.streamerId}` : '전체 방송자';
          return `- ${event} / ${streamer}`;
        }).join('\n');

        await interaction.reply({
          content: `📋 현재 구독 목록:\n${list}`,
          flags: MessageFlags.Ephemeral,
        });
      }

    } catch (error) {
      if (commandName === 'subscribe' && error.response?.status === 409) {
        return await interaction.reply({
          content: '이미 구독 중인 대상입니다!',
          flags: MessageFlags.Ephemeral
        });
      }

      await handleError(interaction, `${commandName} Command`, error);
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
