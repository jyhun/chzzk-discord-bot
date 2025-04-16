package com.streampulse.backend.entity;

import com.streampulse.backend.common.BaseTimeEntity;
import com.streampulse.backend.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StreamEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_metrics_id", nullable = false)
    private StreamMetrics streamMetrics;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false)
    private int viewerCount;

    @Column(nullable = false)
    private float viewerIncreaseRate;

    @Column(length = 500)
    private String summary;

    private int chatCount;

    public void updateSummary(String summary) {
        this.summary = summary;
    }

    public void updateChatCount(int chatCount) {
        this.chatCount = chatCount;
    }
}
