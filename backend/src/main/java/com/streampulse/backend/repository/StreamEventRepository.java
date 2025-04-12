package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamEventRepository extends JpaRepository<StreamEvent, Long> {
}
