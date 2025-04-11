require('dotenv').config();
const { chromium } = require('playwright');
const WebSocket = require('ws');
const axios = require('axios');

async function collectChatsForHighlight(channelId, highlightId) {
  try {
    const browser = await chromium.launch({ headless: true });
    const page = await browser.newPage();

    // joinPayload 확보 (빠르게 종료)
    const joinPayload = await new Promise((resolve, reject) => {
      const timeout = setTimeout(() => reject(new Error('joinPayload timeout')), 10000);
      page.once('websocket', ws => {
        ws.once('framesent', frame => {
          try {
            const msg = JSON.parse(frame.payload);
            if (msg.cmd === 100) {
              clearTimeout(timeout);
              resolve(JSON.stringify(msg));
            }
          } catch (err) {
            clearTimeout(timeout);
            reject(err);
          }
        });
      });
      page.goto(`https://chzzk.naver.com/live/${channelId}`, { timeout: 30000, waitUntil: 'domcontentloaded' });
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

    let ws;
    const messages = [];

    const endCollector = async () => {
      try {
        const payload = { messages };
        if (messages.length > 0) {
          await axios.post(`${process.env.BACKEND_BASE_URL}/api/chat/${channelId}/${highlightId}`, payload);
          console.log(`채팅 ${messages.length}개 전송 완료.`);
        } else {
          console.log('수집된 채팅 없음.');
        }
      } catch (error) {
        console.error('HTTP 요청 오류:', error.message);
      } finally {
        if (ws && ws.readyState === WebSocket.OPEN) {
          ws.close();
        }
      }
    };

    ws = new WebSocket('wss://kr-ss1.chat.naver.com/chat', {
      origin: 'https://chzzk.naver.com'
    });

    const collectorTimeout = setTimeout(endCollector, 30000); // 30초 수집

    ws.on('open', () => {
      ws.send(joinPayload);
      ws.send(subscribePayload);

      const pingInterval = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) ws.ping();
      }, 60000); // Ping 간격 60초

      ws.on('close', () => clearInterval(pingInterval));
    });

    ws.on('message', data => {
      try {
        const { svcid, cmd, bdy } = JSON.parse(data);
        if (svcid === 'game' && cmd === 93101 && Array.isArray(bdy)) {
          for (const chat of bdy) {
            const chatMessage = chat.content || chat.msg;
            if (chatMessage) {
              messages.push(chatMessage);
            }
          }
        }
      } catch {
        // silent fail (성능 위해)
      }
    });

    ws.on('error', err => {
      console.error('WebSocket 오류:', err.message);
    });

  } catch (e) {
    console.error(`채팅 수집 실패: ${e.message}`);
  }
}

module.exports = { collectChatsForHighlight };
