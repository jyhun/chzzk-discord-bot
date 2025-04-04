package com.streampulse.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GptResponseDTO {
    private List<Choice> choices;

    @Data
    public static class Choice {
        private GptMessageDTO message;
    }
}
