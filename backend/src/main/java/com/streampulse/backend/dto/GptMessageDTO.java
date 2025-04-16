package com.streampulse.backend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "content")
public class GptMessageDTO {
    private String role;
    private String content;
}
