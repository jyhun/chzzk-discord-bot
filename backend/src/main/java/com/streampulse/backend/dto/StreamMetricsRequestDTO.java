package com.streampulse.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamMetricsRequestDTO {

    private Long sessionId;
    private int chatCount;
    private int viewerCount;

}
