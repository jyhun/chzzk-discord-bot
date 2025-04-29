package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {
    Optional<StreamSession> findByStreamer_ChannelIdAndEndedAtIsNull(String channelId);
    List<StreamSession> findByStreamerId(Long id);
    List<StreamSession> findAllByStreamerIn(Collection<Streamer> streamers);
}
