package com.streampulse.backend.dto;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionResponseDTO {
    private String discordUserId;
    private String username;
}
