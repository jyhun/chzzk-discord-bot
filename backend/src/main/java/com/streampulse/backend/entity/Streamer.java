package com.streampulse.backend.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Streamer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String channelId;

    private String nickname;

    private boolean live;

    private int averageViewerCount;

    private LocalDateTime updatedAt;

    public void updateLive(boolean live) {
        this.live = live;
    }

    public void updateAverageViewerCount(int averageViewerCount) {
        this.averageViewerCount = averageViewerCount;
    }

}
