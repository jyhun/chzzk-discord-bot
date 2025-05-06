package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.RedisLiveStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LiveSyncService {

    private final ChzzkLiveService chzzkLiveService;
    private final RedisLiveStore redisLiveStore;
    private final LiveSyncAsyncService liveSyncAsyncService;
    private final AtomicBoolean firstRun = new AtomicBoolean(true);

    @LogExecution
    public void syncLiveBroadcasts() {
        List<LiveResponseDTO> liveList = Optional.ofNullable(chzzkLiveService.collectLiveBroadcastersFromRedis())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(dto -> dto.getChannelId() != null)
                .toList();
        if (liveList.isEmpty()) return;

        Map<String, LiveResponseDTO> dtoMap = liveList.stream()
                .collect(Collectors.toMap(LiveResponseDTO::getChannelId, dto -> dto, (e, r) -> e));

        if (dtoMap.isEmpty()) return;

        Set<String> nextIds = dtoMap.keySet();
        Set<String> currIds = redisLiveStore.getLiveStreamerIds();

        Set<String> startIds = new HashSet<>(nextIds);
        startIds.removeAll(currIds);
        Set<String> endIds = new HashSet<>(currIds);
        endIds.removeAll(nextIds);

        redisLiveStore.updateLiveSet(startIds, endIds);

        try {
            Future<?> startFuture = liveSyncAsyncService.handleStart(startIds, dtoMap,firstRun);
            startFuture.get();

            Future<?> endFuture = liveSyncAsyncService.handleEnd(endIds);
            endFuture.get();

            Future<?> topicFuture = liveSyncAsyncService.handleTopic(nextIds, dtoMap,firstRun);
            topicFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            log.error("비동기 처리 에러: {}", e.getMessage());
        }

        firstRun.set(false);
    }

    @PostConstruct
    public void init() {
        redisLiveStore.clearLiveSet();
    }
}
