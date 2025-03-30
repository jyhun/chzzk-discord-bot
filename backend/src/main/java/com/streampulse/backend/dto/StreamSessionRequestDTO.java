package com.streampulse.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamSessionRequestDTO {
    private String channelId;
    private String title;
}
