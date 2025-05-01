package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {
    Optional<StreamSession> findByStreamer_ChannelIdAndEndedAtIsNull(String channelId);
    List<StreamSession> findByStreamerId(Long id);
    List<StreamSession> findAllByStreamerIn(Collection<Streamer> streamers);
    boolean existsByStreamer_ChannelIdAndEndedAtIsNull(String channelId);
    List<StreamSession> findByStreamerIdAndEndedAtAfter(Long streamerId, LocalDateTime threshold);

    @Modifying
    @Query(
            value =
                    "UPDATE stream_session s " +
                            "JOIN ( " +
                            "  SELECT stream_session_id AS id, " +
                            "         AVG(viewer_count) AS avg_v, " +
                            "         MAX(viewer_count) AS peak_v " +
                            "  FROM stream_metrics " +
                            "  WHERE created_at > :threshold " +
                            "  GROUP BY stream_session_id" +
                            ") m ON s.id = m.id " +
                            "SET s.average_viewer_count = m.avg_v, " +
                            "    s.peak_viewer_count   = m.peak_v",
            nativeQuery = true
    )
    int bulkUpdateSessionStats(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM StreamSession s WHERE s.endedAt < :threshold")
    int deleteOldSessions(@Param("threshold") LocalDateTime threshold);

}
