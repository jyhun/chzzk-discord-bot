package com.streampulse.backend.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamSessionResponseDTO {
    private Long id;
    private String channelId;
    private String title;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
}
