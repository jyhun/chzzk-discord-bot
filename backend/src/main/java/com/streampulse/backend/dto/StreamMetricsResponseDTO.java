package com.streampulse.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamMetricsResponseDTO {

    private Long id;
    private Long sessionId;
    private LocalDateTime collectedAt;
    private int chatCount;
    private int viewerCount;

}
