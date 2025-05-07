package com.streampulse.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.dto.StreamMetricsCacheDTO;
import com.streampulse.backend.dto.StreamMetricsInputDTO;
import com.streampulse.backend.dto.SubscriptionCheckDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.entity.Tag;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.infra.RedisLiveStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class LiveSyncService {

    private final StreamerService streamerService;
    private final StreamSessionService streamSessionService;
    private final StreamMetricsService streamMetricsService;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;
    private final RedisLiveStore redisLiveStore;
    private final ChzzkLiveService chzzkLiveService;

    private static final int CHUNK_SIZE = 100;
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

        handleStart(startIds, dtoMap);
        handleEnd(endIds);
        handleTopic(nextIds, dtoMap);

        firstRun.set(false);
    }

    private void handleStart(Set<String> startIds, Map<String, LiveResponseDTO> dtoMap) {
        if (startIds.isEmpty()) return;

        try {
            Map<String, Streamer> streamerMap = streamerService.findAllByChannelIdIn(startIds).stream()
                    .collect(Collectors.toMap(Streamer::getChannelId, streamer -> streamer));

            SubscriptionCheckDTO startCheck = subscriptionService.getSubscriptionCheck(EventType.START);

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
                    })
                    .toList();

            if (!newStreamers.isEmpty()) {
                streamerService.saveStreamersWithUpsertChunks(newStreamers, CHUNK_SIZE);
                newStreamers.forEach(streamer -> streamerMap.put(streamer.getChannelId(), streamer));
            }

            startIds.forEach(id -> {
                Streamer streamer = streamerMap.get(id);
                streamerService.updateLiveStatus(streamer, true);

                if (!firstRun.get()
                        && streamer.getAverageViewerCount() >= 30
                        && startCheck.isSubscribed(id))
                    notificationService.requestStreamStartNotification(id, streamer.getNickname());
            });

            List<StreamSession> newSessions = startIds.stream()
                    .filter(streamerMap::containsKey)
                    .map(id -> {
                        Streamer streamer = streamerMap.get(id);
                        LiveResponseDTO dto = dtoMap.get(id);
                        return StreamSession.builder()
                                .streamer(streamer)
                                .title(dto.getLiveTitle())
                                .category(dto.getLiveCategoryValue())
                                .startedAt(LocalDateTime.parse(dto.getOpenDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                                .build();
                    })
                    .toList();

            if (!newSessions.isEmpty()) {
                streamSessionService.saveSessionsInChunks(newSessions, CHUNK_SIZE);
            }
        } catch (Exception e) {
            log.error("handleStart 오류: {}", e.getMessage());
        }
    }

    private void handleTopic(Set<String> nextIds, Map<String, LiveResponseDTO> dtoMap) {
        if (nextIds.isEmpty()) return;

        try {
            Map<String, Boolean> existsCache = new ConcurrentHashMap<>();
            Map<String, Streamer> streamerCache = new ConcurrentHashMap<>();
            Map<String, StreamSession> sessionCache = new ConcurrentHashMap<>();
            Map<String, String> jsonCache = new ConcurrentHashMap<>();
            SubscriptionCheckDTO topicCheck = subscriptionService.getSubscriptionCheck(EventType.TOPIC);

            List<StreamMetricsInputDTO> inputs = nextIds.stream()
                    .filter(id -> existsCache.computeIfAbsent(id,
                            streamSessionService::existsActiveSessionByChannelId))
                    .map(id -> {
                        LiveResponseDTO dto = dtoMap.get(id);
                        Streamer streamer = streamerCache.computeIfAbsent(id, streamerService::findByChannelId);
                        if (streamer == null) return null;
                        StreamSession session = sessionCache.computeIfAbsent(id,
                                k -> streamSessionService.getActiveSession(streamer));
                        if (session == null) return null;
                        String currJson = jsonCache.computeIfAbsent(id, k -> serialize(dto));
                        String prevJson = redisLiveStore.getSnapshot(session.getId());
                        if (!currJson.equals(prevJson)) {
                            redisLiveStore.saveSnapshot(session.getId(), currJson);
                            if (!firstRun.get()
                                    && topicCheck.isSubscribed(id)
                                    && streamer.getAverageViewerCount() >= 30) {
                                subscriptionService.detectTopicEvent(dto);
                            }
                        }
                        return StreamMetricsInputDTO.builder()
                                .session(session)
                                .dto(dto)
                                .averageViewerCount(streamer.getAverageViewerCount())
                                .build();
                    })
                    .filter(Objects::nonNull)
                    .toList();

            streamMetricsService.saveMetricsInChunks(inputs, CHUNK_SIZE);
        } catch (Exception e) {
            log.error("handleTopic 오류: {}", e.getMessage());
        }
    }

    private void handleEnd(Set<String> endIds) {
        if (endIds.isEmpty()) return;

        try {
            SubscriptionCheckDTO endCheck = subscriptionService.getSubscriptionCheck(EventType.END);

            streamerService.markOffline(endIds);

            List<Streamer> endStreamers = streamerService.findAllByChannelIdIn(endIds);

            Map<Long, List<StreamMetricsCacheDTO>> metricsCache = new HashMap<>();

            List<StreamSession> sessionsToEnd = streamSessionService.findAllByStreamerIn(endStreamers).stream()
                    .filter(session -> session.getEndedAt() == null)
                    .toList();

            if (!sessionsToEnd.isEmpty()) {
                streamSessionService.bulkEndSessions(sessionsToEnd.stream().map(StreamSession::getId).toList());
            }

            List<StreamSession> endedSessions = streamSessionService.findByIds(
                    sessionsToEnd.stream().map(StreamSession::getId).toList()
            );

            for (StreamSession session : endedSessions) {
                List<StreamMetricsCacheDTO> metrics = streamMetricsService.findByStreamSessionId(session.getId());
                metricsCache.put(session.getId(), metrics);

                if (!metrics.isEmpty()) {
                    List<Tag> tagList = new ArrayList<>();
                    List<String> tags = metrics.get(metrics.size() - 1).getTags();
                    for (String tag : tags) {
                        tagList.add(Tag.builder().streamSession(session).value(tag).build());
                    }
                    session.addTags(tagList);
                }
            }

            if (!endedSessions.isEmpty()) {
                streamSessionService.saveSessionsInChunks(endedSessions, CHUNK_SIZE);
            }

            endedSessions.forEach(session -> {
                Streamer streamer = session.getStreamer();
                if (streamer == null) return;
                String id = streamer.getChannelId();
                List<StreamMetricsCacheDTO> metrics = metricsCache.getOrDefault(session.getId(), Collections.emptyList());
                IntSummaryStatistics stats = metrics.stream()
                        .mapToInt(StreamMetricsCacheDTO::getViewerCount)
                        .summaryStatistics();
                int avgViewer = (int) Math.round(stats.getAverage());
                int peakViewer = stats.getMax();
                if (endCheck.isSubscribed(id) && streamer.getAverageViewerCount() >= 30) {
                    notificationService.requestStreamEndNotification(streamer, session, avgViewer, peakViewer);
                }
            });
        } catch (Exception e) {
            log.error("handleEnd 오류: {}", e.getMessage());
        }
    }

    private String serialize(LiveResponseDTO dto) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "title", Optional.ofNullable(dto.getLiveTitle()).orElse(""),
                    "category", Optional.ofNullable(dto.getLiveCategory()).orElse(""),
                    "tags", dto.getTags() != null ? dto.getTags() : List.of()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    public void init() {
        redisLiveStore.clearLiveSet();
    }
}
