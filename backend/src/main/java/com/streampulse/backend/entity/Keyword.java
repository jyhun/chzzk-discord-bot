package com.streampulse.backend.entity;

import com.streampulse.backend.enums.EventType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Keyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    private Subscription subscription;

    @Column(length = 100)
    private String value;

    @Enumerated(EnumType.STRING)
    private EventType eventType;
}