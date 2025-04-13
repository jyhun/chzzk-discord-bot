const express = require('express');
const axios = require('axios');

function createNotificationRouter(client) {
  const router = express.Router();

  router.post('/send-notification', async (req, res) => {
    const { streamEventId, summary, streamerId } = req.body;

    console.info(`[Send Notification] 요청 수신: streamEventId=${streamEventId}, streamerId=${streamerId}`);

    try {
      // 1. 구독자 목록 조회 (Backend API 호출)
      const response = await axios.get(process.env.BACKEND_BASE_URL + '/api/subscriptions', {
        params: { streamerId, eventType: 'HOT' }
      });

      const subscribers = response.data;
      console.info(`[Send Notification] 구독자 수: ${subscribers.length}`);

      // 구독자가 없으면 바로 종료
      if (subscribers.length === 0) {
        console.info('[Send Notification] 구독자가 없어 알림 전송을 종료합니다.');
        return res.json({ message: '구독자가 없습니다.' });
      }

      // 2. 구독자에게 알림 전송
      for (const subscriber of subscribers) {
        const message = `[HOT] 방송자가 급상승했습니다!\n\n${summary}`;

        try {
          const user = await client.users.fetch(subscriber.discordUserId);
          await user.send(message);

          // 3. 알림 결과 저장 (성공)
          await axios.post(process.env.BACKEND_BASE_URL + '/api/notifications', {
            streamEventId,
            receiverId: subscriber.discordUserId,
            success: true,
            message,
            errorMessage: null
          });

          console.info(`[Send Notification] 알림 성공: ${subscriber.discordUserId}`);
        } catch (error) {
          console.error(`[Send Notification] 알림 실패: ${subscriber.discordUserId}`, error.message);

          // 3. 알림 결과 저장 (실패)
          await axios.post(process.env.BACKEND_BASE_URL + '/api/notifications', {
            streamEventId,
            receiverId: subscriber.discordUserId,
            success: false,
            message,
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
