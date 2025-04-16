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
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stream_event_id", nullable = false)
    private StreamEvent streamEvent;

    @Column(nullable = false, length = 100)
    private String receiverId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 500)
    private String errorMessage;

    private LocalDateTime sentAt;

}
