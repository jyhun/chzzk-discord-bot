package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;


@Service
@Transactional
@RequiredArgsConstructor
public class StreamerService {

    private final StreamerRepository streamerRepository;
    private final StreamSessionService streamSessionService;
    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;

    public Streamer getOrCreateStreamer(LiveResponseDTO dto) {
        return streamerRepository.findByChannelId(dto.getChannelId()).orElseGet(
                () -> streamerRepository.save(
                        Streamer.builder()
                                .channelId(dto.getChannelId())
                                .nickname(dto.getChannelName())
                                .averageViewerCount(dto.getConcurrentUserCount())
                                .live(false)
                                .build()));
    }

    public void updateLiveStatus(Streamer streamer, boolean isLive) {
        streamer.updateLive(isLive);
        streamerRepository.save(streamer);
    }

    public void markOfflineStreamers(Set<String> liveStreamerChannelIds) {
        List<Streamer> streamerList = streamerRepository.findByLiveIsTrue();
        for (Streamer streamer : streamerList) {
            if (!liveStreamerChannelIds.contains(streamer.getChannelId())) {
                updateLiveStatus(streamer, false);
                if (subscriptionService.hasSubscribersFor(EventType.END, streamer.getChannelId()) && streamer.getAverageViewerCount() >= 10) {
                    notificationService.requestStreamStatusNotification(streamer.getChannelId(), EventType.END);
                }
                streamSessionService.handleStreamEnd(streamer);
            }
        }
    }
}
