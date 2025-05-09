package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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

    @LogExecution
    public void updateLiveStatus(Streamer streamer, boolean isLive) {
        streamer.updateLive(isLive);
        streamerRepository.save(streamer);
    }

    @LogExecution
    public void markOfflineStreamers(Set<String> liveStreamerChannelIds) {
        List<Streamer> streamerList = streamerRepository.findByLiveIsTrue();
        for (Streamer streamer : streamerList) {
            if (!liveStreamerChannelIds.contains(streamer.getChannelId())) {
                updateLiveStatus(streamer, false);
                StreamSession streamSession = streamSessionService.handleStreamEnd(streamer);
                if (subscriptionService.hasSubscribersFor(EventType.END, streamer.getChannelId()) && streamer.getAverageViewerCount() >= 30) {
                    notificationService.requestStreamEndNotification(streamer, streamSession);
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public Optional<Streamer> findByChannelId(String channelId) {
        return streamerRepository.findByChannelId(channelId);
    }
}
