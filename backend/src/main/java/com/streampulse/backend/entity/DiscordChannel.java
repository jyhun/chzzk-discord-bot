package com.streampulse.backend.entity;

import com.streampulse.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class DiscordChannel extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String discordGuildId;

    @Column(nullable = false, length = 100, unique = true)
    private String discordChannelId;

    @Column(nullable = false)
    private boolean active;

    public void activate() {
        this.active = true;
    }
}
