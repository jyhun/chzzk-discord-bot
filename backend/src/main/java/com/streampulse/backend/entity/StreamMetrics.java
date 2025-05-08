package com.streampulse.backend.entity;

import com.streampulse.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class StreamMetrics extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_session_id", nullable = false)
    private StreamSession streamSession;

    @Column(nullable = false)
    private int viewerCount;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

}
