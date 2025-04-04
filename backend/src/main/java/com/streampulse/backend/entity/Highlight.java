package com.streampulse.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Highlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private StreamSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    private StreamMetrics metrics;

    @Column(length = 500)
    private String summary;

    private boolean notified;

    private LocalDateTime detectedAt;

    private int chatCount;

    private float score;

    public void updateNotified(boolean notified) {
        this.notified = notified;
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }
}
