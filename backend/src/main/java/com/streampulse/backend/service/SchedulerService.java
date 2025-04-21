package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final LiveSyncService liveSyncService;
    private final ChzzkLiveService chzzkLiveService;
    private final RedisTemplate<String,String> redisTemplate;
    private final ReentrantLock lock = new ReentrantLock();
    private static final String REDIS_KEY = "deepScan:ready";

    @LogExecution
    public void doDeepScan() {
        chzzkLiveService.fetchAndStoreValidCursors();
        redisTemplate.opsForValue().set(REDIS_KEY, "true", Duration.ofDays(1));
    }

    @LogExecution
    public void doFastScan() {
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
