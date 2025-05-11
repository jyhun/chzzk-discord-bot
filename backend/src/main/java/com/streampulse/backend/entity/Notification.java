package com.streampulse.backend.entity;

import com.streampulse.backend.common.BaseTimeEntity;
import com.streampulse.backend.enums.EventType;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;

    @Column(nullable = false, length = 100)
    private String receiverId;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private boolean success;

    private LocalDateTime sentAt;

}
