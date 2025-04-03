package com.streampulse.backend.service;

import com.streampulse.backend.entity.Highlight;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.repository.HighlightRepository;
import com.streampulse.backend.repository.StreamMetricsRepository;
import com.streampulse.backend.repository.StreamSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class HighlightService {

    private final HighlightRepository highlightRepository;
    private final StreamMetricsRepository streamMetricsRepository;
    private final StreamSessionRepository streamSessionRepository;
    private final NotificationService notificationService;

    public void detectAndSaveHighlight(Long sessionId) {
        StreamSession streamSession = streamSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다."));

        StreamMetrics currentMetrics = streamMetricsRepository.findTopBySessionOrderByCollectedAtDesc(streamSession)
                .orElseThrow(() -> new IllegalArgumentException("매트릭을 찾을 수 없습니다."));

        Optional<StreamMetrics> previousMetrics = streamMetricsRepository.findTopBySessionAndCollectedAtLessThanOrderByCollectedAtDesc(streamSession, currentMetrics.getCollectedAt());

        if (previousMetrics.isPresent() && isHighlight(currentMetrics.getChatCount(), previousMetrics.get().getChatCount())) {
            float score = (float) currentMetrics.getChatCount() / previousMetrics.get().getChatCount();

            Highlight highlight = Highlight.builder()
                    .session(streamSession)
                    .detectedAt(currentMetrics.getCollectedAt())
                    .chatCount(currentMetrics.getChatCount())
                    .score(score)
                    .build();

            highlightRepository.save(highlight);

            notificationService.notifyHighlight(highlight);
        }
    }

    public boolean isHighlight(int currentChatCount, int previousChatCount) {
        if (previousChatCount == 0) return false;
        float score = (float) currentChatCount / (float) previousChatCount;
        return score >= 2.0f;
    }

    public void saveHighlight(StreamMetrics metrics) {
        Highlight highlight = Highlight.builder()
                .metrics(metrics)
                .summary(null)
                .notified(false)
                .detectedAt(LocalDateTime.now())
                .build();

        highlightRepository.save(highlight);
    }
}
