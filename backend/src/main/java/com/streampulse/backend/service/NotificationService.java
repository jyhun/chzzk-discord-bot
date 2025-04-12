package com.streampulse.backend.service;

import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.Notification;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.infra.DiscordNotifier;
import com.streampulse.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DiscordNotifier discordNotifier;

    public void notifyStreamEvent(StreamEvent streamEvent) {
        String message = generateMessage(streamEvent);
        boolean success;
        String errorMessage = null;

        try {
            success = discordNotifier.sendMessage(message);
            if (success) {
                streamEvent.updateNotified(true);
            } else {
                errorMessage = "디스코드 응답 실패";
            }
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
        }

        Notification notification = Notification.builder()
                .streamEvent(streamEvent)
                .sentAt(LocalDateTime.now())
                .message(message)
                .success(success)
                .errorMessage(errorMessage)
                .build();

        notificationRepository.save(notification);
    }

    private String generateMessage(StreamEvent streamEvent) {
        StreamMetrics metrics = streamEvent.getMetrics();
        String channelId = metrics.getSession().getStreamer().getChannelId();
        String streamerUrl = "https://chzzk.naver.com/live/" + channelId;
        String nickname = metrics.getSession().getStreamer().getNickname();
        String title = metrics.getTitle();
        String category = metrics.getCategory();
        int viewerCount = metrics.getViewerCount();
        String summary = streamEvent.getSummary();
        LocalDateTime detectedAt = streamEvent.getDetectedAt();
        String formattedDate = detectedAt.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 HH:mm"));

        return String.format("""
                🎉 **하이라이트 순간 포착!** 🎉
                
                🔗 [방송 바로 가기](%s)
                🧑‍💻 방송자: **%s**
                🏷️ 방송제목: **%s**
                🎮 카테고리: **%s**
                👥 시청자 수: **%,d명**
                
                🔥 **시청자들이 이렇게 반응했어요!**
                > %s
                
                ⏰ 감지 시각: %s
                """, streamerUrl, nickname, title, category, viewerCount, summary, formattedDate);


    }

}