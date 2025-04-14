package com.streampulse.backend.service;

import com.streampulse.backend.dto.SubscriptionRequestDTO;
import com.streampulse.backend.dto.SubscriptionResponseDTO;
import com.streampulse.backend.entity.DiscordChannel;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.entity.Subscription;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.repository.DiscordChannelRepository;
import com.streampulse.backend.repository.StreamerRepository;
import com.streampulse.backend.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final StreamerRepository streamerRepository;
    private final DiscordChannelRepository discordChannelRepository;

    public void createSubscription(SubscriptionRequestDTO dto) {
        if (dto.getEventType() == null) {
            throw new IllegalArgumentException("eventType 은 필수 값입니다.");
        }

        DiscordChannel discordChannel = discordChannelRepository.findByDiscordChannelId(dto.getDiscordChannelId())
                .orElseGet(() -> discordChannelRepository.save(
                        DiscordChannel.builder()
                                .discordGuildId(dto.getDiscordGuildId())
                                .discordChannelId(dto.getDiscordChannelId())
                                .active(true)
                                .build()));

        Streamer streamer = null;
        if (dto.getStreamerId() != null) {
            streamer = streamerRepository.findByChannelId(dto.getStreamerId())
                    .orElseThrow(() -> new IllegalArgumentException("방송자를 찾을 수 없습니다."));
        }

        boolean exists = streamer == null
                ? subscriptionRepository.existsGlobalSubscription(dto.getDiscordChannelId(), dto.getEventType())
                : subscriptionRepository.existsStreamerSubscription(dto.getDiscordChannelId(), streamer.getChannelId(), dto.getEventType());

        if (exists) {
            log.info("이미 구독 중입니다. discordChannelId={}, streamerId={}", dto.getDiscordChannelId(), dto.getStreamerId());
            throw new IllegalStateException("이미 구독 중인 대상입니다.");
        }

        Subscription subscription = Subscription.builder()
                .discordChannel(discordChannel)
                .streamer(streamer)
                .eventType(dto.getEventType())
                .keyword(dto.getKeyword())
                .active(true)
                .build();

        subscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> getSubscriptions(String streamerId, EventType eventType) {
        log.info("streamerId: {}, eventType: {}", streamerId, eventType);

        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.addAll(subscriptionRepository.findStreamerSubscriptionsByEventType(streamerId, eventType));
        subscriptions.addAll(subscriptionRepository.findGlobalSubscriptionsByEventType(eventType));

        return subscriptions.stream()
                .map(sub -> SubscriptionResponseDTO.builder()
                        .discordGuildId(sub.getDiscordChannel().getDiscordGuildId())
                        .discordChannelId(sub.getDiscordChannel().getDiscordChannelId())
                        .build())
                .collect(Collectors.toList());
    }

    public void deactivateSubscription(SubscriptionRequestDTO dto) {
        List<Subscription> subscriptions;

        if (dto.getEventType() == null) {
            // ✅ 전체 구독 해제
            subscriptions = subscriptionRepository.findActiveSubscriptionsByChannelId(dto.getDiscordChannelId());
        } else if (dto.getStreamerId() == null) {
            // ✅ 이벤트 전체 구독 해제
            subscriptions = subscriptionRepository.findActiveSubscriptionsByChannelIdAndEventType(
                    dto.getDiscordChannelId(), dto.getEventType());
        } else {
            // ✅ 이벤트 + 특정 스트리머 구독 해제
            subscriptions = subscriptionRepository.findActiveSubscriptionsByChannelIdAndStreamerIdAndEventType(
                    dto.getDiscordChannelId(), dto.getStreamerId(), dto.getEventType());
        }

        if (subscriptions.isEmpty()) {
            throw new IllegalArgumentException("구독이 존재하지 않습니다.");
        }

        subscriptions.forEach(Subscription::deactivate);
    }

}
