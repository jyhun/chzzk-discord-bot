package com.streampulse.backend.entity;

import com.streampulse.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Streamer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String channelId;

    @Column(nullable = false)
    private String nickname;

    @Column(nullable = false)
    private boolean live;

    @Column(nullable = false)
    private int averageViewerCount;

    @OneToMany(mappedBy = "streamer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StreamSession> streamSessions = new ArrayList<>();

    public void updateLive(boolean live) {
        this.live = live;
    }

    public void updateAverageViewerCount(int averageViewerCount) {
        this.averageViewerCount = averageViewerCount;
    }

}
