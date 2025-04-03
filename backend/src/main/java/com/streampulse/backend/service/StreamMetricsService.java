package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.dto.StreamMetricsResponseDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.repository.StreamMetricsRepository;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamMetricsService {

    private final StreamMetricsRepository streamMetricsRepository;
    private final StreamSessionRepository streamSessionRepository;
    private final StreamerRepository streamerRepository;
    private final HighlightService highlightService;

    public void saveMetrics(StreamSession session, LiveResponseDTO dto, Integer averageViewerCount) {
        StreamMetrics metrics = StreamMetrics.builder()
                .session(session)
                .collectedAt(LocalDateTime.now())
                .viewerCount(dto.getConcurrentUserCount())
                .category(dto.getLiveCategoryValue())
                .title(dto.getLiveTitle())
                .thumbnailUrl(dto.getLiveThumbnailImageUrl())
                .build();
        metrics = streamMetricsRepository.save(metrics);
        if(metrics.getViewerCount() > averageViewerCount) {
            highlightService.saveHighlight(metrics);
        }
    }

    public List<StreamMetricsResponseDTO> getStreamMetrics(Long sessionId) {
        List<StreamMetrics> streamMetricsList = streamMetricsRepository.findBySessionId(sessionId);

        return streamMetricsList.stream()
                .map(m -> StreamMetricsResponseDTO.builder()
                        .id(m.getId())
                        .sessionId(m.getSession().getId())
                        .collectedAt(m.getCollectedAt())
                        .chatCount(m.getChatCount())
                        .viewerCount(m.getViewerCount())
                        .build())
                .toList();
    }

}
