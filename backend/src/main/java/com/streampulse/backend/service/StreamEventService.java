package com.streampulse.backend.service;

import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.StreamEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class StreamEventService {

    private final StreamEventRepository streamEventRepository;
    private final ChatService chatService;

    public void saveStreamEvent(StreamMetrics metrics, Integer averageViewerCount) {
        StreamEvent streamEvent = StreamEvent.builder()
                .streamMetrics(metrics)
                .eventType(EventType.HOT)
                .viewerCount(metrics.getViewerCount())
                .viewerIncreaseRate((float) metrics.getViewerCount() / averageViewerCount)
                .build();

        streamEventRepository.save(streamEvent);

        chatService.collectChatsForStreamEvent(streamEvent);
    }
}
