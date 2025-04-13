package com.streampulse.backend.controller;

import com.streampulse.backend.dto.ChatMessagesRequestDTO;
import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.service.ChatService;
import com.streampulse.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final NotificationService notificationService;

    @PostMapping("/{channelId}/{streamEventId}")
    public ResponseEntity<String> chatCollector(@PathVariable String channelId, @PathVariable String streamEventId, @RequestBody ChatMessagesRequestDTO chatMessagesRequestDTO) {
        StreamEvent streamEvent = chatService.collectChats(channelId, streamEventId, chatMessagesRequestDTO);
        return ResponseEntity.ok("채팅 요약후 알림 전송 성공");
    }

}
