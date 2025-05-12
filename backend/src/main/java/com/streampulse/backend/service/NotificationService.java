package com.streampulse.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.dto.NotificationRequestDTO;
import com.streampulse.backend.entity.*;
import com.streampulse.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String STREAM_START = "stream:notification:start";
    private static final String STREAM_END = "stream:notification:end";
    private static final String STREAM_TOPIC = "stream:notification:topic";
    private static final String STREAM_HOT = "stream:notification:hot";

    @LogExecution
    public void saveNotification(NotificationRequestDTO dto) {
        notificationRepository.save(
                Notification.builder()
                        .eventType(dto.getEventType())
                        .receiverId(dto.getReceiverId())
                        .success(dto.isSuccess())
                        .message(dto.getMessage())
                        .sentAt(dto.isSuccess() ? LocalDateTime.now() : null)
                        .build()
        );
    }

    @LogExecution
    public void requestStreamStartNotification(String channelId, String streamerName) {
        Map<String, String> payload = Map.of(
                "streamerId", channelId,
                "streamerName", streamerName
        );
        pushToStream(STREAM_START, payload);
    }

    @LogExecution
    public void requestStreamEndNotification(Streamer streamer, StreamSession streamSession) {
        String durationStr = getDurationStr(streamSession.getStartedAt(), streamSession.getEndedAt());
        Map<String, Object> payload = Map.of(
                "streamerId", streamer.getChannelId(),
                "streamerName", streamer.getNickname(),
                "peakViewerCount", streamSession.getPeakViewerCount(),
                "averageViewerCount", streamSession.getAverageViewerCount(),
                "duration", durationStr
        );
        pushToStream(STREAM_END, payload);
    }

    @LogExecution
    public void requestStreamTopicNotification(String streamerChannelId, String streamerName, String discordChannelId, List<String> matchedKeywords, LiveResponseDTO dto) {
        Map<String, Object> payload = Map.of(
                "streamerId", streamerChannelId,
                "streamerName", streamerName,
                "discordChannelId", discordChannelId,
                "keywords", matchedKeywords,
                "title", dto.getLiveTitle(),
                "category", dto.getLiveCategoryValue(),
                "tags", dto.getTags()
        );
        pushToStream(STREAM_TOPIC, payload);
    }

    @LogExecution
    public void requestStreamHotNotification(StreamEvent streamEvent) {
        StreamMetrics metrics = streamEvent.getStreamMetrics();
        StreamSession session = metrics.getStreamSession();
        Streamer streamer = session.getStreamer();

        String durationStr = getDurationStr(session.getStartedAt(), streamEvent.getCreatedAt());
        ZonedDateTime detectedAtSeoul = streamEvent.getCreatedAt().atZone(ZoneId.of("Asia/Seoul"));
        String formattedDate = detectedAtSeoul.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm"));

        float rate = 0;
        if (streamer.getAverageViewerCount() > 0) {
            rate = ((float) metrics.getViewerCount() / streamer.getAverageViewerCount()) * 100;
        }

        Map<String, Object> payload = Map.of(
                "streamerId", streamer.getChannelId(),
                "streamerUrl", "https://chzzk.naver.com/live/" + streamer.getChannelId(),
                "nickname", streamer.getNickname(),
                "title", metrics.getTitle(),
                "category", metrics.getCategory(),
                "viewerCount", metrics.getViewerCount(),
                "summary", streamEvent.getSummary(),
                "formattedDate", formattedDate,
                "broadcastElapsedTime", durationStr,
                "viewerIncreaseRate", rate
        );
        pushToStream(STREAM_HOT, payload);
    }

    private String getDurationStr(LocalDateTime start, LocalDateTime end) {
        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return hours > 0 ? String.format("%d시간 %d분", hours, minutes) : String.format("%d분", minutes);
    }

    private void pushToStream(String streamKey, Map<String, ?> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForStream().add(streamKey, Map.of("payload", json));
        } catch (JsonProcessingException e) {
            log.error("[RedisStream] 메시지 직렬화 실패", e);
        }
    }
}
