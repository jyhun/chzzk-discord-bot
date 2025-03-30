package com.streampulse.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StreamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Streamer streamer;

    private String title;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;


    public void updateEndedAt() {
        if (endedAt == null) {
            this.endedAt = LocalDateTime.now();
        }
    }
}
