package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {

    private final LiveSyncService liveSyncService;
    private final ChzzkLiveService chzzkLiveService;
    private final StringRedisTemplate redisTemplate;
    private final ReentrantLock lock = new ReentrantLock();
    private static final String REDIS_KEY = "cursor_zset:current";

    @LogExecution
    public void doDeepScan() {
        int maxAttempts = 3;

        for(int i = 1; i <= maxAttempts; i++) {
            boolean success = chzzkLiveService.fetchAndStoreValidCursors();
            if (success) {
                log.info("DeepScan 성공 ({}회 시도)", i);
                return;
            }

            log.warn("DeepScan 실패 ({} 회차), 재시도 중...", i);
            try {
                Thread.sleep(5000);
            }catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.error("DeepScan {}회 시도 후 실패.", maxAttempts);
    }

    public void doFastScan() {
        Boolean exists = redisTemplate.hasKey(REDIS_KEY);
        if (!exists) return;
        if (!lock.tryLock()) return;
        try {
            liveSyncService.syncLiveBroadcasts();
        } finally {
            lock.unlock();
        }
    }
}
