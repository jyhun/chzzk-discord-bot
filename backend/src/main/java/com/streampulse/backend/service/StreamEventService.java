package com.streampulse.backend.service;

import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.repository.StreamEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamEventService {

    private final StreamEventRepository streamEventRepository;
    private final ChatService chatService;

    public void saveStreamEvent(StreamMetrics metrics) {
        StreamEvent streamEvent = StreamEvent.builder()
                .metrics(metrics)
                .summary(null)
                .notified(false)
                .detectedAt(LocalDateTime.now())
                .build();

        streamEventRepository.save(streamEvent);

        chatService.collectChatsForStreamEvent(streamEvent);
    }
}
