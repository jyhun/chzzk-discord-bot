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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
                log.info("[handleStart] 시작 후 {}분 경과 → SKIP, channelId={}", delay.toMinutes(), channelId);
                continue;
            }

            boolean isNew = streamSessionService.findByStreamerAndStartedAt(streamer, startedAt).isEmpty();
            if (!isNew) {
                log.info("[handleStart] 세션 중복 감지 → SKIP, channelId={}", channelId);
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
            log.info("[handleStart] channelId = {}", channelId);
        }
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public void handleEnd(Set<String> endIds) {
        if (endIds.isEmpty()) return;

        for (String channelId : endIds) {
            Optional<Streamer> optStreamer = streamerService.findByChannelId(channelId);
            if (optStreamer.isEmpty()) continue;
            Streamer streamer = optStreamer.get();

            Optional<StreamSession> sessionOpt = streamSessionService.getActiveSession(streamer);
            if (sessionOpt.isEmpty()) {
                log.info("[handleEnd] 활성 세션 없음 → SKIP, channelId={}", channelId);
                continue;
            }
            StreamSession session = sessionOpt.get();

            streamerService.updateLiveStatus(streamer, false);

            StreamSession endedSession = streamSessionService.handleStreamEnd(streamer, session.getStartedAt());
            if (endedSession == null) {
                log.warn("[handleEnd] 세션 종료 실패 → SKIP, channelId={}", channelId);
                continue;
            }

            if (streamer.getAverageViewerCount() >= 30
                    && subscriptionService.hasSubscribersFor(EventType.END, channelId)) {
                notificationService.requestStreamEndNotification(streamer, session);
            }

            log.info("[handleEnd] channelId = {}", channelId);
            redisLiveStore.deleteSnapshot(channelId);
            redisLiveStore.deleteLiveKey(channelId);

        }
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public void handleTopic(Set<String> nextIds, Map<String, LiveResponseDTO> dtoMap) {
        if (nextIds.isEmpty()) return;

        for (String channelId : nextIds) {
            LiveResponseDTO dto = dtoMap.get(channelId);
            if (dto == null) continue;

            Streamer streamer = streamerService.getOrCreateStreamer(dto);
            Optional<StreamSession> sessionOpt = streamSessionService.getActiveSession(streamer);
            if (sessionOpt.isEmpty()) {
                log.info("[handleTopic] 활성 세션 없음 → SKIP, channelId={}", channelId);
                continue;
            }
            StreamSession session = sessionOpt.get();

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
