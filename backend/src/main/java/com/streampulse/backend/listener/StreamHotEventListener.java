package com.streampulse.backend.listener;

import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.repository.StreamEventRepository;
import com.streampulse.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class StreamHotEventListener {

    private final ChatService chatService;
    private final StreamEventRepository streamEventRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onHotCreated(Long streamEventId) {
        StreamEvent streamEvent = streamEventRepository.findById(streamEventId)
                .orElseThrow(() -> new IllegalArgumentException("StreamEvent 찾을수 없습니다."));
        chatService.collectChatsForStreamEvent(streamEvent);
    }
}
