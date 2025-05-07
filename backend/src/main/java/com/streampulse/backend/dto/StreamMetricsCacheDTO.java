package com.streampulse.backend.dto;

import com.streampulse.backend.entity.StreamMetrics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamMetricsCacheDTO {

    private Long id;
    private int viewerCount;
    private String category;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static StreamMetricsCacheDTO fromEntity(StreamMetrics metrics) {
        return StreamMetricsCacheDTO.builder()
                .id(metrics.getId())
                .viewerCount(metrics.getViewerCount())
                .category(metrics.getCategory())
                .title(metrics.getTitle())
                .createdAt(metrics.getCreatedAt())
                .updatedAt(metrics.getUpdatedAt())
                .build();
    }

}