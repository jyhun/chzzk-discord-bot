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
public class Keyword extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, length = 100)
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;
}