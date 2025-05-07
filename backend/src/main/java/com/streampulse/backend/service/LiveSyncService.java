package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.RedisLiveStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveSyncService {

    private final LiveHandlerService liveHandlerService;
    private final RedisLiveStore redisLiveStore;
    private final ChzzkLiveService chzzkLiveService;
    private final Environment environment;

    private final AtomicBoolean firstRun = new AtomicBoolean(true);

    @LogExecution
    @Transactional
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

        liveHandlerService.handleStart(startIds, dtoMap, firstRun);
        liveHandlerService.handleEnd(endIds);
        liveHandlerService.handleTopic(nextIds, dtoMap, firstRun);

        firstRun.set(false);
    }

    @PostConstruct
    public void init() {
        boolean isProd = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod"));
        if (!isProd) {
            redisLiveStore.clearLiveSet();
        }
    }

}
