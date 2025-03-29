package com.streampulse.backend.controller;

import com.streampulse.backend.dto.StreamerRequestDTO;
import com.streampulse.backend.dto.StreamerResponseDTO;
import com.streampulse.backend.service.StreamerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping("/by-channel/{channelId}")
    public ResponseEntity<StreamerResponseDTO> getStreamerByChannelId(@PathVariable String channelId) {
        StreamerResponseDTO streamerResponseDTO = streamerService.getStreamerByChannelId(channelId);
        return ResponseEntity.ok(streamerResponseDTO);
    }

    @GetMapping
    public ResponseEntity<List<StreamerResponseDTO>> getAllStreamers() {
        List<StreamerResponseDTO> streamerResponseDTOList = streamerService.getAllStreamers();
        return ResponseEntity.ok(streamerResponseDTOList);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StreamerResponseDTO> updateStreamer(
            @PathVariable Long id,
            @RequestBody StreamerRequestDTO streamerRequestDTO
    ) {
        StreamerResponseDTO streamerResponseDTO = streamerService.updateStreamer(id, streamerRequestDTO);
        return ResponseEntity.ok(streamerResponseDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStreamer(@PathVariable Long id) {
        streamerService.deleteStreamer(id);
        return ResponseEntity.noContent().build();
    }

}
