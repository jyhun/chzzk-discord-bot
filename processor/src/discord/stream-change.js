const express = require('express');

function createChangeRouter(client) {
  const router = express.Router();

  router.post('/stream-change', async (req, res) => {
    const { streamerId, discordChannelId, keywords, title, category, tags } = req.body;

    console.info(`[CHANGE] 변경 감지 요청 수신: ${streamerId}, 채널: ${discordChannelId}`);

    try {
      const keywordList = (keywords || []).map(k => `\`${k}\``).join(', ') || 'N/A';
      const tagStr = (tags || []).join(', ') || '태그 없음';

      const message =
        `📢 방송 정보 변경 감지됨!\n\n` +
        `📺 방송자 채널 ID: ${streamerId}\n` +
        `📝 감지된 키워드: ${keywordList}\n` +
        `🏷️ 제목: ${title || '제목 없음'}\n` +
        `🎮 카테고리: ${category || '카테고리 없음'}\n` +
        `🏷️ 태그: ${tagStr}\n` +
        `https://chzzk.naver.com/live/${streamerId}`;

      const channel = await client.channels.fetch(discordChannelId);
      await channel.send(message);

      console.info(`[CHANGE] 알림 전송 성공: ${discordChannelId}`);
      return res.json({ message: '알림 전송 완료' });
    } catch (error) {
      console.error(`[CHANGE] 알림 전송 실패: ${discordChannelId}`, error.message);
      return res.status(500).json({ error: '알림 전송 실패' });
    }
  });

  return router;
}

module.exports = createChangeRouter;
