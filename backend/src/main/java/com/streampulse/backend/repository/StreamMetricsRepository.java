package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamMetrics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamMetricsRepository extends JpaRepository<StreamMetrics, Long> {
}
