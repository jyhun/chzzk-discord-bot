package com.streampulse.backend.entity;

import com.streampulse.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Tag extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_session_id")
    private StreamSession streamSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_metrics_id")
    private StreamMetrics streamMetrics;

    public static Tag forMetrics(String value, StreamMetrics metrics) {
        return Tag.builder()
                .value(value)
                .streamMetrics(metrics)
                .build();
    }

    public void assignStreamSession(StreamSession streamSession) {
        this.streamSession = streamSession;
    }

    public void assignStreamMetrics(StreamMetrics streamMetrics) {
        this.streamMetrics = streamMetrics;
    }

}
