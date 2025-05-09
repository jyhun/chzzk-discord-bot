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
        log.info("DeepScan 시작");
        try {
            schedulerService.doDeepScan();
        } catch (Exception e) {
            log.error("deepScan 실행 중 오류 발생", e);
        }
    }

    @Scheduled(cron = "0 * * * * *")
    public void fastScan() {
        log.info("FastScan 시작");
        try {
            schedulerService.doFastScan();
        } catch (Exception e) {
            log.error("FastScan 실행 중 오류 발생", e);
        }
    }
}
