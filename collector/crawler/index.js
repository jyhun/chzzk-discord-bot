require('dotenv').config();
const { chromium } = require('playwright');
const WebSocket = require('ws');

const channelId = process.env.CHANNEL_ID;
if (!channelId) {
  console.error('Error: CHANNEL_ID 환경변수가 설정되지 않았습니다.');
  process.exit(1);
}

(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  const joinPayload = await new Promise(resolve => {
    page.on('websocket', ws => {
      ws.on('framesent', frame => {
        try {
          const msg = JSON.parse(frame.payload);
          if (msg.cmd === 100) resolve(JSON.stringify(msg));
        } catch {}
      });
    });
    page.goto(`https://chzzk.naver.com/live/${channelId}`);
  });
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

  function connect() {
    ws = new WebSocket('wss://kr-ss1.chat.naver.com/chat', {
      origin: 'https://chzzk.naver.com'
    });

    ws.on('open', () => {
      console.log('▶ WS opened — subscribing');
      ws.send(joinPayload);
      ws.send(subscribePayload);

      pingInterval = setInterval(() => {
        if (ws.readyState === WebSocket.OPEN) ws.ping();
      }, 30000);
    });

    ws.on('pong', () => console.log('▶ Pong received'));

    ws.on('message', data => {
      try {
        const { svcid, cmd, bdy } = JSON.parse(data);
        if (svcid === 'game' && cmd === 93101 && Array.isArray(bdy)) {
          bdy.forEach(chat => {
            const timestamp = chat.msgTime || chat.messageTime || chat.createTime;           
            const currentTime = new Date(timestamp).toISOString();

            const output = {
              channelId: channelId,
              nickname: JSON.parse(chat.profile).nickname,
              message: chat.content || chat.msg,
              currentTime: currentTime
            };
            console.log(JSON.stringify(output));
          });
        }
      } catch (err) {
        console.error(err);
      }
    });

    ws.on('error', err => console.error('‼ WS error:', err.message));

    ws.on('close', () => {
      clearInterval(pingInterval);
      setTimeout(connect, 1000);
    });
  }

  connect();
})();
