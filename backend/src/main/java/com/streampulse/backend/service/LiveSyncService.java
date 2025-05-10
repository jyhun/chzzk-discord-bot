package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.RedisLiveStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LiveSyncService {

    private final ChzzkLiveService chzzkLiveService;
    private final RedisLiveStore redisLiveStore;
    private final LiveHandlerService liveHandlerService;

    @LogExecution
    public void syncLiveBroadcasts() {
        List<LiveResponseDTO> liveList = Optional.ofNullable(chzzkLiveService.collectLiveBroadcastersFromRedis())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(dto -> dto.getChannelId() != null)
                .toList();

        log.info("syncLiveBroadcasts size: {}", liveList.size());
        if (liveList.isEmpty()) return;

        Map<String, LiveResponseDTO> dtoMap = liveList.stream()
                .collect(Collectors.toMap(LiveResponseDTO::getChannelId, dto -> dto, (e, r) -> e));

        Set<String> nextIds = dtoMap.keySet();

        Set<String> prevIds = redisLiveStore.getLiveKeys();

        Set<String> endIds = prevIds.stream()
                .filter(id -> !nextIds.contains(id))
                .collect(Collectors.toSet());

        liveHandlerService.handleStart(nextIds, dtoMap);
        liveHandlerService.handleEnd(endIds);
        liveHandlerService.handleTopic(nextIds, dtoMap);

        redisLiveStore.clearAllLiveKeys();
        nextIds.forEach(redisLiveStore::setLiveKey);
    }

}
