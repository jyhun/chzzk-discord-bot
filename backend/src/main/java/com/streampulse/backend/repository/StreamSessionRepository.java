package com.streampulse.backend.repository;

import com.streampulse.backend.entity.StreamSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamSessionRepository extends JpaRepository<StreamSession, Long> {
}
