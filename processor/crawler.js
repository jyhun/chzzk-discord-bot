// crawler.js
require('dotenv').config();
const { chromium } = require('playwright');
const WebSocket = require('ws');
const axios = require('axios');

/**
 * 채팅 크롤링 및 수집 후 HTTP POST 전송 기능.
 * @param {string} channelId - 스트리머 채널 ID.
 * @param {string|number} highlightId - 하이라이트 ID.
 * @returns {Promise<void>}
 */
async function collectChatsForHighlight(channelId, highlightId) {
  try {
    // Playwright를 사용해 크롬 브라우저 실행하여 joinPayload 획득
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();

    const joinPayload = await new Promise(resolve => {
      page.once('websocket', ws => {
        ws.once('framesent', frame => {
          try {
            const msg = JSON.parse(frame.payload);
            if (msg.cmd === 100) {
              resolve(JSON.stringify(msg));
            }
          } catch (err) {
            console.error('joinPayload 파싱 오류:', err.message);
          }
        });
      });
      page.goto(`https://chzzk.naver.com/live/${channelId}`);
    });

    await page.close();
    await browser.close();

    const { cid } = JSON.parse(joinPayload);
    const subscribePayload = JSON.stringify({
      svcid: 'game',
      ver: '1',
      cmd: 93101,
      cid,
      tid: 3,
      bdy: []
    });

    let pingInterval;
    let ws;
    const messages = [];

    // 30초 후에 HTTP POST 요청으로 수집한 메시지 전송 (백그라운드 동작)
    setTimeout(async () => {
      const payload = { messages };
      try {
        await axios.post(`${process.env.BACKEND_BASE_URL}/api/chat/${channelId}/${highlightId}`, payload);
        console.log('최종 HTTP 요청 전송 완료:', payload);
      } catch (error) {
        console.error('HTTP 요청 오류:', error.message);
      }
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.close();
      }
      // process.exit() 호출은 Express 서버와 같이 동작할 경우 제거합니다.
    }, 30000);

    function connect() {
      ws = new WebSocket('wss://kr-ss1.chat.naver.com/chat', {
        origin: 'https://chzzk.naver.com'
      });

      ws.on('open', () => {
        ws.send(joinPayload);
        ws.send(subscribePayload);

        pingInterval = setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.ping();
          }
        }, 30000);
      });

      ws.on('message', data => {
        try {
          const { svcid, cmd, bdy } = JSON.parse(data);
          if (svcid === 'game' && cmd === 93101 && Array.isArray(bdy)) {
            bdy.forEach(chat => {
              const chatMessage = chat.content || chat.msg;
              if (chatMessage) {
                messages.push(chatMessage);
              }
              const timestamp = chat.msgTime || chat.messageTime || chat.createTime;
              const currentTime = new Date(timestamp).toISOString();
              const output = {
                channelId,
                message: chatMessage,
                currentTime
              };
              console.log('채팅 수신:', JSON.stringify(output));
            });
          }
        } catch (err) {
          console.error('채팅 파싱 오류:', err.message);
        }
      });

      ws.on('error', err => {
        console.error('WebSocket 오류:', err.message);
      });

      ws.on('close', () => {
        clearInterval(pingInterval);
      });
    }

    connect();
  } catch (e) {
    throw new Error(`채팅 수집 프로세스 실행 실패: ${e.message}`);
  }
}

module.exports = { collectChatsForHighlight };
