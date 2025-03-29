package com.streampulse.backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StreamerResponseDTO {
    private Long id;
    private String channelId;
    private String nickname;
}
