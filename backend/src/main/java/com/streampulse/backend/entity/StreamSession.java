package com.streampulse.backend.entity;

import com.streampulse.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StreamSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "streamer_id", nullable = false)
    private Streamer streamer;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private int averageViewerCount;

    private int peakViewerCount;

    public void updateEndedAt() {
        if (endedAt == null) {
            this.endedAt = LocalDateTime.now();
        }
    }

    public void updateAverageViewerCount(int averageViewerCount) {
        this.averageViewerCount = averageViewerCount;
    }

    public void updatePeakViewerCount(int peakViewerCount) {
        this.peakViewerCount = peakViewerCount;
    }

}
