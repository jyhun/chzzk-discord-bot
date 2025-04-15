package com.streampulse.backend.dto;

import com.streampulse.backend.enums.EventType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class SubscriptionRequestDTO {
    private String discordGuildId;
    private String discordChannelId;
    private String streamerId;
    private EventType eventType;
    private String keyword;
}
