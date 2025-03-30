package com.streampulse.backend.controller;

import com.streampulse.backend.dto.StreamSessionRequestDTO;
import com.streampulse.backend.dto.StreamSessionResponseDTO;
import com.streampulse.backend.service.StreamSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stream-sessions")
@RequiredArgsConstructor
public class StreamSessionController {

    private final StreamSessionService streamSessionService;

    @PostMapping("/start")
    public ResponseEntity<StreamSessionResponseDTO> startStreamSession(@RequestBody StreamSessionRequestDTO streamSessionRequestDTO) {
        StreamSessionResponseDTO streamSessionResponseDTO = streamSessionService.startSession(streamSessionRequestDTO);
        return ResponseEntity.ok(streamSessionResponseDTO);
    }

    @PostMapping("/end/{channelId}")
    public ResponseEntity<StreamSessionResponseDTO> endStreamSession(@PathVariable String channelId) {
        StreamSessionResponseDTO streamSessionResponseDTO = streamSessionService.endSession(channelId);
        return ResponseEntity.ok(streamSessionResponseDTO);
    }

}
