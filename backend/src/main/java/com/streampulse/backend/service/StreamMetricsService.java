package com.streampulse.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.dto.StreamMetricsInputDTO;
import com.streampulse.backend.dto.StreamMetricsCacheDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.repository.StreamMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamMetricsService {

    private final StreamMetricsRepository streamMetricsRepository;
    private final StreamEventService streamEventService;
    private final ChunkService chunkService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String METRICS_KEY = "metrics:session:";
    private static final Duration METRICS_TTL = Duration.ofDays(1);

    public void saveMetricsInChunks(List<StreamMetricsInputDTO> inputs, int chunkSize) {
        List<StreamMetrics> batch = new ArrayList<>(inputs.size());
        List<Runnable> hotEvents = new ArrayList<>();

        for (StreamMetricsInputDTO input : inputs) {
            StreamMetrics metrics = StreamMetrics.builder()
                    .streamSession(input.getSession())
                    .viewerCount(input.getDto().getConcurrentUserCount())
                    .category(input.getDto().getLiveCategoryValue())
                    .title(input.getDto().getLiveTitle())
                    .build();
            metrics.addTags(input.getDto().getTags());
            batch.add(metrics);

            if (metrics.getViewerCount() >= 1000
                    && metrics.getViewerCount() > input.getAverageViewerCount() * 1.5) {
                hotEvents.add(() ->
                        streamEventService.saveStreamEvent(metrics, input.getAverageViewerCount())
                );
            }
        }

        for (int i = 0; i < batch.size(); i += chunkSize) {
            List<StreamMetrics> chunk = batch.subList(i, Math.min(i + chunkSize, batch.size()));
            chunkService.saveMetricsChunk(chunk);
        }

        hotEvents.forEach(Runnable::run);
    }

    @Transactional(readOnly = true)
    public List<StreamMetricsCacheDTO> findByStreamSessionId(Long sessionId) {
        String cacheKey = METRICS_KEY + sessionId;
        Object cachedObj = redisTemplate.opsForValue().get(cacheKey);

        if (cachedObj != null) {
            return objectMapper.convertValue(
                    cachedObj,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StreamMetricsCacheDTO.class)
            );
        }

        List<StreamMetrics> metrics = streamMetricsRepository.findByStreamSessionId(sessionId);
        metrics.forEach(m -> Hibernate.initialize(m.getTags()));

        List<StreamMetricsCacheDTO> dtoList = metrics.stream()
                .map(StreamMetricsCacheDTO::fromEntity)
                .toList();

        if(!dtoList.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, dtoList, METRICS_TTL);
        }

        return dtoList;
    }

}
