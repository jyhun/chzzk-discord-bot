package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.RedisLiveStore;
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
        Set<String> currIds = firstRun.get() ? redisLiveStore.getStaticKeys() : redisLiveStore.getLiveKeys();

        Set<String> startIds = new HashSet<>(nextIds);
        startIds.removeAll(currIds);

        Set<String> endIds = new HashSet<>(currIds);
        endIds.removeAll(nextIds);

        liveHandlerService.handleStart(startIds, dtoMap);
        liveHandlerService.handleEnd(endIds, dtoMap);
        liveHandlerService.handleTopic(nextIds, dtoMap);

        if (firstRun.get()) {
            redisLiveStore.clearAllStaticKeys();            // 기존 전체 삭제
            nextIds.forEach(redisLiveStore::setStaticKey);  // 현재 방송중 ID 전부 등록
        } else {
            startIds.forEach(redisLiveStore::setStaticKey);
        }

        endIds.forEach(redisLiveStore::deleteStaticKey);

        // 조건부 TTL 연장 또는 키 생성
        nextIds.forEach(channelId -> {
            if (redisLiveStore.hasLiveKey(channelId)) {
                redisLiveStore.updateLiveTtl(channelId); // TTL만 연장
            } else {
                redisLiveStore.saveLiveKey(channelId); // 새로 생성
            }
        });

        firstRun.set(false);

    }

}
