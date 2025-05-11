package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.infra.RedisLiveStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    private static final long END_THRESHOLD_SECONDS = 600;

    @LogExecution
    public void syncLiveBroadcasts() {
        List<LiveResponseDTO> liveList = Optional.ofNullable(chzzkLiveService.collectLiveBroadcastersFromRedis())
                .orElse(Collections.emptyList())
                .stream()
                .filter(Objects::nonNull)
                .filter(dto -> dto.getChannelId() != null)
                .toList();

        log.info("syncLiveBroadcasts size: {}", liveList.size());

        Map<String, LiveResponseDTO> dtoMap = liveList.stream()
                .collect(Collectors.toMap(LiveResponseDTO::getChannelId, dto -> dto, (e, r) -> e));

        Set<String> nextIds = dtoMap.keySet();

        nextIds.forEach(redisLiveStore::updateLastSeen);

        Set<String> staticPrevIds = redisLiveStore.getStaticKeys();

        long now = Instant.now().getEpochSecond();
        Set<String> endIds = new HashSet<>();
        for (String channelId : staticPrevIds) {
            if(nextIds.contains(channelId)) continue;;

            Long lastSeen = redisLiveStore.getLastSeen(channelId);
            if(lastSeen == null) continue;

            if (now - lastSeen > END_THRESHOLD_SECONDS) {
                endIds.add(channelId);
            }
        }

        liveHandlerService.handleEnd(endIds);
        liveHandlerService.handleStart(nextIds, dtoMap);
        liveHandlerService.handleTopic(nextIds, dtoMap);

    }

}
