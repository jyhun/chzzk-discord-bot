package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.repository.StreamMetricsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamMetricsService {

    private final StreamMetricsRepository streamMetricsRepository;
    private final StreamEventService streamEventService;

    public void saveMetrics(StreamSession session, LiveResponseDTO dto, Integer averageViewerCount) {
        StreamMetrics metrics = StreamMetrics.builder()
                .session(session)
                .collectedAt(LocalDateTime.now())
                .viewerCount(dto.getConcurrentUserCount())
                .category(dto.getLiveCategoryValue())
                .title(dto.getLiveTitle())
                .build();
        metrics = streamMetricsRepository.save(metrics);
        if(metrics.getViewerCount() > averageViewerCount * 1.5) {
            streamEventService.saveStreamEvent(metrics);
        }
    }

}
