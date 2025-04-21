require('dotenv').config();
const { exec } = require('child_process');

exec('pkill -f headless_shell', (err) => {
  if (err) {
    console.log('[init] headless_shell 종료 실패 또는 없음');
  } else {
    console.log('[init] 기존 headless_shell 프로세스 정리 완료');
  }
});

const express = require('express');
const bodyParser = require('body-parser');
const { collectChatsForStreamEvent } = require('./crawler/crawler');
const startBot = require('./discord/bot');
const { client } = require('./discord/bot');
const createStreamHotRouter = require('./discord/stream-hot');
const createStreamStartRouter = require('./discord/stream-start');
const createStreamEndRouter = require('./discord/stream-end');
const createStreamTopicRouter = require('./discord/stream-topic');

const app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));
app.use('/api', createStreamHotRouter(client));
app.use('/api', createStreamStartRouter(client));
app.use('/api', createStreamEndRouter(client));
app.use('/api', createStreamTopicRouter(client));

// /crawler API: 채널 ID와 하이라이트 ID를 받아 크롤러를 실행합니다.
app.post('/crawler', async (req, res) => {
  const { channelId, streamEventId } = req.body;
  if (!channelId || !streamEventId) {
    return res.status(400).json({ error: 'channelId와 streamEventId는 필수입니다.' });
  }

  try {
    // 크롤링을 백그라운드로 실행 (비동기로)
    collectChatsForStreamEvent(channelId, streamEventId)
      .then(() => console.log('채팅 크롤링 프로세스가 종료되었습니다.'))
      .catch(err => console.error('크롤링 에러:', err.message));
    
    res.json({ message: '채팅 크롤링을 시작합니다.' });
  } catch (error) {
    console.error('채팅 수집 API 에러:', error.message);
    res.status(500).json({ error: '채팅 수집 실행 실패' });
  }
});

app.listen(3001, () => {
  console.log('processor 서버 실행 시작 (포트: 3001)');
});

startBot();
