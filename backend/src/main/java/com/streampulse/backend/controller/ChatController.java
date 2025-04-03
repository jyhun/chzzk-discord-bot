package com.streampulse.backend.controller;

import com.streampulse.backend.dto.ChatMessagesRequestDTO;
import com.streampulse.backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{channelId}")
    public ResponseEntity<String> chatCollector(@PathVariable String channelId, @RequestBody ChatMessagesRequestDTO chatMessagesRequestDTO) {
        chatService.collectChats(channelId, chatMessagesRequestDTO);
        return ResponseEntity.ok("채팅 수집 실행");
    }

}
