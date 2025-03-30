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

        try {
            discordNotifier.sendMessage(message);

            Notification notification = Notification.builder()
                    .highlight(highlight)
                    .sentAt(LocalDateTime.now())
                    .message(message)
                    .build();

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("알림 전송 실패", e);
        }
    }

    private String generateMessage(Highlight highlight) {
        return String.format("""
                **하이라이트 감지!**
                방송자: %s
                채팅 수: %d
                급등률: %.1f배
                시각: %s
                """, highlight.getSession().getStreamer().getNickname(), highlight.getChatCount(), highlight.getScore(), highlight.getDetectedAt());
    }

}
