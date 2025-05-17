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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String PREFIX = "stream:notification:";
    private static final String STREAM_START = PREFIX + "start";
    private static final String STREAM_END = PREFIX + "end";
    private static final String STREAM_TOPIC = PREFIX + "topic";
    private static final String STREAM_HOT = PREFIX + "hot";

    @LogExecution
    @Transactional
    public void saveNotification(NotificationRequestDTO notificationRequestDTO) {
        Notification notification = Notification.builder()
                .eventType(notificationRequestDTO.getEventType())
                .receiverId(notificationRequestDTO.getReceiverId())
                .success(notificationRequestDTO.isSuccess())
                .message(notificationRequestDTO.getMessage())
                .sentAt(notificationRequestDTO.isSuccess() ? LocalDateTime.now() : null)
                .build();

        notificationRepository.save(notification);
    }

    @LogExecution
    public void requestStreamStartNotification(String channelId, String streamerName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("streamerId", channelId);
        payload.put("streamerName", streamerName);
        pushToStream(STREAM_START, payload);
    }

    @LogExecution
    public void requestStreamEndNotification(Streamer streamer, StreamSession streamSession) {
        String durationStr = getDurationStr(streamSession.getStartedAt(), streamSession.getEndedAt());

        Map<String, Object> payload = new HashMap<>();
        payload.put("streamerId", streamer.getChannelId());
        payload.put("streamerName", streamer.getNickname());
        payload.put("peakViewerCount", streamSession.getPeakViewerCount());
        payload.put("averageViewerCount", streamSession.getAverageViewerCount());
        payload.put("duration", durationStr);

        pushToStream(STREAM_END, payload);
    }

    @LogExecution
    public void requestStreamTopicNotification(String streamerChannelId, String streamerName, String discordChannelId, List<String> matchedKeywords, LiveResponseDTO dto) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("streamerId", streamerChannelId);
        payload.put("streamerName", streamerName);
        payload.put("discordChannelId", discordChannelId);
        payload.put("keywords", matchedKeywords);
        payload.put("title", dto.getLiveTitle());
        payload.put("category", dto.getLiveCategoryValue());
        payload.put("tags", dto.getTags());

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

        Map<String, Object> payload = new HashMap<>();
        payload.put("streamerId", streamer.getChannelId());
        payload.put("streamerUrl", "https://chzzk.naver.com/live/" + streamer.getChannelId());
        payload.put("nickname", streamer.getNickname());
        payload.put("title", metrics.getTitle());
        payload.put("category", metrics.getCategory());
        payload.put("viewerCount", metrics.getViewerCount());
        payload.put("summary", streamEvent.getSummary());
        payload.put("formattedDate", formattedDate);
        payload.put("broadcastElapsedTime", durationStr);
        payload.put("viewerIncreaseRate", rate);

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
            log.error("[RedisStream] 메시지 직렬화 실패 – key={}, 원인={}", streamKey, e.getMessage(), e);
        } catch (Exception e) {
            log.error("[RedisStream] 스트림 전송 실패 – key={}, 원인={}", streamKey, e.getMessage(), e);
        }
    }

}