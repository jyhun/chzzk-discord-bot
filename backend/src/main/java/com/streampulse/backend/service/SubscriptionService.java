package com.streampulse.backend.service;

import com.streampulse.backend.dto.LiveResponseDTO;
import com.streampulse.backend.dto.SubscriptionRequestDTO;
import com.streampulse.backend.dto.SubscriptionResponseDTO;
import com.streampulse.backend.entity.DiscordChannel;
import com.streampulse.backend.entity.Keyword;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final StreamerRepository streamerRepository;
    private final DiscordChannelRepository discordChannelRepository;
    private final NotificationService notificationService;

    // 구독 생성
    public void createSubscription(SubscriptionRequestDTO dto) {
        // 1. 디스코드 채널 조회 또는 생성
        DiscordChannel discordChannel = discordChannelRepository.findByDiscordChannelId(dto.getDiscordChannelId())
                .orElseGet(() -> discordChannelRepository.save(
                        DiscordChannel.builder()
                                .discordGuildId(dto.getDiscordGuildId())
                                .discordChannelId(dto.getDiscordChannelId())
                                .active(true)
                                .build()));

        // 2. 스트리머 조회 (있을 경우)
        Streamer streamer = null;
        if (dto.getStreamerId() != null) {
            streamer = streamerRepository.findByChannelId(dto.getStreamerId())
                    .orElseThrow(() -> new IllegalArgumentException("방송자를 찾을 수 없습니다."));
        }

        // 3. CHANGE 이벤트일 경우 키워드 필수
        if (dto.getEventType() == EventType.CHANGE &&
                (dto.getKeyword() == null || dto.getKeyword().trim().isEmpty())) {
            throw new IllegalArgumentException("CHANGE 이벤트는 키워드가 필수입니다.");
        }

        // 4. 기존 구독 존재 여부 확인
        Subscription subscription = subscriptionRepository.findActiveByChannelAndStreamerAndEventType(
                discordChannel.getDiscordChannelId(),
                streamer != null ? streamer.getChannelId() : null,
                dto.getEventType()
        ).orElse(null);

        // 5. 기존 구독이 있으면 중복 키워드 확인 후 추가
        if (subscription != null) {
            if (dto.getEventType() == EventType.CHANGE) {
                String keyword = dto.getKeyword().trim().toLowerCase();

                boolean exists = subscription.getKeywords().stream()
                        .anyMatch(k -> k.getValue().equalsIgnoreCase(keyword));

                if (exists) {
                    throw new IllegalStateException("이미 등록된 키워드입니다.");
                }

                Keyword newKeyword = Keyword.builder()
                        .subscription(subscription)
                        .value(keyword)
                        .build();
                subscription.getKeywords().add(newKeyword);
            }

        } else {
            // 6. 구독이 없다면 새로 생성
            subscription = Subscription.builder()
                    .discordChannel(discordChannel)
                    .streamer(streamer)
                    .eventType(dto.getEventType())
                    .active(true)
                    .build();

            // 키워드 추가 (CHANGE 이벤트인 경우만)
            if (dto.getEventType() == EventType.CHANGE) {
                Keyword keyword = Keyword.builder()
                        .subscription(subscription)
                        .value(dto.getKeyword().trim().toLowerCase())
                        .build();
                subscription.getKeywords().add(keyword);
            }

            subscriptionRepository.save(subscription);
        }
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
                .keywords(subscription.getKeywords().stream().map(Keyword::getValue).collect(Collectors.toList()))
                .build();
    }

    public void detectChangeEvent(LiveResponseDTO dto) {
        String channelId = dto.getChannelId();

        // 1. 특정 방송자 구독
        List<Subscription> perStreamerSubs = subscriptionRepository
                .findByStreamer_ChannelIdAndEventTypeAndActiveTrue(channelId, EventType.CHANGE);

        // 2. 전체 방송자 구독
        List<Subscription> globalSubs = subscriptionRepository
                .findByStreamerIsNullAndEventTypeAndActiveTrue(EventType.CHANGE);

        // 3. 통합 후 채널별 키워드 감지 정리
        Map<String, List<String>> notifyMap = new HashMap<>();

        List<Subscription> allSubs = new ArrayList<>();
        allSubs.addAll(perStreamerSubs);
        allSubs.addAll(globalSubs);

        for (Subscription sub : allSubs) {
            String discordChannelId = sub.getDiscordChannel().getDiscordChannelId();

            for (Keyword keyword : sub.getKeywords()) {
                if (containsKeyword(keyword.getValue(), dto)) {
                    notifyMap.computeIfAbsent(discordChannelId, k -> new ArrayList<>()).add(keyword.getValue());
                }
            }
        }

        // 4. 감지된 디스코드 채널별로 한 번씩 알림 전송
        for (Map.Entry<String, List<String>> entry : notifyMap.entrySet()) {
            String discordChannelId = entry.getKey();
            List<String> matchedKeywords = entry.getValue();

            notificationService.requestChangeEventNotification(channelId, discordChannelId, matchedKeywords, dto);
        }
    }


    private boolean containsKeyword(String keyword, LiveResponseDTO dto) {
        keyword = keyword.toLowerCase();

        if (dto.getLiveTitle() != null && dto.getLiveTitle().toLowerCase().contains(keyword)) return true;
        if (dto.getLiveCategoryValue() != null && dto.getLiveCategoryValue().toLowerCase().contains(keyword))
            return true;
        if (dto.getTags() != null) {
            for (String tag : dto.getTags()) {
                if (tag.toLowerCase().contains(keyword)) return true;
            }
        }
        return false;
    }

}
