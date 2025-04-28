package com.streampulse.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.infra.RedisLiveStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LiveSyncService {

    private final ChzzkLiveService chzzkLiveService;
    private final StreamerService streamerService;
    private final StreamSessionService streamSessionService;
    private final StreamMetricsService streamMetricsService;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;
    private final RedisLiveStore redisLiveStore;
    private final ObjectMapper objectMapper;

    @LogExecution
    public void syncLiveBroadcasts() {
        // 현재 방송 목록 수집
        List<LiveResponseDTO> liveList = chzzkLiveService.collectLiveBroadcastersFromRedis();
        Map<String, LiveResponseDTO> dtoMap = liveList.stream()
                .collect(Collectors.toMap(LiveResponseDTO::getChannelId,
                        dto -> dto,
                        (existing, replacement) -> existing
                ));

        Set<String> nextIds = dtoMap.keySet();
        Set<String> currIds = redisLiveStore.getLiveStreamerIds();

        Set<String> startIds = new HashSet<>(nextIds);
        startIds.removeAll(currIds);

        Set<String> endIds = new HashSet<>(currIds);
        endIds.removeAll(nextIds);

        redisLiveStore.updateLiveSet(startIds, endIds);

        // START 처리
        for (String id : startIds) {
            LiveResponseDTO dto = dtoMap.get(id);
            Streamer streamer = streamerService.getOrCreateStreamer(dto);
            streamerService.updateLiveStatus(streamer, true);

            if (streamer.getAverageViewerCount() >= 30) {
                if (subscriptionService.hasSubscribersFor(EventType.START, id))
                    notificationService.requestStreamStartNotification(id, streamer.getNickname());
                if (subscriptionService.hasSubscribersFor(EventType.TOPIC, id))
                    subscriptionService.detectTopicEvent(dto);
            }
        }

        // 세션 캐싱
        Map<String, StreamSession> sessionMap = nextIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            LiveResponseDTO dto = dtoMap.get(id);
                            Streamer streamer = streamerService.getOrCreateStreamer(dto);
                            return streamSessionService.getOrCreateSession(streamer, dto);
                        }
                ));

        // CHANGE 감지 및 메트릭 저장
        for (String id : nextIds) {
            LiveResponseDTO dto = dtoMap.get(id);
            StreamSession session = sessionMap.get(id);
            Streamer s = streamerService.getOrCreateStreamer(dto);

            String currJson = serialize(dto);
            String prevJson = redisLiveStore.getSnapshot(session.getId());

            if (!currJson.equals(prevJson)) {
                redisLiveStore.saveSnapshot(session.getId(), currJson);
                if (subscriptionService.hasSubscribersFor(EventType.TOPIC, id)
                        && s.getAverageViewerCount() >= 30) {
                    subscriptionService.detectTopicEvent(dto);
                }
            }
            streamMetricsService.saveMetrics(session, dto, s.getAverageViewerCount());
        }

        // END 처리
        for (String id : endIds) {
            Streamer s = streamerService.findByChannelId(id);
            if (s != null) {
                streamerService.updateLiveStatus(s, false);
                StreamSession session = streamSessionService.handleStreamEnd(s);

                if (subscriptionService.hasSubscribersFor(EventType.END, id)
                        && s.getAverageViewerCount() >= 30) {
                    notificationService.requestStreamEndNotification(s, session);
                }
            }
        }

    }

    private String serialize(LiveResponseDTO dto) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", dto.getLiveTitle());
        map.put("category", dto.getLiveCategory());
        map.put("tags", dto.getTags() != null ? dto.getTags() : List.of());
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
