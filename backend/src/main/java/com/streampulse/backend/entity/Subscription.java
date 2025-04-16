package com.streampulse.backend.entity;

import com.streampulse.backend.common.BaseTimeEntity;
import com.streampulse.backend.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Subscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discord_channel_id", nullable = false)
    private DiscordChannel discordChannel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "streamer_id", nullable = false)
    private Streamer streamer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private boolean active;

    @OneToMany(mappedBy = "subscription", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Keyword> keywords = new ArrayList<>();

    public void deactivate() {
        this.active = false;
    }

}
