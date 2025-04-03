package com.streampulse.backend.controller;

import com.streampulse.backend.dto.StreamMetricsResponseDTO;
import com.streampulse.backend.service.StreamMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/stream-metrics")
@RequiredArgsConstructor
public class StreamMetricsController {

    private final StreamMetricsService streamMetricsService;

//    @PostMapping
//    public ResponseEntity<StreamMetricsResponseDTO> saveMetrics(@RequestBody StreamMetricsRequestDTO streamMetricsRequestDTO) {
//        StreamMetricsResponseDTO streamMetricsResponseDTO = streamMetricsService.saveMetrics(streamMetricsRequestDTO);
//        return ResponseEntity.ok(streamMetricsResponseDTO);
//    }

    @GetMapping("/by-session/{sessionId}")
    public ResponseEntity<List<StreamMetricsResponseDTO>> getMetrics(@PathVariable Long sessionId) {
        List<StreamMetricsResponseDTO> streamMetricsResponseDTOList = streamMetricsService.getStreamMetrics(sessionId);
        return ResponseEntity.ok(streamMetricsResponseDTOList);
    }

}
