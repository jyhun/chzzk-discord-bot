package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {
    Optional<StreamSession> findFirstByStreamer_ChannelIdAndEndedAtIsNullOrderByStartedAtDesc(String channelId);
    List<StreamSession> findByStreamerId(Long id);
    List<StreamSession> findAllByStreamerIn(Collection<Streamer> streamers);
    boolean existsByStreamer_ChannelIdAndEndedAtIsNull(String channelId);
    List<StreamSession> findByStreamerIdAndEndedAtAfter(Long streamerId, LocalDateTime threshold);
    @Query("SELECT s.streamer.id, AVG(s.averageViewerCount), MAX(s.averageViewerCount) FROM StreamSession s WHERE s.endedAt > :threshold GROUP BY s.streamer.id")
    List<Object[]> getStreamerAvgViewerCount(@Param("threshold") LocalDateTime threshold);


    @Query("SELECT s.id FROM StreamSession s WHERE s.createdAt < :threshold ORDER BY s.createdAt ASC")
    List<Long> fetchSessionIdsForUpdate(@Param("threshold") LocalDateTime threshold, Pageable pageable);


    @Modifying
    @Query(
            value =
                    "UPDATE stream_session s " +
                            "JOIN ( " +
                            "  SELECT sm.stream_session_id AS id, " +
                            "         AVG(sm.viewer_count) AS avg_v, " +
                            "         MAX(sm.viewer_count) AS peak_v " +
                            "  FROM stream_metrics sm " +
                            "  WHERE sm.stream_session_id IN :sessionIds " +
                            "  AND sm.created_at > :threshold " +
                            "  GROUP BY sm.stream_session_id" +
                            ") m ON s.id = m.id " +
                            "SET s.average_viewer_count = m.avg_v, " +
                            "    s.peak_viewer_count = m.peak_v",
            nativeQuery = true
    )
    int bulkUpdateSessionStats(@Param("sessionIds") List<Long> sessionIds, @Param("threshold") LocalDateTime threshold);


}
