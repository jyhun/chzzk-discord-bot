package com.streampulse.backend.entity;

import com.streampulse.backend.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private DiscordChannel discordChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    private Streamer streamer;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    private boolean active;

    @Column(length = 100)
    private String keyword;

    public void deactivate() {
        this.active = false;
    }

}
