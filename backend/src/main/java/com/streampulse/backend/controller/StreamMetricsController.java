package com.streampulse.backend.controller;

import com.streampulse.backend.dto.StreamMetricsRequestDTO;
import com.streampulse.backend.dto.StreamMetricsResponseDTO;
import com.streampulse.backend.service.StreamMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stream-metrics")
@RequiredArgsConstructor
public class StreamMetricsController {

    private final StreamMetricsService streamMetricsService;

    @PostMapping
    public ResponseEntity<StreamMetricsResponseDTO> saveMetrics(@RequestBody StreamMetricsRequestDTO streamMetricsRequestDTO) {
        StreamMetricsResponseDTO streamMetricsResponseDTO = streamMetricsService.saveMetrics(streamMetricsRequestDTO);
        return ResponseEntity.ok(streamMetricsResponseDTO);
    }

}
