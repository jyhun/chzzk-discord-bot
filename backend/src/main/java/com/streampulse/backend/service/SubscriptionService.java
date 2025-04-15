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

    // 구독 생성
    public void createSubscription(SubscriptionRequestDTO dto) {
        log.info("dto:{}", dto.toString());
        // 디스코드 채널 없으면 저장
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

        // 중복 검사
        boolean exists = (streamer == null) ?
                subscriptionRepository.existsActiveGlobalSubscription(dto.getDiscordChannelId(), dto.getEventType()) :
                subscriptionRepository.existsActiveStreamerSubscription(dto.getDiscordChannelId(), streamer.getChannelId(), dto.getEventType());

        if (exists) {
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

    // (내부) 방송자 기준 구독자 목록
    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> getSubscriptions(String streamerId, EventType eventType) {
        List<Subscription> subscriptions = new ArrayList<>();
        subscriptions.addAll(subscriptionRepository.findByActiveStreamerAndEvent(streamerId, eventType));
        subscriptions.addAll(subscriptionRepository.findByActiveGlobalAndEvent(eventType));

        return subscriptions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // (디스코드 봇용) 사용자 구독 목록
    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> getMySubscriptions(String discordChannelId, String streamerId, EventType eventType) {
        List<Subscription> subscriptions = new ArrayList<>();

        if (streamerId != null && eventType != null) {
            subscriptions.addAll(subscriptionRepository.findByActiveChannelAndStreamerAndEvent(discordChannelId, streamerId, eventType));
        } else if (eventType != null) {
            subscriptions.addAll(subscriptionRepository.findByActiveChannelAndEvent(discordChannelId, eventType));
        } else {
            subscriptions.addAll(subscriptionRepository.findByActiveChannel(discordChannelId));
        }

        return subscriptions.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // 구독 해제
    public void deactivateSubscription(SubscriptionRequestDTO dto) {
        List<Subscription> subscriptions;

        if (dto.getEventType() == null) {
            subscriptions = subscriptionRepository.findActiveByChannel(dto.getDiscordChannelId());
        } else if (dto.getStreamerId() == null) {
            subscriptions = subscriptionRepository.findByActiveChannelAndGlobalAndEvent(
                    dto.getDiscordChannelId(), dto.getEventType());
        } else {
            subscriptions = subscriptionRepository.findByActiveChannelAndStreamerAndEventForDeactivate(
                    dto.getDiscordChannelId(), dto.getStreamerId(), dto.getEventType());
        }

        if (subscriptions.isEmpty()) {
            throw new IllegalArgumentException("구독이 존재하지 않습니다.");
        }

        subscriptions.forEach(Subscription::deactivate);
    }

    // 공통 DTO 변환
    private SubscriptionResponseDTO toResponseDTO(Subscription subscription) {
        return SubscriptionResponseDTO.builder()
                .discordGuildId(subscription.getDiscordChannel().getDiscordGuildId())
                .discordChannelId(subscription.getDiscordChannel().getDiscordChannelId())
                .eventType(subscription.getEventType())
                .streamerId(subscription.getStreamer() != null ? subscription.getStreamer().getChannelId() : null)
                .build();
    }
}
