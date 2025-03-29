package com.streampulse.backend.controller;

import com.streampulse.backend.dto.StreamerRequestDTO;
import com.streampulse.backend.dto.StreamerResponseDTO;
import com.streampulse.backend.service.StreamerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/streamers")
@RequiredArgsConstructor
public class StreamerController {

    private final StreamerService streamerService;

    @PostMapping
    public ResponseEntity<StreamerResponseDTO> registerStreamer(@RequestBody StreamerRequestDTO streamerRequestDTO) {
        StreamerResponseDTO streamerResponseDTO = streamerService.registerStreamer(streamerRequestDTO);
        return ResponseEntity.ok(streamerResponseDTO);
    }

}
