const express = require('express');
const axios = require('axios');
const { EmbedBuilder } = require('discord.js');

function createStreamEndRouter(client) {
  const router = express.Router();

  router.post('/stream-end', async (req, res) => {
    const { streamerId, streamerName, peakViewerCount, averageViewerCount, duration } = req.body;

    if (!streamerId || !streamerName || peakViewerCount == null || averageViewerCount == null || !duration) {
      return res.status(400).json({ error: '필수 항목 누락: streamerId, streamerName, peakViewerCount, averageViewerCount, duration' });
    }

    try {
      const { data: subscribers } = await axios.get(process.env.BACKEND_BASE_URL + '/api/subscriptions', {
        params: { streamerId, eventType: 'END' }
      });

      if (!subscribers.length) return res.json({ message: '알림 대상이 없습니다.' });

      const streamUrl = `https://chzzk.naver.com/live/${streamerId}`;

      const message =
        `[${streamerName}] 방송이 종료되었습니다.\n` +
        `⏱ 총 방송 시간: ${duration}\n` +
        `🔝 최고 시청자 수: ${peakViewerCount}명\n` +
        `👥 평균 시청자 수: ${averageViewerCount}명\n\n` +
        `https://chzzk.naver.com/live/${streamerId}`;

      for (const s of subscribers) {
        try {
          const channel = await client.channels.fetch(s.discordChannelId);
          await channel.send(message);
          console.info(`[END] ${s.discordChannelId} 전송 성공`);
        } catch (e) {
          console.error(`[END] ${s.discordChannelId} 전송 실패`, e.message);
        }
      }

      res.json({ message: '알림 전송 완료' });
    } catch (e) {
      console.error('[END] 처리 오류:', e.response?.data || e.message);
      res.status(500).json({ error: 'END 알림 처리 중 오류' });
    }
  });

  return router;
}

module.exports = createStreamEndRouter;
