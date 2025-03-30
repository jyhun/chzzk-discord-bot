package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StreamMetricsRepository extends JpaRepository<StreamMetrics, Long> {
    List<StreamMetrics> findBySessionId(Long sessionId);

    Optional<StreamMetrics> findTopBySessionOrderByCollectedAtDesc(StreamSession streamSession);

    Optional<StreamMetrics> findTopBySessionAndCollectedAtLessThanOrderByCollectedAtDesc(StreamSession session, LocalDateTime collectedAtIsLessThan);
}
