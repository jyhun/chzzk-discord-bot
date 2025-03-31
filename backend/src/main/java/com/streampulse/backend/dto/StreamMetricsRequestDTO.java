package com.streampulse.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamMetricsRequestDTO {

    private String channelId;
    private int chatCount;
    private int viewerCount;

}
