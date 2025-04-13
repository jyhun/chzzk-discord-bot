package com.streampulse.backend.scheduler;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.service.LiveSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LiveSyncScheduler {

    private final LiveSyncService liveSyncService;

//    @Scheduled(fixedRate = 60000)
    @LogExecution
    public void scheduleSyncLiveBroadcasts() {
        liveSyncService.syncLiveBroadcasts();
    }

}
