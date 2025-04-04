package com.streampulse.backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GptRequestDTO {
    private String model;
    private List<GptMessageDTO> messages;
    private double temperature;
    private int max_tokens;
}
