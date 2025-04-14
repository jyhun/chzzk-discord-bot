package com.streampulse.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class DiscordChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String discordGuildId;

    @Column(length = 100, unique = true)
    private String discordChannelId;

    private boolean active;

    private LocalDateTime createdAt;

    public void activate() {
        this.active = true;
    }
}
