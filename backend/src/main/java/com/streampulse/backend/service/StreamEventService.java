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

    @LogExecution
    public void saveStreamEvent(StreamMetrics metrics, Integer averageViewerCount) {
        String sessionId = metrics.getStreamSession().getId().toString();

        if (redisTemplate.hasKey(sessionId)) {
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

        redisTemplate.opsForValue().set(sessionId, "HOT", Duration.ofDays(1));

        if (!subscriptionService.hasSubscribersFor(EventType.HOT, metrics.getStreamSession().getStreamer().getChannelId())) {
            return;
        }

        applicationEventPublisher.publishEvent(streamEvent.getId());
    }
}
