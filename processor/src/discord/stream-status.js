const express = require('express');
const axios = require('axios');
const { EmbedBuilder } = require('discord.js');

function createStreamStatusRouter(client) {
  const router = express.Router();

  router.post('/stream-status', async (req, res) => {
    const { streamerId, eventType } = req.body;

    console.info(`[Stream Status] ${eventType} 감지됨 - 방송자 ID: ${streamerId}`);

    if (!streamerId || !eventType) {
      return res.status(400).json({ error: 'streamerId와 eventType은 필수입니다.' });
    }

    try {
      // 1. 구독자 목록 요청
      const response = await axios.get(process.env.BACKEND_BASE_URL + '/api/subscriptions', {
        params: {
          streamerId,
          eventType
        }
      });

      const subscribers = response.data;
      console.info(`[Stream Status] ${subscribers.length}명의 구독자에게 알림 전송 시도`);

      if (subscribers.length === 0) {
        return res.json({ message: '알림 대상이 없습니다.' });
      }

      for (const subscriber of subscribers) {
        try {
          const channel = await client.channels.fetch(subscriber.discordChannelId);

          const embed = new EmbedBuilder()
            .setColor(eventType === 'START' ? 0x00C853 : 0xFF1744) 
            .setTitle(eventType === 'START' ? '방송이 시작되었습니다!' : '방송이 종료되었습니다!')
            .setDescription(`방송자 채널: https://chzzk.naver.com/live/${streamerId}`)
            .setTimestamp(new Date());

          await channel.send({ embeds: [embed] });

          console.info(`[Stream Status] 알림 전송 성공 → ${subscriber.discordChannelId}`);
        } catch (error) {
          console.error(`[Stream Status] 알림 실패 → ${subscriber.discordChannelId}`, error.message);
        }
      }

      res.json({ message: '알림 전송 완료' });

    } catch (error) {
      console.error('[Stream Status] 처리 오류:', error.response?.data || error.message);
      res.status(500).json({ error: '알림 처리 중 오류 발생' });
    }
  });

  return router;
}

module.exports = createStreamStatusRouter;
