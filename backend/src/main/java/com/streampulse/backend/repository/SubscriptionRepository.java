package com.streampulse.backend.repository;

import com.streampulse.backend.entity.Subscription;
import com.streampulse.backend.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // 📌 사용자의 전체 구독 조회 (디스코드 채널 기준)
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.active = true")
    List<Subscription> findByActiveChannel(@Param("discordChannelId") String discordChannelId);

    // 📌 사용자의 이벤트별 구독 조회
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findByActiveChannelAndEvent(@Param("discordChannelId") String discordChannelId,
                                                   @Param("eventType") EventType eventType);

    // 📌 사용자의 이벤트 + 특정 방송자 구독 조회
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.streamer.channelId = :streamerId " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findByActiveChannelAndStreamerAndEvent(@Param("discordChannelId") String discordChannelId,
                                                              @Param("streamerId") String streamerId,
                                                              @Param("eventType") EventType eventType);

    // 📌 알림 발송용: 특정 방송자 구독자 조회
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.streamer.channelId = :streamerId " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findByActiveStreamerAndEvent(@Param("streamerId") String streamerId,
                                                    @Param("eventType") EventType eventType);

    // 📌 알림 발송용: 전체 구독자 조회 (Global)
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.streamer IS NULL " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findByActiveGlobalAndEvent(@Param("eventType") EventType eventType);

    // 📌 구독 존재 여부 체크: Global
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.streamer IS NULL " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    boolean existsActiveGlobalSubscription(@Param("discordChannelId") String discordChannelId,
                                           @Param("eventType") EventType eventType);

    // 📌 구독 존재 여부 체크: 특정 방송자
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END " +
            "FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.streamer.channelId = :streamerChannelId " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    boolean existsActiveStreamerSubscription(@Param("discordChannelId") String discordChannelId,
                                             @Param("streamerChannelId") String streamerChannelId,
                                             @Param("eventType") EventType eventType);

    // 📌 구독 해제용 (Global)
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.streamer IS NULL " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findByActiveChannelAndGlobalAndEvent(@Param("discordChannelId") String discordChannelId,
                                                            @Param("eventType") EventType eventType);

    // 📌 구독 해제용 (특정 방송자)
    @Query("SELECT s FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.streamer.channelId = :streamerId " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findByActiveChannelAndStreamerAndEventForDeactivate(@Param("discordChannelId") String discordChannelId,
                                                                           @Param("streamerId") String streamerId,
                                                                           @Param("eventType") EventType eventType);

    @Query("SELECT s FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND s.active = true")
    List<Subscription> findActiveByChannel(@Param("discordChannelId") String discordChannelId);

    @Query("SELECT s FROM Subscription s " +
            "WHERE s.streamer.channelId = :channelId " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findByStreamer_ChannelIdAndEventTypeAndActiveTrue(
            @Param("channelId") String channelId,
            @Param("eventType") EventType eventType);

    @Query("SELECT s FROM Subscription s " +
            "WHERE s.streamer IS NULL " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    List<Subscription> findByStreamerIsNullAndEventTypeAndActiveTrue(@Param("eventType") EventType eventType);

    @Query("SELECT s FROM Subscription s " +
            "WHERE s.discordChannel.discordChannelId = :discordChannelId " +
            "AND (:streamerChannelId IS NULL AND s.streamer IS NULL OR s.streamer.channelId = :streamerChannelId) " +
            "AND s.eventType = :eventType " +
            "AND s.active = true")
    Optional<Subscription> findActiveByChannelAndStreamerAndEventType(
            @Param("discordChannelId") String discordChannelId,
            @Param("streamerChannelId") String streamerChannelId,
            @Param("eventType") EventType eventType
    );


}