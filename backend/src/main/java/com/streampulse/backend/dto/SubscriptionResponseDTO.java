package com.streampulse.backend.dto;


import com.streampulse.backend.enums.EventType;
import lombok.*;

import java.util.List;

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
    private List<String> keywords;
}
