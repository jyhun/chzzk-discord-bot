package com.streampulse.backend.dto;

import com.streampulse.backend.enums.EventType;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SubscriptionRequestDTO {
    private String discordUserId;
    private String username;
    private String streamerId;
    private EventType eventType;
    private String keyword;
}
