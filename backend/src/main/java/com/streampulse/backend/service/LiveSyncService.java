package com.streampulse.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.dto.StreamMetricsInputDTO;
import com.streampulse.backend.dto.SubscriptionCheckDTO;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.infra.RedisLiveStore;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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

    @PersistenceContext
    private EntityManager entityManager;

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

        handleStart(startIds, dtoMap);
        handleEnd(endIds);
        handleTopic(nextIds, dtoMap);

        firstRun.set(false);
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStart(Set<String> startIds, Map<String, LiveResponseDTO> dtoMap) {
        if (startIds.isEmpty()) return;

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
            chunkedSaveAll(newStreamers, streamerService::saveAll, 500);
            newStreamers.forEach(streamer -> streamerMap.put(streamer.getChannelId(), streamer));
        }

        startIds.parallelStream().forEach(id -> {
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
            chunkedSaveAll(newSessions, streamSessionService::saveAll, 500);
        }
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleTopic(Set<String> nextIds, Map<String, LiveResponseDTO> dtoMap) {
        if (nextIds.isEmpty()) return;

        Map<String,Boolean> existsCache = new ConcurrentHashMap<>();
        Map<String, Streamer> streamerCache = new ConcurrentHashMap<>();
        Map<String, StreamSession> sessionCache = new ConcurrentHashMap<>();
        Map<String, String> jsonCache = new ConcurrentHashMap<>();
        SubscriptionCheckDTO topicCheck = subscriptionService.getSubscriptionCheck(EventType.TOPIC);

        List<StreamMetricsInputDTO> inputs = nextIds.parallelStream()
                .filter(id -> existsCache.computeIfAbsent(id,
                        streamSessionService::existsActiveSessionByChannelId))
                .map(id -> {
                    LiveResponseDTO dto = dtoMap.get(id);
                    Streamer streamer = streamerCache.computeIfAbsent(id, streamerService::findByChannelId);
                    if (streamer == null) return null;
                    StreamSession session = sessionCache.computeIfAbsent(id,
                            k -> streamSessionService.getActiveSession(streamer));

                    String currJson = jsonCache.computeIfAbsent(id, k-> serialize(dto));
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

        streamMetricsService.saveMetricsBulk(inputs);
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleEnd(Set<String> endIds) {
        if (endIds.isEmpty()) return;

        Map<Long, List<StreamMetrics>> metricsCache = new ConcurrentHashMap<>();
        Map<Long, List<StreamSession>> sessionCache = new ConcurrentHashMap<>();
        SubscriptionCheckDTO endCheck = subscriptionService.getSubscriptionCheck(EventType.END);

        streamerService.markOffline(endIds);

        List<Streamer> endStreamers = streamerService.findAllByChannelIdIn(endIds);
        List<StreamSession> sessionsToEnd = streamSessionService.findAllByStreamerIn(endStreamers).stream()
                .filter(session -> session.getEndedAt() == null)
                .peek(session -> {
                    session.updateEndedAt();

                    List<StreamMetrics> metrics = metricsCache.computeIfAbsent(
                            session.getId(), streamMetricsService::findByStreamSessionId);
                    int sessionAvg = (int) metrics.stream()
                            .mapToInt(StreamMetrics::getViewerCount)
                            .average().orElse(0.0);
                    session.updateAverageViewerCount(sessionAvg);

                    int sessionPeak = metrics.stream()
                            .mapToInt(StreamMetrics::getViewerCount)
                            .max().orElse(0);

                    session.updatePeakViewerCount(sessionPeak);

                    if (!metrics.isEmpty()) {
                        session.addTags(metrics.get(metrics.size() - 1).getTags());
                    }
                })
                .toList();

        if (!sessionsToEnd.isEmpty()) {
            chunkedSaveAll(sessionsToEnd, streamSessionService::saveAll, 500);
        }

        endStreamers.parallelStream().forEach(s -> {
            List<StreamSession> allSessions = sessionCache.computeIfAbsent(
                    s.getId(), streamSessionService::findByStreamerId);
            int streamerAvg = (int) allSessions.stream()
                    .mapToInt(StreamSession::getAverageViewerCount)
                    .average().orElse(0.0);

            s.updateAverageViewerCount(streamerAvg);

        });

        chunkedSaveAll(endStreamers, streamerService::saveAll, 500);

        sessionsToEnd.parallelStream().forEach(session -> {
            Streamer streamer = session.getStreamer();
            if (streamer == null) return;
            String id = streamer.getChannelId();
            if (endCheck.isSubscribed(id) && streamer.getAverageViewerCount() >= 30) {
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
        if (list == null || list.isEmpty()) return;
        for (int i = 0; i < list.size(); i += chunkSize) {
            saveFn.accept(list.subList(i, Math.min(i + chunkSize, list.size())));
            entityManager.flush();
            entityManager.clear();
        }
    }

    @PostConstruct
    public void init() {
        redisLiveStore.clearLiveSet();
    }
}
