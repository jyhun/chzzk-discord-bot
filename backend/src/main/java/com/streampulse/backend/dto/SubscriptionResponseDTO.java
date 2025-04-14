package com.streampulse.backend.dto;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionResponseDTO {
    private String discordGuildId;
    private String discordChannelId;
}
