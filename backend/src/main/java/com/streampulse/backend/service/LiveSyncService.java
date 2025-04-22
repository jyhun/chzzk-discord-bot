package com.streampulse.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class LiveSyncService {

    private final ChzzkLiveService chzzkLiveService;
    private final StreamerService streamerService;
    private final StreamSessionService streamSessionService;
    private final StreamMetricsService streamMetricsService;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_KEY_PREFIX = "snapshot:";

    @LogExecution
    public void syncLiveBroadcasts() {
        Set<String> liveStreamerIds = new HashSet<>();
        Set<String> visitedBroadcasterIds = new HashSet<>();

        List<LiveResponseDTO> liveList = chzzkLiveService.collectLiveBroadcastersFromRedis();

        for (LiveResponseDTO dto : liveList) {
            String channelId = dto.getChannelId();

            if (!visitedBroadcasterIds.add(channelId)) continue;

            liveStreamerIds.add(channelId);

            Streamer streamer = streamerService.getOrCreateStreamer(dto);
            StreamSession session = streamSessionService.getOrCreateSession(streamer, dto);

            if (!streamer.isLive()) {
                streamerService.updateLiveStatus(streamer, true);

                if (streamer.getAverageViewerCount() >= 10) {
                    if (subscriptionService.hasSubscribersFor(EventType.START, channelId)) {
                        notificationService.requestStreamStartNotification(channelId, streamer.getNickname());
                    }

                    if (subscriptionService.hasSubscribersFor(EventType.TOPIC, channelId)) {
                        subscriptionService.detectTopicEvent(dto);
                    }
                }
            }

            // 변경 사항 있을 경우 알림
            else if (hasChanged(session.getId(), dto)) {
                if (subscriptionService.hasSubscribersFor(EventType.TOPIC, channelId) && streamer.getAverageViewerCount() >= 10) {
                    subscriptionService.detectTopicEvent(dto);
                }
            }

            streamMetricsService.saveMetrics(session, dto, streamer.getAverageViewerCount());

        }

        streamerService.markOfflineStreamers(liveStreamerIds);
    }

    private boolean hasChanged(Long sessionId, LiveResponseDTO dto) {
        String redisKey = REDIS_KEY_PREFIX + sessionId;

        String currJson = serialize(dto);
        String prevJson = redisTemplate.opsForValue().get(redisKey);

        if (!currJson.equals(prevJson)) {
            redisTemplate.opsForValue().set(redisKey, currJson);
            return true;
        }
        return false;
    }

    private String serialize(LiveResponseDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", dto.getLiveTitle());
        map.put("category", dto.getLiveCategory());
        map.put("tags", dto.getTags() != null ? dto.getTags() : List.of());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
