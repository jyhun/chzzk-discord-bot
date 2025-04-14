const express = require('express');
const axios = require('axios');
const { EmbedBuilder } = require('discord.js');
const dayjs = require('dayjs');
const duration = require('dayjs/plugin/duration');

dayjs.extend(duration);

function createNotificationRouter(client) {
  const router = express.Router();

  router.post('/send-notification', async (req, res) => {
    const {
      streamEventId,
      streamerId,
      streamerUrl,
      nickname,
      title,
      category,
      viewerCount,
      summary,
      formattedDate,
      startedAt,
      viewerIncreaseRate
    } = req.body;

    console.info(`[Send Notification] 요청 수신: streamEventId=${streamEventId}, streamerId=${streamerId}`);

    try {
      // 1. 구독자 목록 조회 (Backend API 호출)
      const response = await axios.get(process.env.BACKEND_BASE_URL + '/api/subscriptions', {
        params: { streamerId, eventType: 'HOT' }
      });

      const subscribers = response.data;
      console.info(`[Send Notification] 구독자 수: ${subscribers.length}`);

      if (subscribers.length === 0) {
        console.info('[Send Notification] 구독자가 없어 알림 전송을 종료합니다.');
        return res.json({ message: '구독자가 없습니다.' });
      }

      // 방송 시작 후 경과 시간 계산
      const now = dayjs();
      const started = dayjs(startedAt);
      const diffMinutes = now.diff(started, 'minute');
      const broadcastElapsedTime = `${diffMinutes}분 경과`;

      // 2. 구독자에게 알림 전송
      for (const subscriber of subscribers) {
        try {
          const channel = await client.channels.fetch(subscriber.discordChannelId);

          const embed = new EmbedBuilder()
            .setColor(0xFF5733)
            .setTitle('🚀 실시간 방송 인기 급상승 순간 포착!')
            .addFields(
              { name: '🧑‍💻 방송자', value: nickname, inline: true },
              { name: '🎮 카테고리', value: category, inline: true },
              { name: '👥 현재 시청자 수', value: `${Number(viewerCount).toLocaleString()}명 (평균 대비 +${Math.round(viewerIncreaseRate)}%)`, inline: true },
              { name: '🕰️ 방송 시작 후', value: broadcastElapsedTime, inline: true },
              { name: '🏷️ 방송 제목', value: title || '제목 없음' },
              { name: '🔥 시청자 반응', value: summary || '요약된 내용 없음' },
              { name: '⏰ 감지 시각', value: formattedDate }
            )

          await channel.send({
            content: streamerUrl,
            embeds: [embed]
          });


          // 3. 알림 결과 저장 (성공)
          await axios.post(process.env.BACKEND_BASE_URL + '/api/notifications', {
            streamEventId,
            receiverId: subscriber.discordChannelId,
            success: true,
            message: null,
            errorMessage: null
          });

          console.info(`[Send Notification] 알림 성공: ${subscriber.discordChannelId}`);
        } catch (error) {
          console.error(`[Send Notification] 알림 실패: ${subscriber.discordChannelId}`, error.message);

          // 3. 알림 결과 저장 (실패)
          await axios.post(process.env.BACKEND_BASE_URL + '/api/notifications', {
            streamEventId,
            receiverId: subscriber.discordChannelId,
            success: false,
            message: null,
            errorMessage: error.message
          });
        }
      }

      res.json({ message: '알림 전송 완료' });
    } catch (error) {
      console.error('[Send Notification] 처리 오류:', error.response?.data || error.message);
      res.status(500).json({ error: '알림 전송 처리 중 오류' });
    }
  });

  return router;
}

module.exports = createNotificationRouter;
