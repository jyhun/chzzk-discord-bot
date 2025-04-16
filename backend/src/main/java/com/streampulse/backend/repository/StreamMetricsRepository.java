package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamMetricsRepository extends JpaRepository<StreamMetrics, Long> {
    List<StreamMetrics> findByStreamSessionId(Long sessionId);
}
