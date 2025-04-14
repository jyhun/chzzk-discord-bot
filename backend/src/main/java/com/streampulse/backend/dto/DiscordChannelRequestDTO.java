package com.streampulse.backend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscordChannelRequestDTO {
    private String discordGuildId;
    private String discordChannelId;
}
