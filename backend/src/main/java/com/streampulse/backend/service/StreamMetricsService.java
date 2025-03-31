package com.streampulse.backend.service;

import com.streampulse.backend.dto.StreamMetricsRequestDTO;
import com.streampulse.backend.dto.StreamMetricsResponseDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
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

    public StreamMetricsResponseDTO saveMetrics(StreamMetricsRequestDTO streamMetricsRequestDTO) {
        Streamer streamer = streamerRepository.findByChannelId(streamMetricsRequestDTO.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("방송자를 찾을 수 없습니다."));
        StreamSession streamSession = streamSessionRepository.findByStreamerId(streamer.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 채널 ID의 세션을 찾을 수 없습니다."));

        StreamMetrics streamMetrics = StreamMetrics.builder()
                .session(streamSession)
                .chatCount(streamMetricsRequestDTO.getChatCount())
                .viewerCount(streamMetricsRequestDTO.getViewerCount())
                .collectedAt(LocalDateTime.now())
                .build();

        StreamMetrics savedStreamMetrics = streamMetricsRepository.save(streamMetrics);

        highlightService.detectAndSaveHighlight(streamSession.getId());

        return StreamMetricsResponseDTO.builder()
                .id(savedStreamMetrics.getId())
                .sessionId(streamSession.getId())
                .chatCount(savedStreamMetrics.getChatCount())
                .viewerCount(savedStreamMetrics.getViewerCount())
                .collectedAt(savedStreamMetrics.getCollectedAt())
                .build();

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
