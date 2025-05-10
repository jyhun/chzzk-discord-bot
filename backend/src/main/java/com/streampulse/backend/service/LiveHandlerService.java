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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    public void handleStart(Set<String> startIds, Map<String, LiveResponseDTO> dtoMap) {
        if (startIds.isEmpty()) return;

        for (String channelId : startIds) {
            LiveResponseDTO dto = dtoMap.get(channelId);
            if (dto == null) continue;

            Streamer streamer = streamerService.getOrCreateStreamer(dto);
            StreamSession session = streamSessionService.getOrCreateSession(streamer, dto);

            redisLiveStore.saveSnapshot(channelId, serialize(dto));

            if (streamer.getAverageViewerCount() >= 30
                    && subscriptionService.hasSubscribersFor(EventType.START, channelId))
                notificationService.requestStreamStartNotification(channelId, streamer.getNickname());

            if (subscriptionService.hasSubscribersFor(EventType.TOPIC, channelId)) {
                subscriptionService.detectTopicEvent(dto);
            }
        }
    }

    @LogExecution
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public void handleEnd(Set<String> endIds) {
        if (endIds.isEmpty()) return;

        for (String channelId : endIds) {
            String json = redisLiveStore.getSnapshot(channelId);
            if (json == null) continue;

            LiveResponseDTO dto;
            try {
                dto = objectMapper.readValue(json, LiveResponseDTO.class);
            } catch (JsonProcessingException e) {
                log.warn("snapshot parse error, channelId={}", channelId, e);
                continue;
            }

            Optional<Streamer> optStreamer = streamerService.findByChannelId(channelId);
            if (optStreamer.isEmpty()) continue;

            Streamer streamer = optStreamer.get();
            streamerService.updateLiveStatus(streamer, false);

            LocalDateTime startedAt = LocalDateTime.parse(dto.getOpenDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            StreamSession session = streamSessionService.handleStreamEnd(streamer, startedAt);
            if (session == null) {
                log.warn("session null, sessionId = {}", streamer.getChannelId());
                continue;
            }

            if (streamer.getAverageViewerCount() >= 30
                    && subscriptionService.hasSubscribersFor(EventType.END, channelId)) {
                notificationService.requestStreamEndNotification(streamer, session);
            }

            redisLiveStore.deleteSnapshot(channelId);
        }
    }

    @LogExecution
    public void handleTopic(Set<String> nextIds, Map<String, LiveResponseDTO> dtoMap) {
        if (nextIds.isEmpty()) return;

        for (String channelId : nextIds) {
            LiveResponseDTO dto = dtoMap.get(channelId);
            if (dto == null) continue;

            Streamer streamer = streamerService.getOrCreateStreamer(dto);
            StreamSession session = streamSessionService.getOrCreateSession(streamer, dto);

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
