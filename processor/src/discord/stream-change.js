const express = require('express');
const axios = require('axios');
const { EmbedBuilder } = require('discord.js');

function createChangeRouter(client) {
  const router = express.Router();

  router.post('/stream-change', async (req, res) => {
    const { streamerId, eventType, keyword, title, category, tags } = req.body;

    if (eventType !== 'CHANGE') {
      return res.status(400).json({ error: '지원하지 않는 이벤트 타입입니다.' });
    }

    console.info(`[CHANGE] 변경 감지 요청 수신: ${streamerId}`);

    try {
      // 1. 해당 방송자의 CHANGE 구독 목록 요청
      const response = await axios.get(process.env.BACKEND_BASE_URL + '/api/subscriptions', {
        params: {
          streamerId,
          eventType: 'CHANGE'
        }
      });

      const subscriptions = response.data;

      // 2. 디스코드 채널 기준으로 키워드 그룹화
      const channelMap = new Map();

      for (const sub of subscriptions) {
        const channelId = sub.discordChannelId;
        const keyword = sub.keyword;

        if (!channelMap.has(channelId)) {
          channelMap.set(channelId, new Set());
        }

        if (keyword) {
          channelMap.get(channelId).add(keyword);
        }
      }

      // 3. 알림 전송
      for (const [channelId, keywordSet] of channelMap.entries()) {
        try {
          const channel = await client.channels.fetch(channelId);
          const keywords = Array.from(keywordSet).map(k => `\`${k}\``).join(', ');

          const embed = new EmbedBuilder()
            .setColor(0xFFD700)
            .setTitle('🔄 방송 정보 변경 감지!')
            .addFields(
              { name: '방송자 채널 ID', value: streamerId, inline: false },
              { name: '감지된 키워드', value: keywords || 'N/A', inline: false },
              { name: '제목', value: title || '제목 없음', inline: false },
              { name: '카테고리', value: category || '카테고리 없음', inline: false },
              { name: '태그', value: (tags || []).join(', ') || '태그 없음', inline: false },
              { name: '이벤트', value: 'CHANGE', inline: true }
            )
            .setTimestamp();

          await channel.send({
            content: `📢 방송 정보 변경 감지됨!`,
            embeds: [embed]
          });

          console.info(`[CHANGE] 알림 전송 성공: ${channelId}`);
        } catch (err) {
          console.error(`[CHANGE] 알림 전송 실패: ${channelId}`, err.message);
        }
      }

      return res.json({ message: '알림 전송 완료' });
    } catch (error) {
      console.error('[CHANGE] 처리 실패:', error.response?.data || error.message);
      return res.status(500).json({ error: '서버 내부 오류' });
    }
  });

  return router;
}

module.exports = createChangeRouter;
