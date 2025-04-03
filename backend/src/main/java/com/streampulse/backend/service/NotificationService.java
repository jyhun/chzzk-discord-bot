package com.streampulse.backend.service;

import com.streampulse.backend.entity.Highlight;
import com.streampulse.backend.entity.Notification;
import com.streampulse.backend.infra.DiscordNotifier;
import com.streampulse.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DiscordNotifier discordNotifier;

    public void notifyHighlight(Highlight highlight) {
        String message = generateMessage(highlight);
        boolean success;
        String errorMessage = null;

        try {
            success = discordNotifier.sendMessage(message);
            if (success) {
                highlight.updateNotified(true);
            } else {
                errorMessage = "디스코드 응답 실패";
            }
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            log.error("알림 전송 중 예외 발생", e);
        }
        Notification notification = Notification.builder()
                .highlight(highlight)
                .sentAt(LocalDateTime.now())
                .message(message)
                .success(success)
                .errorMessage(errorMessage)
                .build();

        notificationRepository.save(notification);
    }

    private String generateMessage(Highlight highlight) {
        String nickname = highlight.getMetrics()
                .getSession()
                .getStreamer()
                .getNickname();
        String title = highlight.getMetrics().getTitle();
        String category = highlight.getMetrics().getCategory();
        int viewerCount = highlight.getMetrics().getViewerCount();
        LocalDateTime detectedAt = highlight.getDetectedAt();

        return String.format("""
                **하이라이트 감지!**
                방송자: %s
                방송제목: %s
                카테고리: %s
                시청자 수: %,d명
                감지 시각: %s                
                """, nickname, title, category, viewerCount, detectedAt);
    }

}
