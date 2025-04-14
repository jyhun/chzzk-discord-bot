package com.streampulse.backend.repository;

import com.streampulse.backend.entity.Subscription;
import com.streampulse.backend.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.streamer IS NULL " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findGlobalSubscriptionsByEventType(@Param("eventType") EventType eventType);

    @Query("SELECT s FROM Subscription s " +
            "WHERE s.streamer.channelId = :streamerId " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findStreamerSubscriptionsByEventType(@Param("streamerId") String streamerId,
                                                            @Param("eventType") EventType eventType);

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.streamer IS NULL " +
            "AND s.eventType = :eventType " +

            "AND s.active = true")
    boolean existsGlobalSubscription(
            @Param("discordChannelId") String discordChannelId,
            @Param("eventType") EventType eventType
    );

    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.streamer.channelId = :streamerChannelId " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    boolean existsStreamerSubscription(
            @Param("discordChannelId") String discordChannelId,
            @Param("streamerChannelId") String streamerChannelId,
            @Param("eventType") EventType eventType
    );

}
