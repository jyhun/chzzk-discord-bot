package com.streampulse.backend.scheduler;

import com.streampulse.backend.service.SchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScanScheduler {

    private final SchedulerService schedulerService;

    @Scheduled(initialDelay = 0, fixedDelay = 10 * 60 * 1000 * 6)
    public void deepScan() {
        log.info("deepScan 시작");
        schedulerService.doDeepScan();
    }

    @Scheduled(fixedRate = 60 * 1000)
    public void fastScan() {
        schedulerService.doFastScan();
    }
}
