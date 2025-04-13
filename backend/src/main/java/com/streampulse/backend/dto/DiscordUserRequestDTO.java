package com.streampulse.backend.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DiscordUserRequestDTO {
    private String discordUserId;
    private String username;
}
