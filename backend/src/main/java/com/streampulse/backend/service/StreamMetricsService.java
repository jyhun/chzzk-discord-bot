package com.streampulse.backend.service;

import com.streampulse.backend.dto.StreamMetricsInputDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.repository.StreamMetricsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamMetricsService {

    private final StreamMetricsRepository streamMetricsRepository;
    private final StreamEventService streamEventService;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMetricsBulk(List<StreamMetricsInputDTO> inputs) {
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

            if (metrics.getViewerCount() >= 1000 && metrics.getViewerCount() > input.getAverageViewerCount() * 1.5) {
                hotEvents.add(() -> streamEventService.saveStreamEvent(metrics, input.getAverageViewerCount()));
            }
        }

        chunkedSaveAll(batch, streamMetricsRepository::saveAll, 500);
        hotEvents.forEach(Runnable::run);
    }

    public List<StreamMetrics> findByStreamSessionId(Long id) {
        return streamMetricsRepository.findByStreamSessionId(id);
    }

    private <T> void chunkedSaveAll(List<T> list, Consumer<List<T>> saveFn, int chunkSize) {
        for (int i = 0; i < list.size(); i += chunkSize) {
            saveFn.accept(list.subList(i, Math.min(i + chunkSize, list.size())));
            entityManager.flush();
            entityManager.clear();
        }
    }

}
