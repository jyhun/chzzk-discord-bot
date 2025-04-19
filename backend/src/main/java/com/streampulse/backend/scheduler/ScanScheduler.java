package com.streampulse.backend.scheduler;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.service.ChzzkLiveService;
import com.streampulse.backend.service.LiveSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@RequiredArgsConstructor
public class ScanScheduler {

    private final LiveSyncService liveSyncService;
    private final ChzzkLiveService chzzkLiveService;

    private final AtomicBoolean deepScanFinished = new AtomicBoolean(false);

    @LogExecution
    @Scheduled(initialDelay = 0, fixedDelay = 10 * 60 * 1000)
    public void deepScan() {
        try {
            chzzkLiveService.fetchAndStoreValidCursors();
        } finally {
            deepScanFinished.set(true);
        }
    }

    @LogExecution
    @Scheduled(fixedDelay = 60 * 1000)
    public void fastScan() {
        if (!deepScanFinished.get()) return;

        liveSyncService.syncLiveBroadcasts();
    }

}
