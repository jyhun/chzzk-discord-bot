package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final LiveSyncService liveSyncService;
    private final ChzzkLiveService chzzkLiveService;
    private final RedisTemplate<String,String> redisTemplate;
    private final ReentrantLock lock = new ReentrantLock();
    private static final String REDIS_KEY = "cursor_zset:current";

    @LogExecution
    public void doDeepScan() {
        chzzkLiveService.fetchAndStoreValidCursors();
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
