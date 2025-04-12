// src/index.js
require('dotenv').config();
const express = require('express');
const bodyParser = require('body-parser');
const { collectChatsForHighlight } = require('./crawler/crawler');

const app = express();

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// /crawler API: 채널 ID와 하이라이트 ID를 받아 크롤러를 실행합니다.
app.post('/crawler', async (req, res) => {
  const { channelId, highlightId } = req.body;
  if (!channelId || !highlightId) {
    return res.status(400).json({ error: 'channelId와 highlightId는 필수입니다.' });
  }

  try {
    // 크롤링을 백그라운드로 실행 (비동기로)
    collectChatsForHighlight(channelId, highlightId)
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
