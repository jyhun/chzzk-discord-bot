package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.RedisLiveStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LiveSyncService {

    private final ChzzkLiveService chzzkLiveService;
    private final RedisLiveStore redisLiveStore;
    private final LiveHandlerService liveHandlerService;

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
        Set<String> prevIds = redisLiveStore.getStaticKeys();

        Set<String> startIds = new HashSet<>(nextIds);
        startIds.removeAll(prevIds);

        Set<String> endIds = new HashSet<>(prevIds);
        endIds.removeAll(nextIds);

        if (firstRun.get()) {
            firstRun.set(false);
            nextIds.forEach(redisLiveStore::setStaticKey);
            nextIds.forEach(redisLiveStore::saveLiveKey);
            nextIds.stream()
                    .filter(redisLiveStore::hasLiveKey)
                    .forEach(redisLiveStore::updateLiveTtl);
            return;
        }

        liveHandlerService.handleStart(startIds, dtoMap);
        liveHandlerService.handleEnd(endIds);
        liveHandlerService.handleTopic(nextIds, dtoMap);

        endIds.forEach(redisLiveStore::deleteStaticKey);
        startIds.forEach(redisLiveStore::setStaticKey);
        startIds.forEach(redisLiveStore::saveLiveKey);
        nextIds.stream()
                .filter(redisLiveStore::hasLiveKey)
                .forEach(redisLiveStore::updateLiveTtl);
        endIds.forEach(redisLiveStore::deleteLiveKey);
        endIds.forEach(redisLiveStore::deleteSnapshot);
    }

    @PostConstruct
    public void init() {
        // 앱 재시작 시 이전 상태 초기화
        redisLiveStore.clearAllStaticKeys();
    }

}
