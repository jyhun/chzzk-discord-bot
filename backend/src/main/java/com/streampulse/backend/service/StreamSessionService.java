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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamSessionService {

    private final StreamSessionRepository streamSessionRepository;
    private final StreamerRepository streamerRepository;
    private final StreamMetricsRepository streamMetricsRepository;

    public StreamSession getOrCreateSession(Streamer streamer, LiveResponseDTO dto) {
        if (!streamer.isLive()) {
            StreamSession session = StreamSession.builder()
                    .streamer(streamer)
                    .title(dto.getLiveTitle())
                    .category(dto.getLiveCategoryValue())
                    .startedAt(LocalDateTime.parse(dto.getOpenDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();
            return streamSessionRepository.save(session);
        }
        return streamSessionRepository.findByStreamer_ChannelIdAndEndedAtIsNull(streamer.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("방송 세션을 찾을 수 없습니다."));
    }


    @LogExecution
    public StreamSession handleStreamEnd(Streamer streamer) {
        StreamSession streamSession = streamSessionRepository.findByStreamer_ChannelIdAndEndedAtIsNull(streamer.getChannelId())
                .orElseThrow(() -> new IllegalArgumentException("방송 세션을 찾을 수 없습니다."));
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


        List<StreamSession> streamSessionList = streamSessionRepository.findByStreamerId(streamer.getId());
        int streamerAvgViewer = (int) streamSessionList.stream()
                .mapToInt(StreamSession::getAverageViewerCount)
                .average()
                .orElse(0.0);

        streamer.updateAverageViewerCount(streamerAvgViewer);
        streamerRepository.save(streamer);

        return streamSessionRepository.save(streamSession);
    }
}
