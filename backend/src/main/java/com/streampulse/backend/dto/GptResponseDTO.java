package com.streampulse.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = "choices")
public class GptResponseDTO {
    private List<Choice> choices;

    @Data
    @ToString(exclude = "message")
    public static class Choice {
        private GptMessageDTO message;
    }
}
