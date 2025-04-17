package com.streampulse.backend.service;

import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.StreamEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StreamEventServiceTest {

    @Mock
    private StreamEventRepository streamEventRepository;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private StreamEventService streamEventService;

    @Test
    void saveStreamEvent_shouldSaveEventAndCallChatService() {
        // given
        StreamMetrics metrics = mock(StreamMetrics.class);
        when(metrics.getViewerCount()).thenReturn(200);

        // when
        streamEventService.saveStreamEvent(metrics, 100); // 평균 100명 → 200명이면 2배

        // then
        verify(streamEventRepository).save(argThat(event ->
                event.getStreamMetrics() == metrics &&
                        event.getEventType() == EventType.HOT &&
                        event.getViewerCount() == 200 &&
                        event.getViewerIncreaseRate() == 2.0f
        ));

        verify(chatService).collectChatsForStreamEvent(any(StreamEvent.class));
    }
}
