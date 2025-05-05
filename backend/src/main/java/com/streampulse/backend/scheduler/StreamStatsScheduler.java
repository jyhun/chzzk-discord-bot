package com.streampulse.backend.scheduler;

import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StreamStatsScheduler {

    private final StreamSessionRepository streamSessionRepository;
    private final StreamerRepository streamerRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.scheduler.retention-days}")
    private int retentionDays;

    @Value("${app.scheduler.batch-size}")
    private int batchSize;

    /**
     * 세션/스트리머 집계 실행
     */
    @Scheduled(cron = "${app.scheduler.cron}")
    @Transactional
    public void aggregateAllStats() {
        long startTime = System.nanoTime();
        log.info("[방송 통계 집계] 집계 시작");

        // 1. 세션 bulk 업데이트
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        int updatedSessions = streamSessionRepository.bulkUpdateSessionStats(threshold);
        log.info("[방송 통계 집계] {}개의 세션 시청자 수 업데이트 완료", updatedSessions);

        // 2. 스트리머 페이지별 평균 뷰어 집계
        int page = 0;
        while (true) {
            Page<Streamer> pageData = streamerRepository.findAll(PageRequest.of(page, batchSize));
            List<Streamer> streamers = pageData.getContent();
            if (streamers.isEmpty()) break;

            for (Streamer streamer : streamers) {
                List<StreamSession> sessions = streamSessionRepository
                        .findByStreamerIdAndEndedAtAfter(streamer.getId(), threshold);
                if (sessions.isEmpty()) continue;

                int sumViewer = sessions.stream()
                        .mapToInt(StreamSession::getAverageViewerCount)
                        .sum();
                int avgViewer = sumViewer / sessions.size();
                streamer.updateAverageViewerCount(avgViewer);
            }
            streamerRepository.saveAll(streamers);
            log.info("[방송 통계 집계] {}페이지 스트리머 {}명 평균 시청자 수 및 최고 시청자 수 업데이트 완료", page, streamers.size());
            if (pageData.isLast()) break;
            page++;
        }

        long elapsed = System.nanoTime() - startTime;
        log.info("[방송 통계 집계] 전체 집계 완료 (총 소요 시간: {} ms)", elapsed / 1_000_000);
    }

    /**
     * 오래된 세션(및 매트릭 자동 cascade) 삭제 스케줄러
     */
    @Scheduled(cron = "${app.scheduler.cron}")
    @Transactional
    public void cleanupOldData() {
        long startTime = System.nanoTime();
        log.info("[방송 데이터 삭제] 오래된 세션 및 매트릭 삭제 시작");

        try {
            jdbcTemplate.execute("CALL delete_old_stream_sessions_and_metrics_safe()");
            log.info("[방송 데이터 삭제] 프로시저 실행 완료");
        } catch (Exception e) {
            log.error("[방송 데이터 삭제] 프로시저 실행 중 오류 발생: {}", e.getMessage(), e);
        }
        long elapsed = System.nanoTime() - startTime;
        log.info("[방송 데이터 삭제] 삭제 완료 (총 소요 시간: {} ms)", elapsed / 1_000_000);
    }
}
