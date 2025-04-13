package com.streampulse.backend.repository;

import com.streampulse.backend.entity.Subscription;
import com.streampulse.backend.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription,Long> {
    List<Subscription> findByStreamerIsNullAndEventTypeAndActiveTrue(EventType eventType);

    List<Subscription> findByStreamer_ChannelIdAndEventTypeAndActiveTrue(String streamerId, EventType eventType);
}
