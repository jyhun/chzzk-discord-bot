package com.streampulse.backend.entity;

import com.streampulse.backend.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StreamEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private StreamSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    private StreamMetrics metrics;

    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(length = 500)
    private String summary;

    private boolean notified;

    private LocalDateTime detectedAt;

    private int viewerCount;

    private int chatCount;

    private float viewerIncreaseRate;

    public void updateNotified(boolean notified) {
        this.notified = notified;
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }
}
