require('dotenv').config();
const { chromium } = require('playwright');
const WebSocket = require('ws');
const axios = require('axios');

const CHAT_WEBSOCKET_URL = 'wss://kr-ss1.chat.naver.com/chat';
const CHAT_ORIGIN = 'https://chzzk.naver.com';
const PING_INTERVAL_MS = 60000;
const COLLECTOR_TIMEOUT_MS = 30000;

async function collectChatsForStreamEvent(channelId, streamEventId) {
  try {
    const joinPayload = await getJoinPayload(channelId);
    if (!joinPayload) {
      throw new Error('joinPayload 획득 실패');
    }
    const messages = await collectChatsOverWebSocket(joinPayload, channelId);

    await sendChatCollectionToBackend(channelId, streamEventId, messages);
  } catch (error) {
    console.error(`채팅 수집 프로세스 실행 실패: ${error.message}`);
  }
}

async function getJoinPayload(channelId) {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  const joinPayload = await new Promise((resolve) => {
    page.once('websocket', ws => {
      ws.once('framesent', frame => {
        try {
          const msg = JSON.parse(frame.payload);
          if (msg.cmd === 100) {
            resolve(msg);
          }
        } catch (err) {
          console.error(`joinPayload 파싱 오류: ${err.message}`);
        }
      });
    });
    page.goto(`https://chzzk.naver.com/live/${channelId}`, {
      timeout: 60000,
      waitUntil: 'domcontentloaded'
    });
  });

  await page.close();
  await browser.close();

  return joinPayload;
}

function collectChatsOverWebSocket(joinPayload, channelId) {
  return new Promise((resolve) => {
    let ws, pingInterval, sid;
    const messages = [];

    const createSubscribePayload = () => JSON.stringify({
      svcid: 'game',
      ver: '1',
      cmd: 93101,
      cid: joinPayload.cid,
      sid,
      tid: 3,
      bdy: []
    });

    const createRecentMessagePayload = () => JSON.stringify({
      svcid: 'game',
      ver: '3',
      cmd: 5101,
      cid: joinPayload.cid,
      sid,
      tid: 2,
      bdy: { recentMessageCount: 50 }
    });

    const endCollector = async () => {
      clearInterval(pingInterval);
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.close();
      }
      resolve(messages);
    };

    ws = new WebSocket(CHAT_WEBSOCKET_URL, { origin: CHAT_ORIGIN });

    ws.on('open', () => {
      ws.send(JSON.stringify(joinPayload));
    });

    ws.on('message', (data) => {
      try {
        const parsed = JSON.parse(data);
        const { svcid, cmd, retCode, bdy } = parsed;

        if (cmd === 10100 && retCode === 0) {
          sid = bdy.sid;
          ws.send(createRecentMessagePayload());
        }

        if (cmd === 5101) {
          ws.send(createSubscribePayload());
        }

        if (svcid === 'game' && cmd === 93101 && Array.isArray(bdy)) {
          bdy.forEach(chat => {
            const chatMessage = chat.content || chat.msg;
            if (chatMessage) {
              console.log(chatMessage);
              messages.push(chatMessage);
            }
          });
        }
      } catch (err) {
        console.error(`채팅 파싱 오류: ${err.message}`);
      }
    });

    ws.on('error', (err) => {
      console.error(`WebSocket 오류: ${err.message}`);
    });

    ws.on('close', () => {
      clearInterval(pingInterval);
    });

    pingInterval = setInterval(() => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.ping();
      }
    }, PING_INTERVAL_MS);

    setTimeout(endCollector, COLLECTOR_TIMEOUT_MS);
  });
}

async function sendChatCollectionToBackend(channelId, streamEventId, messages) {
  const payload = { messages };
  try {
    await axios.post(`${process.env.BACKEND_BASE_URL}/api/chat/${channelId}/${streamEventId}`, payload);
    console.log(`최종 HTTP 요청 전송 완료 (채널: ${channelId}, 하이라이트: ${streamEventId})`);
  } catch (error) {
    console.error(`HTTP 요청 오류: ${error.message}`);
  }
}

module.exports = { collectChatsForStreamEvent };
