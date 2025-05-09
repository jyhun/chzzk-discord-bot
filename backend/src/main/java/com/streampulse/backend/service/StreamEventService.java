package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.StreamEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamEventService {

    private final StreamEventRepository streamEventRepository;
    private final SubscriptionService subscriptionService;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String STREAM_EVENT_KEY = "hot:";
    private static final Duration STREAM_EVENT_TTL = Duration.ofDays(1);

    @LogExecution
    public void saveStreamEvent(StreamMetrics metrics, Integer averageViewerCount) {
        String sessionId = metrics.getStreamSession().getId().toString();

        String cacheKey = STREAM_EVENT_KEY + sessionId;

        if (redisTemplate.hasKey(cacheKey)) {
            return;
        }

        float viewerIncreaseRate = averageViewerCount == 0
                ? 0.0f
                : (float) metrics.getViewerCount() / averageViewerCount;

        StreamEvent streamEvent = StreamEvent.builder()
                .streamMetrics(metrics)
                .eventType(EventType.HOT)
                .viewerCount(metrics.getViewerCount())
                .viewerIncreaseRate(viewerIncreaseRate)
                .build();

        streamEvent = streamEventRepository.save(streamEvent);

        redisTemplate.opsForValue().set(cacheKey, "HOT", STREAM_EVENT_TTL);

        if (!subscriptionService.hasSubscribersFor(EventType.HOT, metrics.getStreamSession().getStreamer().getChannelId())) {
            return;
        }

        applicationEventPublisher.publishEvent(streamEvent.getId());
    }
}
