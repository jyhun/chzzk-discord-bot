package com.streampulse.backend.repository;

import com.streampulse.backend.entity.Streamer;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StreamerRepository extends JpaRepository<Streamer, Long> {
    Optional<Streamer> findByChannelId(String channelId);
    List<Streamer> findAllByChannelIdIn(Collection<String> channelIds);
    @Modifying
    @Query("UPDATE Streamer s SET s.live = false WHERE s.channelId IN :ids")
    int markOffline(@Param("ids") Collection<String> ids);
}
