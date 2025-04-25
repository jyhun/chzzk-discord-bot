require('dotenv').config();
const { Client, GatewayIntentBits, REST, Routes, Events, MessageFlags } = require('discord.js');
const commands = require('./commands');
const axios = require('axios');

const client = new Client({ intents: [GatewayIntentBits.Guilds] });
const VALID_EVENT_TYPES = ['HOT', 'START', 'END', 'TOPIC'];

// 공통 에러 핸들러
async function handleError(interaction, context, error) {
  // 1. 기본 메시지 초기화
  let errorMessage = '알 수 없는 오류가 발생했습니다.';

  // 2. 에러 응답 메시지를 파싱
  if (error.response?.data) {
    const data = error.response.data;

    // 객체인 경우
    if (typeof data === 'object') {
      // 백엔드에서 { error: '메시지' } 형식으로 주는 경우
      errorMessage = data.error || JSON.stringify(data);
    } else {
      errorMessage = data;
    }
  } else if (error.message) {
    errorMessage = error.message;
  }

  // 3. 로그 출력
  console.error(`[${context}] 오류:`, errorMessage);

  // 4. 사용자에게 응답
  const replyPayload = {
    content: `⚠️ 오류가 발생했습니다:\n\`\`\`${errorMessage}\`\`\``,
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
      Routes.applicationCommands(process.env.DISCORD_CLIENT_ID),
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
    const keyword = options.getString('keyword');
    const streamerId = target || null;

    console.info(`[Command] ${commandName} 요청: 채널 ${channel.name} (${channelId}), eventType: ${rawEventType}, target: ${target}, keyword: ${keyword}`);

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
            '📖 사용 가능한 명령어 안내',
            '',
            '✅ **구독 명령어** (`/subscribe`)',
            '- `/subscribe HOT` : 전체 방송자 급상승(HOT) 감지 구독',
            '- `/subscribe HOT <채널ID>` : 특정 방송자 HOT 감지 구독',
            '- `/subscribe START` : 전체 방송자 방송 시작 감지 구독',
            '- `/subscribe START <채널ID>` : 특정 방송자 방송 시작 구독',
            '- `/subscribe END` : 전체 방송자 방송 종료 감지 구독',
            '- `/subscribe END <채널ID>` : 특정 방송자 방송 종료 구독',
            '- `/subscribe TOPIC <채널ID> <키워드>` : 특정 방송자 방송 제목, 태그, 카테고리에서 키워드 포함시 감지 구독',
            '- `/subscribe TOPIC <키워드>` : 전체 방송자 방송 제목, 태그, 카테고리에서 키워드 포함시 감지 구독',
            '',
            '🚫 **구독 해제 명령어** (`/unsubscribe`)',
            '- `/unsubscribe` : 모든 구독 해제',
            '- `/unsubscribe HOT` : 전체 HOT 구독 해제',
            '- `/unsubscribe HOT <채널ID>` : 특정 방송자의 HOT 구독 해제',
            '- `/unsubscribe START` / `END` / `TOPIC` 도 위와 동일한 방식으로 해제 가능',
            '',
            '📋 **구독 조회 명령어** (`/subscriptions`)',
            '- `/subscriptions` : 현재 채널의 전체 구독 목록 조회',
            '- `/subscriptions HOT` : HOT 구독 목록만 조회',
            '- `/subscriptions START <채널ID>` : 특정 방송자의 START 구독 여부 조회',
            '- `/subscriptions TOPIC <채널ID>` : TOPIC 키워드 구독도 포함하여 확인 가능',
            '',
            'ℹ️ 키워드는 `TOPIC` 이벤트에서만 사용됩니다. 키워드는 제목, 태그, 카테고리에 포함될 경우 감지됩니다.',
            '',
            '예시) `/subscribe TOPIC streamer123 롤` → streamer123의 제목/카테고리/태그에 "롤"이 포함되면 알림'
          ].join('\n'),
          flags: MessageFlags.Ephemeral
        });
      }

      // subscribe
      else if (commandName === 'subscribe') {
        if (eventType === 'TOPIC' && (!keyword || keyword.trim() === '')) {
          return await interaction.reply({
            content: `TOPIC 이벤트는 키워드가 필수입니다. 예: \`/subscribe TOPIC <채널ID> <키워드>\` 또는 \`/subscribe TOPIC <키워드>\``,
            flags: MessageFlags.Ephemeral,
          });
        }
        const singleKeyword = keyword?.trim();
        await axios.post(`${process.env.BACKEND_BASE_URL}/api/subscriptions`, {
          discordGuildId: interaction.guildId,
          discordChannelId: interaction.channelId,
          streamerId,
          eventType,
          keyword: eventType === 'TOPIC' ? singleKeyword : null,
        });

        let message = '';
        if (eventType === 'TOPIC') {
          message = streamerId
            ? `방송자 **${streamerId}** 의 TOPIC 키워드 \`${singleKeyword}\` 알림 구독이 완료되었습니다.`
            : `전체 방송자의 TOPIC 키워드 \`${singleKeyword}\` 알림 구독이 완료되었습니다.`;
        } else {
          message = streamerId
            ? `방송자 **${streamerId}** 의 ${eventType} 알림 구독이 완료되었습니다.`
            : `전체 방송자의 ${eventType} 알림 구독이 완료되었습니다.`;
        }
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
      // 기존 /subscriptions 명령어 처리 부분 수정
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
          const parts = [];

          const event = sub.eventType || 'Unknown';
          parts.push(event);

          const streamer = sub.streamerId ? `방송자 채널ID: ${sub.streamerId}` : '전체 방송자';
          parts.push(streamer);

          if (event === 'TOPIC' && sub.keywords && sub.keywords.length > 0) {
            parts.push(`키워드: ${sub.keywords.join(', ')}`);
          }

          return `- ${parts.join(' / ')}`;
        }).join('\n');

        await interaction.reply({
          content: `📋 현재 구독 목록:\n${list}`,
          flags: MessageFlags.Ephemeral,
        });
      }


    } catch (error) {
      if (commandName === 'subscribe' && error.response?.status === 409) {
        const msg = error.response.data.error || '오류가 발생했습니다.';
        return await interaction.reply({
          content: msg,
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
