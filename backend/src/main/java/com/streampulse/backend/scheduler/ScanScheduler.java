package com.streampulse.backend.scheduler;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.service.ChzzkLiveService;
import com.streampulse.backend.service.LiveSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

@Component
@RequiredArgsConstructor
public class ScanScheduler {

    private final LiveSyncService liveSyncService;
    private final ChzzkLiveService chzzkLiveService;
    private final RedisTemplate<String, String> redisTemplate;

    private final ReentrantLock lock = new ReentrantLock();

    private static final String REDIS_KEY = "deepScan:ready";

    @LogExecution
    @Scheduled(initialDelay = 0, fixedDelay = 10 * 60 * 1000 * 6) // 1시간
    public void deepScan() {
        chzzkLiveService.fetchAndStoreValidCursors();
        redisTemplate.opsForValue().set(REDIS_KEY, "true", Duration.ofDays(1));
    }

    @LogExecution
    @Scheduled(fixedRate = 60 * 1000) // 1분
    public void fastScan() {
        String ready = redisTemplate.opsForValue().get(REDIS_KEY);
        if (!"true".equals(ready)) return;
        if (!lock.tryLock()) return;
        try {
            liveSyncService.syncLiveBroadcasts();
        } finally {
            lock.unlock();
        }
    }

}
