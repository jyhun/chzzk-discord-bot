package com.streampulse.backend.service;

import com.streampulse.backend.dto.StreamMetricsInputDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.repository.StreamMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamMetricsService {

    private final StreamMetricsRepository streamMetricsRepository;
    private final StreamEventService streamEventService;
    private final ChunkService chunkService;

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
    public List<StreamMetrics> findByStreamSessionId(Long id) {
        List<StreamMetrics> metrics = streamMetricsRepository.findByStreamSessionId(id);
        metrics.forEach(m -> Hibernate.initialize(m.getTags()));
        return metrics;
    }

}
