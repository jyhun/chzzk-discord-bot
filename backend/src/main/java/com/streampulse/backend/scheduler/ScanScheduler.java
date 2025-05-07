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

    @Scheduled(fixedDelay = 10 * 60 * 1000 * 6)
    public void deepScan() {
        log.info("deepScan 시작");
        schedulerService.doDeepScan();
    }

    @Scheduled(cron = "0 * * * * *")
    public void fastScan() {
        log.info("fastScan 시작");
        schedulerService.doFastScan();
    }
}
