package com.streampulse.backend.repository;

import com.streampulse.backend.entity.Streamer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StreamerRepository extends JpaRepository<Streamer, Long> {
    Optional<Streamer> findByChannelId(String channelId);
    List<Streamer> findByLiveIsTrue();
}
