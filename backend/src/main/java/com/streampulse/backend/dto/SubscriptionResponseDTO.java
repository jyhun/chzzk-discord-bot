package com.streampulse.backend.dto;


import com.streampulse.backend.enums.EventType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionResponseDTO {
    private String discordGuildId;
    private String discordChannelId;
    private String streamerId;
    private EventType eventType;
}
