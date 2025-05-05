package com.streampulse.backend.dto;

import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StreamMetricsCacheDTO {

    private int viewerCount;
    private String category;
    private String title;
    private List<String> tags;
    private LocalDateTime createdAt;

    public static StreamMetricsCacheDTO fromEntity(StreamMetrics metrics) {
        return StreamMetricsCacheDTO.builder()
                .viewerCount(metrics.getViewerCount())
                .category(metrics.getCategory())
                .title(metrics.getTitle())
                .tags(metrics.getTags() != null ?
                        metrics.getTags().stream()
                                .map(Tag::getValue)
                                .collect(Collectors.toList())
                        : List.of())
                .createdAt(metrics.getCreatedAt())
                .build();
    }

}