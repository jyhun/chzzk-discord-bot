package com.streampulse.backend.repository;

import com.streampulse.backend.entity.Streamer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamerRepository extends JpaRepository<Streamer, Long> {
}
