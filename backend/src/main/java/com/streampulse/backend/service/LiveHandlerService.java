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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveHandlerService {

    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;
    private final StreamerService streamerService;
    private final StreamMetricsService streamMetricsService;
    private final ObjectMapper objectMapper;
    private final StreamSessionService streamSessionService;
    private final RedisLiveStore redisLiveStore;

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public void handleStart(Set<String> nextIds, Map<String, LiveResponseDTO> dtoMap) {
        if (nextIds.isEmpty()) return;

        for (String channelId : nextIds) {
            try {
                LiveResponseDTO dto = dtoMap.get(channelId);

                if (dto == null) continue;

                Streamer streamer = streamerService.getOrCreateStreamer(dto);

                LocalDateTime startedAt;
                try {
                    startedAt = LocalDateTime.parse(dto.getOpenDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException e) {
                    log.warn("[handleStart] startedAt 파싱 실패: {}, channelId={}", dto.getOpenDate(), channelId);
                    continue;
                }

                Duration delay = Duration.between(startedAt, LocalDateTime.now());
                if (delay.toMinutes() >= 5) {
                    redisLiveStore.setStaticKey(channelId);
                    continue;
                }

                List<StreamSession> sameStartSessions = streamSessionService.findByStreamerAndStartedAt(streamer, startedAt);
                if (!sameStartSessions.isEmpty()) {
                    continue;
                }

                streamSessionService.createSession(streamer, dto);
                redisLiveStore.saveSnapshot(channelId, serialize(dto));

                if (streamer.getAverageViewerCount() >= 30
                        && subscriptionService.hasSubscribersFor(EventType.START, channelId)) {
                    notificationService.requestStreamStartNotification(channelId, streamer.getNickname());
                }

                if (streamer.getAverageViewerCount() >= 30
                        && subscriptionService.hasSubscribersFor(EventType.TOPIC, channelId)) {
                    subscriptionService.detectTopicEvent(dto);
                }
                redisLiveStore.setStaticKey(channelId);
                log.info("[handleStart] channelId = {}", channelId);
            } catch (Exception e) {
                log.error("[handleStart] 예외 발생 - channelId = {}, error = {}", channelId, e.getMessage(), e);
            }
        }
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public void handleEnd(Set<String> endIds) {
        if (endIds.isEmpty()) return;

        for (String channelId : endIds) {
            try {
                Optional<Streamer> optStreamer = streamerService.findByChannelId(channelId);
                if (optStreamer.isEmpty()) continue;
                Streamer streamer = optStreamer.get();

                List<StreamSession> activeSessions = streamSessionService.getAllUnendedSessions(streamer);
                if(activeSessions.isEmpty()) continue;

                activeSessions.sort((a, b) -> b.getStartedAt().compareTo(a.getStartedAt()));

                boolean notified = false;
                for (StreamSession session : activeSessions) {
                    StreamSession endedSession = streamSessionService.handleStreamEnd(streamer, session.getStartedAt());
                    if(endedSession == null) {
                        log.warn("[handleEnd] 세션 종료 실패 → SKIP, sessionId={}, channelId={}", session.getId(), channelId);
                        continue;
                    }

                    if (!notified && streamer.getAverageViewerCount() >= 30 &&
                            subscriptionService.hasSubscribersFor(EventType.END, channelId)) {
                        notificationService.requestStreamEndNotification(streamer, session);
                        notified = true;
                    }

                    log.info("[handleEnd] 종료 처리 완료 - sessionId={}, channelId={}", session.getId(), channelId);
                }

                streamerService.updateLiveStatus(streamer, false);
                redisLiveStore.deleteSnapshot(channelId);
                redisLiveStore.deleteStaticKey(channelId);
            } catch (Exception e) {
                log.error("[handleEnd] 예외 발생 - channelId = {}, error = {}", channelId, e.getMessage(), e);

            }
        }
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public void handleTopic(Set<String> nextIds, Map<String, LiveResponseDTO> dtoMap) {
        if (nextIds.isEmpty()) return;

        for (String channelId : nextIds) {
            try {
                LiveResponseDTO dto = dtoMap.get(channelId);

                if (dto == null) continue;

                Streamer streamer = streamerService.getOrCreateStreamer(dto);
                List<StreamSession> activeSessions = streamSessionService.getAllUnendedSessions(streamer);
                if (activeSessions.isEmpty()) {
                    continue;
                }

                StreamSession session = activeSessions.stream()
                        .max(Comparator.comparing(StreamSession::getStartedAt))
                        .get();

                String currJson = serialize(dto);
                String prevJson = redisLiveStore.getSnapshot(channelId);

                if (!currJson.equals(prevJson)) {
                    redisLiveStore.saveSnapshot(channelId, currJson);
                    if (streamer.getAverageViewerCount() >= 30
                            && subscriptionService.hasSubscribersFor(EventType.TOPIC, channelId)) {
                        subscriptionService.detectTopicEvent(dto);
                    }
                }
                streamMetricsService.saveMetrics(session, dto, streamer.getAverageViewerCount());
            } catch (Exception e) {
                log.error("[handleTopic] 예외 발생 - channelId = {}, error = {}", channelId, e.getMessage(), e);
            }
        }
    }

    private String serialize(LiveResponseDTO dto) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "channelId", Optional.ofNullable(dto.getChannelId()).orElse(""),
                    "openDate", Optional.ofNullable(dto.getOpenDate()).orElse(""),
                    "title", Optional.ofNullable(dto.getLiveTitle()).orElse(""),
                    "category", Optional.ofNullable(dto.getLiveCategory()).orElse(""),
                    "tags", dto.getTags() != null ? dto.getTags() : List.of()
            ));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
