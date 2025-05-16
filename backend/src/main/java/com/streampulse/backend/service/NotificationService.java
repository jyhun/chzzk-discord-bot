package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.dto.NotificationRequestDTO;
import com.streampulse.backend.entity.*;
import com.streampulse.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final RestTemplate restTemplate;

    @Value("${processor.url}")
    private String processorUrl;

    @LogExecution
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
    @Async
    public void requestStreamStartNotification(String channelId, String streamerName) {
        String url = processorUrl + "/api/stream-start";

        Map<String, String> payload = new HashMap<>();
        payload.put("streamerId", channelId);
        payload.put("streamerName", streamerName);


        try {
            restTemplate.postForEntity(url, payload, Void.class);
        } catch (Exception e) {
            log.warn("디스코드 알림 실패 – {}", e.getMessage());
        }

    }

    @LogExecution
    @Async
    public void requestStreamEndNotification(Streamer streamer, StreamSession streamSession) {
        String url = processorUrl + "/api/stream-end";

        String channelId = streamer.getChannelId();
        String streamerName = streamer.getNickname();
        int peakViewerCount = streamSession.getPeakViewerCount();
        int averageViewerCount = streamSession.getAverageViewerCount();

        LocalDateTime startedAt = streamSession.getStartedAt();
        LocalDateTime endedAt = streamSession.getEndedAt();

        Duration duration = Duration.between(startedAt, endedAt);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        String durationStr;
        if (hours > 0) {
            durationStr = String.format("%d시간 %d분", hours, minutes);
        } else {
            durationStr = String.format("%d분", minutes);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("streamerId", channelId);
        payload.put("streamerName", streamerName);
        payload.put("peakViewerCount", peakViewerCount);
        payload.put("averageViewerCount", averageViewerCount);
        payload.put("duration", durationStr);

        try {
            restTemplate.postForEntity(url, payload, Void.class);
        } catch (Exception e) {
            log.warn("디스코드 알림 실패 – {}", e.getMessage());
        }

    }

    @LogExecution
    @Async
    public void requestStreamTopicNotification(String streamerChannelId, String streamerName, String discordChannelId, List<String> matchedKeywords, LiveResponseDTO dto) {
        String url = processorUrl + "/api/stream-topic";

        Map<String, Object> payload = new HashMap<>();
        payload.put("streamerId", streamerChannelId);
        payload.put("streamerName", streamerName);
        payload.put("discordChannelId", discordChannelId);
        payload.put("keywords", matchedKeywords);
        payload.put("title", dto.getLiveTitle());
        payload.put("category", dto.getLiveCategoryValue());
        payload.put("tags", dto.getTags());

        try {
            restTemplate.postForEntity(url, payload, Void.class);
        } catch (Exception e) {
            log.warn("디스코드 알림 실패 – {}", e.getMessage());
        }

    }

    @LogExecution
    @Async
    public void requestStreamHotNotification(StreamEvent streamEvent) {
        String url = processorUrl + "/api/stream-hot";

        // 한국 시간대로 시간대 설정
        ZoneId seoulZoneId = ZoneId.of("Asia/Seoul");

        // 데이터 추출
        StreamMetrics metrics = streamEvent.getStreamMetrics();
        String channelId = metrics.getStreamSession().getStreamer().getChannelId();
        String streamerUrl = "https://chzzk.naver.com/live/" + channelId;
        String nickname = metrics.getStreamSession().getStreamer().getNickname();
        String title = metrics.getTitle();
        String category = metrics.getCategory();
        int viewerCount = metrics.getViewerCount();
        int averageViewerCount = metrics.getStreamSession().getStreamer().getAverageViewerCount();
        String summary = streamEvent.getSummary();

        // 감지 시각 (한국 시간)
        ZonedDateTime detectedAtSeoul = streamEvent.getCreatedAt().atZone(seoulZoneId);
        String formattedDate = detectedAtSeoul.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm"));

        LocalDateTime startedAt = metrics.getStreamSession().getStartedAt();
        LocalDateTime endedAt = streamEvent.getCreatedAt();

        Duration duration = Duration.between(startedAt, endedAt);
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;

        String durationStr;
        if (hours > 0) {
            durationStr = String.format("%d시간 %d분", hours, minutes);
        } else {
            durationStr = String.format("%d분", minutes);
        }

        // 평균 대비 증가율 계산 (0 division 방지)
        float viewerIncreaseRate = 0;
        if (averageViewerCount > 0) {
            viewerIncreaseRate = ((float) viewerCount / averageViewerCount) * 100;
        }

        // Payload 구성
        Map<String, Object> payload = new HashMap<>();
        payload.put("streamerId", channelId);
        payload.put("streamerUrl", streamerUrl);
        payload.put("nickname", nickname);
        payload.put("title", title);
        payload.put("category", category);
        payload.put("viewerCount", viewerCount);
        payload.put("summary", summary);
        payload.put("formattedDate", formattedDate);
        payload.put("broadcastElapsedTime", durationStr);
        payload.put("viewerIncreaseRate", viewerIncreaseRate);

        try {
            restTemplate.postForEntity(url, payload, Void.class);
        } catch (Exception e) {
            log.warn("디스코드 알림 실패 – {}", e.getMessage());
        }

    }


}