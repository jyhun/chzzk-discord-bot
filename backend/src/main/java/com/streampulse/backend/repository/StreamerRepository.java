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


    List<Streamer> findAllByChannelIdIn(@Param("channelIds") Collection<String> channelIds);

    @Modifying
    @Query("UPDATE Streamer s SET s.live = false WHERE s.channelId IN :ids")
    int markOffline(@Param("ids") Collection<String> ids);

    @Query("SELECT s.channelId FROM Streamer s WHERE s.channelId IN :ids AND s.live = true")
    List<String> findLiveChannelIds(@Param("ids") Collection<String> ids);

    @Modifying
    @Query(value = """
        INSERT INTO streamer (channel_id, nickname, average_viewer_count, live, created_at, updated_at)
        VALUES (:channelId, :nickname, :averageViewerCount, :live, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            nickname = VALUES(nickname),
            average_viewer_count = VALUES(average_viewer_count),
            live = VALUES(live),
            updated_at = NOW()
        """, nativeQuery = true)
    void upsertStreamer(
            @Param("channelId") String channelId,
            @Param("nickname") String nickname,
            @Param("averageViewerCount") int averageViewerCount,
            @Param("live") boolean live
    );

}
