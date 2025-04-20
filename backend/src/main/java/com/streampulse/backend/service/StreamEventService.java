package com.streampulse.backend.service;

import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.StreamEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamEventService {

    private final StreamEventRepository streamEventRepository;
    private final SubscriptionService subscriptionService;
    private final ChatService chatService;
    private final RedisTemplate<String, String> redisTemplate;

    public void saveStreamEvent(StreamMetrics metrics, Integer averageViewerCount) {
        String sessionId = metrics.getStreamSession().getId().toString();

        if (redisTemplate.hasKey(sessionId)) {
            return;
        }

        StreamEvent streamEvent = StreamEvent.builder()
                .streamMetrics(metrics)
                .eventType(EventType.HOT)
                .viewerCount(metrics.getViewerCount())
                .viewerIncreaseRate((float) metrics.getViewerCount() / averageViewerCount)
                .build();

        streamEventRepository.save(streamEvent);

        redisTemplate.opsForValue().set(sessionId, "HOT", Duration.ofDays(1));

        if (!subscriptionService.hasSubscribersFor(EventType.HOT, metrics.getStreamSession().getStreamer().getChannelId())) {
            return;
        }

        chatService.collectChatsForStreamEvent(streamEvent);
    }
}
