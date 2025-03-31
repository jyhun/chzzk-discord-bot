package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {
    Optional<StreamSession> findByStreamer_ChannelIdAndEndedAtIsNull(String channelId);
    Optional<StreamSession> findByStreamerId(Long id);
}
