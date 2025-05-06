package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveSyncAsyncService {

    private final LiveSyncHandleService liveSyncHandleService;

    @Async("taskExecutor")
    public Future<?> handleStart(Set<String> startIds, Map<String, LiveResponseDTO> dtoMap, AtomicBoolean firstRun) {
        return CompletableFuture.runAsync(() -> {
            liveSyncHandleService.handleStart(startIds, dtoMap, firstRun);
        });
    }

    @Async("taskExecutor")
    public Future<?> handleTopic(Set<String> nextIds, Map<String, LiveResponseDTO> dtoMap, AtomicBoolean firstRun) {
        return CompletableFuture.runAsync(() -> {
            liveSyncHandleService.handleTopic(nextIds, dtoMap, firstRun);
        });
    }

    @Async("taskExecutor")
    public Future<?> handleEnd(Set<String> endIds) {
        return CompletableFuture.runAsync(() -> {
            liveSyncHandleService.handleEnd(endIds);
        });
    }

}
