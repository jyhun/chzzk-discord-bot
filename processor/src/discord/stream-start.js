const express = require('express');
const axios = require('axios');

function createStreamStartRouter(client) {
    const router = express.Router();

    router.post('/stream-start', async (req, res) => {
        const { streamerId, streamerName } = req.body;
        if (!streamerId) return res.status(400).json({ error: 'streamerId는 필수입니다.' });

        try {
            const { data: subscribers } = await axios.get(process.env.BACKEND_BASE_URL + '/api/subscriptions', {
                params: { streamerId, eventType: 'START' }
            });

            if (!subscribers.length) return res.json({ message: '알림 대상이 없습니다.' });

            const message = `[${streamerName}] 방송이 시작되었습니다\nhttps://chzzk.naver.com/live/${streamerId}`;

            for (const s of subscribers) {
                try {
                    const channel = await client.channels.fetch(s.discordChannelId);
                    await channel.send(message);
                    console.info(`[START] ${s.discordChannelId} 전송 성공`);

                
                    await axios.post(process.env.BACKEND_BASE_URL + '/api/notifications', {
                        eventType: 'START',
                        receiverId: s.discordChannelId,
                        success: true,
                        message: message
                    });
                } catch (e) {
                    console.error(`[START] ${s.discordChannelId} 전송 실패`, e.message);

                    await axios.post(process.env.BACKEND_BASE_URL + '/api/notifications', {
                        eventType: 'START',
                        receiverId: s.discordChannelId,
                        success: false,                    
                        message: e.message
                    });
                }
            }

            res.json({ message: '알림 전송 완료' });
        } catch (e) {
            console.error('[START] 처리 오류:', e.response?.data || e.message);
            res.status(500).json({ error: 'START 알림 처리 중 오류' });
        }
    });

    return router;
}

module.exports = createStreamStartRouter;
