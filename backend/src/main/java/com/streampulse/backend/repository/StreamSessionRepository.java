package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {
    Optional<StreamSession> findByStreamer_ChannelIdAndStartedAt(String channelId, LocalDateTime startedAt);
    List<StreamSession> findByStreamerIdAndEndedAtIsNotNull(Long id);

    @Query("SELECT s FROM StreamSession s WHERE s.streamer = :streamer AND s.startedAt = :startedAt ORDER BY s.id DESC")
    List<StreamSession> findByStreamerAndStartedAt(@Param("streamer") Streamer streamer,
                                                   @Param("startedAt") LocalDateTime startedAt);


    List<StreamSession> findByStreamerIdAndEndedAtIsNull(Long id);
}
