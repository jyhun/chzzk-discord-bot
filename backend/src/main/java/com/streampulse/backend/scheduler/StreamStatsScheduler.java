package com.streampulse.backend.scheduler;

import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamSessionRepository;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
//    @Scheduled(cron = "${app.scheduler.cron}")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void aggregateAllStats() {
        long startTime = System.nanoTime();
        log.info("[방송 통계 집계] 집계 시작");

        // 세션 집계
        aggregateSessionStats();

        // 스트리머 집계
        aggregateStreamerStats();

        long elapsed = System.nanoTime() - startTime;
        log.info("[방송 통계 집계] 전체 집계 완료 (총 소요 시간: {} ms)", elapsed / 1_000_000);
    }

    public void aggregateSessionStats() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(1);
        int updatedSessions = 0;

        List<Long> sessionIds;
        int batchCount = 0;

        while (true) {
            Pageable pageable = PageRequest.of(batchCount, batchSize);
            sessionIds = streamSessionRepository.fetchSessionIdsForUpdate(threshold, pageable);

            if (sessionIds.isEmpty()) {
                break;
            }

            updatedSessions += streamSessionRepository.bulkUpdateSessionStats(sessionIds, threshold);
            batchCount++;
            log.info("[방송 통계 집계] 배치 {}번: {}개의 세션 시청자 수 업데이트 완료", batchCount, sessionIds.size());
        }

        log.info("[방송 통계 집계] 총 {}개의 세션 시청자 수 업데이트 완료", updatedSessions);
    }

    public void aggregateStreamerStats() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(retentionDays);
        List<Object[]> streamerAvgStats = streamSessionRepository.getStreamerAvgViewerCount(threshold);

        List<Streamer> streamers = new ArrayList<>();
        for (Object[] stat : streamerAvgStats) {
            Long streamerId = (Long) stat[0];
            int avgViewer = (int) Math.round((Double) stat[1]);
            Streamer streamer = streamerRepository.findById(streamerId).orElseThrow();
            streamer.updateAverageViewerCount(avgViewer);
            streamers.add(streamer);
        }

        streamerRepository.saveAll(streamers);
        log.info("[방송 통계 집계] 스트리머 평균 시청자 수 업데이트 완료");
    }


    /**
     * 오래된 세션(및 매트릭 자동 cascade) 삭제 스케줄러
     */
//    @Scheduled(cron = "${app.scheduler.cron}")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cleanupOldData() {
        long startTime = System.nanoTime();
        log.info("[방송 데이터 삭제] 오래된 세션 및 매트릭 삭제 시작");

        try {
            jdbcTemplate.update("CALL delete_old_stream_sessions_and_metrics_safe()");
            log.info("[방송 데이터 삭제] 프로시저 실행 완료");
        } catch (Exception e) {
            log.error("[방송 데이터 삭제] 프로시저 실행 중 오류 발생: {}", e.getMessage(), e);
        }
        long elapsed = System.nanoTime() - startTime;
        log.info("[방송 데이터 삭제] 삭제 완료 (총 소요 시간: {} ms)", elapsed / 1_000_000);
    }
}
