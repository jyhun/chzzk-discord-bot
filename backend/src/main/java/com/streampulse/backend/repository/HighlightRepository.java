package com.streampulse.backend.repository;

import com.streampulse.backend.entity.Highlight;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HighlightRepository extends JpaRepository<Highlight, Long> {
}
