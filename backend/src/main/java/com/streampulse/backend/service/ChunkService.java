package com.streampulse.backend.service;

import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamMetricsRepository;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 청크 단위로 각각의 엔티티를 별도 트랜잭션으로 저장합니다.
 */
@Service
@RequiredArgsConstructor
public class ChunkService {

    private final StreamerRepository streamerRepository;
    private final StreamSessionRepository streamSessionRepository;
    private final StreamMetricsRepository streamMetricsRepository;
    private final JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Streamer 엔티티 리스트를 REQUIRES_NEW 트랜잭션으로 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveStreamersChunk(List<Streamer> chunk) {
        streamerRepository.saveAll(chunk);
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * StreamSession 엔티티 리스트를 REQUIRES_NEW 트랜잭션으로 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSessionsChunk(List<StreamSession> chunk) {
        streamSessionRepository.saveAll(chunk);
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * StreamMetrics 엔티티 리스트를 REQUIRES_NEW 트랜잭션으로 저장합니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveMetricsChunk(List<StreamMetrics> chunk) {
        streamMetricsRepository.saveAll(chunk);
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Streamer 리스트를 chunk 단위로 bulk upsert 수행 (JDBC 직접 실행)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertStreamersChunk(List<Streamer> chunk) {
        if (chunk.isEmpty()) return;

        StringBuilder sql = new StringBuilder();
        sql.append("""
            INSERT INTO streamer (channel_id, nickname, average_viewer_count, live)
            VALUES 
        """);

        List<Object> params = new java.util.ArrayList<>();
        for (int i = 0; i < chunk.size(); i++) {
            Streamer s = chunk.get(i);
            sql.append("(?, ?, ?, ?)");
            if (i < chunk.size() - 1) {
                sql.append(",");
            }
            params.add(s.getChannelId());
            params.add(s.getNickname());
            params.add(s.getAverageViewerCount());
            params.add(s.isLive());
        }

        sql.append("""
            ON DUPLICATE KEY UPDATE 
                nickname = VALUES(nickname), 
                average_viewer_count = VALUES(average_viewer_count), 
                live = VALUES(live)
        """);

        jdbcTemplate.update(sql.toString(), params.toArray());
    }

}
