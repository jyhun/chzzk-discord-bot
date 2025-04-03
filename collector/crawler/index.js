require('dotenv').config();
const { chromium } = require('playwright');
const WebSocket = require('ws');
const axios = require('axios');

const baseUrl = process.env.BASE_URL;
const channelId = process.argv[2];

if (!channelId) {
  console.error('Error: CHANNEL_ID가 인자로 전달되지 않았습니다.');
  process.exit(1);
}

// 일정 시간 동안 누적된 채팅의 message들을 저장할 배열
let messages = [];

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();

  // 웹소켓 연결 시 최초로 전송되는 joinPayload를 획득
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

  let pingInterval, ws;

  // 일정시간 이후에 message 배열을 포함하는 HTTP POST 요청을 전송하고 프로세스 종료
  setTimeout(async () => {
    const payload = {
      messages: messages
    };
    try {
      await axios.post(`${baseUrl}/api/chat/${channelId}`, payload);
      console.log('최종 HTTP 요청 전송 완료:', payload);
    } catch (error) {
      console.error('HTTP 요청 오류:', error.message);
    }
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.close();
    }
    process.exit(0);
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
            // 채팅의 message 값 추출 (chat.content 또는 chat.msg)
            const chatMessage = chat.content || chat.msg;
            if (chatMessage) {
              messages.push(chatMessage);
            }
            // 로그 출력을 위한 정보
            const timestamp = chat.msgTime || chat.messageTime || chat.createTime;
            const currentTime = new Date(timestamp).toISOString();
            const output = {
              channelId: channelId,
              message: chatMessage,
              currentTime: currentTime
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
})();
