package com.streampulse.backend.service;

import com.streampulse.backend.entity.Highlight;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.repository.HighlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class HighlightService {

    private final HighlightRepository highlightRepository;
    private final ChatService chatService;

    public void saveHighlight(StreamMetrics metrics) {
        Highlight highlight = Highlight.builder()
                .metrics(metrics)
                .summary(null)
                .notified(false)
                .detectedAt(LocalDateTime.now())
                .build();

        highlightRepository.save(highlight);

        chatService.collectChatsForHighlight(highlight);
    }
}
