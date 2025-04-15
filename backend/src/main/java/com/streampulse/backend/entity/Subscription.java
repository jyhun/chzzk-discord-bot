package com.streampulse.backend.entity;

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

    @OneToMany(mappedBy = "subscription", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<Keyword> keywords = new ArrayList<>();

    public void deactivate() {
        this.active = false;
    }

}
