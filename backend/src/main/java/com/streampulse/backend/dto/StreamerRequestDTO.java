package com.streampulse.backend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StreamerRequestDTO {
    private String channelId;
    private String nickname;
}
