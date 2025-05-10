package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamMetricsRepository;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class StreamSessionService {

    private final StreamSessionRepository streamSessionRepository;
    private final StreamerRepository streamerRepository;
    private final StreamMetricsRepository streamMetricsRepository;

    public StreamSession getOrCreateSession(Streamer streamer, LiveResponseDTO dto) {
        LocalDateTime startedAt = LocalDateTime.parse(dto.getOpenDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return streamSessionRepository
                .findByStreamer_ChannelIdAndStartedAt(streamer.getChannelId(), startedAt)
                .orElseGet(() -> streamSessionRepository.save(
                        StreamSession.builder()
                                .streamer(streamer)
                                .title(dto.getLiveTitle())
                                .category(dto.getLiveCategoryValue())
                                .startedAt(startedAt)
                                .build()
                ));
    }

    @LogExecution
    public StreamSession handleStreamEnd(Streamer streamer,LocalDateTime startedAt) {
        return streamSessionRepository.findByStreamer_ChannelIdAndStartedAt(streamer.getChannelId(), startedAt)
                .map(streamSession -> {
                    streamSession.updateEndedAt();

                    List<StreamMetrics> streamMetricsList = streamMetricsRepository.findByStreamSessionId(streamSession.getId());

                    int sessionAvgViewer = (int) streamMetricsList.stream()
                            .mapToInt(StreamMetrics::getViewerCount)
                            .average()
                            .orElse(0.0);

                    streamSession.updateAverageViewerCount(sessionAvgViewer);

                    int sessionPeakViewer = streamMetricsList.stream()
                            .mapToInt(StreamMetrics::getViewerCount)
                            .max()
                            .orElse(0);

                    streamSession.updatePeakViewerCount(sessionPeakViewer);

                    List<StreamSession> streamSessionList = streamSessionRepository.findByStreamerIdAndEndedAtIsNotNull(streamer.getId());
                    int streamerAvgViewer = (int) streamSessionList.stream()
                            .mapToInt(StreamSession::getAverageViewerCount)
                            .average()
                            .orElse(0.0);

                    streamer.updateAverageViewerCount(streamerAvgViewer);
                    streamerRepository.save(streamer);

                    return streamSessionRepository.save(streamSession);
                })
                .orElse(null);
    }

}
