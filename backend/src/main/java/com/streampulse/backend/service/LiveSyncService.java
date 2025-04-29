package com.streampulse.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.infra.RedisLiveStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
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
        List<LiveResponseDTO> liveList = chzzkLiveService.collectLiveBroadcastersFromRedis();
        Map<String, LiveResponseDTO> dtoMap = liveList.stream()
                .collect(Collectors.toMap(LiveResponseDTO::getChannelId, dto -> dto, (e, r) -> e));

        Set<String> nextIds = dtoMap.keySet();
        Set<String> currIds = redisLiveStore.getLiveStreamerIds();

        Set<String> startIds = new HashSet<>(nextIds);
        startIds.removeAll(currIds);
        Set<String> endIds = new HashSet<>(currIds);
        endIds.removeAll(nextIds);

        redisLiveStore.updateLiveSet(startIds, endIds);

        handleStart(startIds, dtoMap);
        handleTopic(nextIds, dtoMap);
        handleEnd(endIds);
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStart(Set<String> startIds, Map<String, LiveResponseDTO> dtoMap) {
        if (startIds.isEmpty()) return;

        Map<String, Streamer> streamerMap = streamerService.findAllByChannelIdIn(startIds).stream()
                .collect(Collectors.toMap(Streamer::getChannelId, streamer -> streamer));

        List<Streamer> newStreamers = startIds.stream()
                .filter(id -> !streamerMap.containsKey(id))
                .map(id -> {
                    LiveResponseDTO dto = dtoMap.get(id);
                    return Streamer.builder()
                            .channelId(dto.getChannelId())
                            .nickname(dto.getChannelName())
                            .averageViewerCount(dto.getConcurrentUserCount())
                            .live(true)
                            .build();
                }).toList();

        if (!newStreamers.isEmpty()) {
            chunkedSaveAll(newStreamers, streamerService::saveAll, 500);
            newStreamers.forEach(s -> streamerMap.put(s.getChannelId(), s));
        }

        for (String id : startIds) {
            Streamer streamer = streamerMap.get(id);
            streamerService.updateLiveStatus(streamer, true);

            if (streamer.getAverageViewerCount() >= 30) {
                if (subscriptionService.hasSubscribersFor(EventType.START, id))
                    notificationService.requestStreamStartNotification(id, streamer.getNickname());
                if (subscriptionService.hasSubscribersFor(EventType.TOPIC, id))
                    subscriptionService.detectTopicEvent(dtoMap.get(id));
            }
        }

        List<StreamSession> newSessions = startIds.stream()
                .map(id -> {
                    Streamer streamer = streamerMap.get(id);
                    LiveResponseDTO dto = dtoMap.get(id);
                    return StreamSession.builder()
                            .streamer(streamer)
                            .title(dto.getLiveTitle())
                            .category(dto.getLiveCategoryValue())
                            .startedAt(LocalDateTime.parse(dto.getOpenDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                            .build();
                }).toList();

        if (!newSessions.isEmpty()) {
            chunkedSaveAll(newSessions, streamSessionService::saveAll, 500);
        }
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTopic(Set<String> nextIds, Map<String, LiveResponseDTO> dtoMap) {
        if(nextIds.isEmpty()) return;

        nextIds.forEach(id -> {
            LiveResponseDTO dto = dtoMap.get(id);
            Streamer streamer = streamerService.findByChannelId(id);
            StreamSession session = streamSessionService.getActiveSession(streamer);

            String currJson = serialize(dto);
            String prevJson = redisLiveStore.getSnapshot(session.getId());
            if(!currJson.equals(prevJson)) {
                redisLiveStore.saveSnapshot(session.getId(), currJson);
                if (subscriptionService.hasSubscribersFor(EventType.TOPIC, id)
                        && streamer.getAverageViewerCount() >= 30) {
                    subscriptionService.detectTopicEvent(dto);
                }
            }
            streamMetricsService.saveMetrics(session, dto, streamer.getAverageViewerCount());
        });
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEnd(Set<String> endIds) {
        if (endIds.isEmpty()) return;

        streamerService.markOffline(endIds);

        List<Streamer> endStreamers = streamerService.findAllByChannelIdIn(endIds);
        List<StreamSession> sessionsToEnd = streamSessionService.findAllByStreamerIn(endStreamers).stream()
                .filter(session -> session.getEndedAt() == null)
                .peek(session -> {
                    session.updateEndedAt();

                    List<StreamMetrics> metrics = streamMetricsService.findByStreamSessionId(session.getId());
                    int sessionAvg = (int) metrics.stream()
                            .mapToInt(StreamMetrics::getViewerCount)
                            .average().orElse(0.0);
                    session.updateAverageViewerCount(sessionAvg);

                    int sessionPeak = metrics.stream()
                            .mapToInt(StreamMetrics::getViewerCount)
                            .max().orElse(0);

                    session.updatePeakViewerCount(sessionPeak);

                    if(!metrics.isEmpty()) {
                        session.addTags(metrics.get(metrics.size() - 1).getTags());
                    }
                })
                .toList();

        if(!sessionsToEnd.isEmpty()) {
            chunkedSaveAll(sessionsToEnd, streamSessionService::saveAll, 500);
        }

        endStreamers.forEach(s -> {
            List<StreamSession> allSessions = streamSessionService.findByStreamerId(s.getId());
            int streamerAvg = (int) allSessions.stream()
                    .mapToInt(StreamSession::getAverageViewerCount)
                    .average().orElse(0.0);

            s.updateAverageViewerCount(streamerAvg);

        });

        chunkedSaveAll(endStreamers, streamerService::saveAll, 500);

        sessionsToEnd.forEach(session -> {
            Streamer streamer = session.getStreamer();
            String id = streamer.getChannelId();
            if (subscriptionService.hasSubscribersFor(EventType.END, id)
                    && streamer.getAverageViewerCount() >= 30) {
                notificationService.requestStreamEndNotification(streamer, session);
            }
        });
    }

    private String serialize(LiveResponseDTO dto) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "title", dto.getLiveTitle(),
                    "category", dto.getLiveCategory(),
                    "tags", dto.getTags() != null ? dto.getTags() : List.of()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> void chunkedSaveAll(List<T> list, Consumer<List<T>> saveFn, int chunkSize) {
        for (int i = 0; i < list.size(); i += chunkSize) {
            saveFn.accept(list.subList(i, Math.min(i + chunkSize, list.size())));
        }
    }
}
