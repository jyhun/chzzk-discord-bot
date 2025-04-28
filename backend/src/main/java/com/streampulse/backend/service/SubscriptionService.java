package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
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

import java.util.*;
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
    @LogExecution
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

        // 3. TOPIC 이벤트일 경우 키워드 필수
        if (dto.getEventType() == EventType.TOPIC &&
                (dto.getKeyword() == null || dto.getKeyword().trim().isEmpty())) {
            throw new IllegalArgumentException("TOPIC 이벤트는 키워드가 필수입니다.");
        }

        if (dto.getStreamerId() != null) {
            // 전체 방송자 구독이 이미 존재하는 경우 → 개별 방송자 구독 불가
            boolean hasGlobalSubscription = subscriptionRepository
                    .existsGlobalSubscription(
                            dto.getDiscordChannelId(), dto.getEventType());
            if (hasGlobalSubscription) {
                throw new IllegalStateException("해당 채널은 이미 전체 방송자 구독 중입니다. 개별 방송자 구독은 불가능합니다.");
            }
        }

        if (dto.getStreamerId() == null) {
            // 개별 방송자 구독이 이미 존재하는 경우 → 전체 방송자 구독 불가
            boolean hasPerStreamerSubscription = subscriptionRepository
                    .existsPerStreamerSubscription(
                            dto.getDiscordChannelId(), dto.getEventType());
            if (hasPerStreamerSubscription) {
                throw new IllegalStateException("해당 채널은 이미 개별 방송자 구독 중입니다. 전체 방송자 구독은 불가능합니다.");
            }
        }

        // 4. 기존 구독 존재 여부 확인
        Optional<Subscription> existingOpt = subscriptionRepository
                .findInactiveByChannelAndStreamerAndEventType(
                        dto.getDiscordChannelId(),
                        dto.getStreamerId(),
                        dto.getEventType()
                );

        Subscription subscription;

        // 5. 기존 구독이 있으면 중복 키워드 확인 후 추가 (active false 일때)
        if (existingOpt.isPresent()) {
            subscription = existingOpt.get();
            subscription.activate();

            if (dto.getEventType() == EventType.TOPIC) {
                String keyword = dto.getKeyword().trim().toLowerCase();
                if (subscription.getKeywords().stream().anyMatch(k -> k.getValue().equalsIgnoreCase(keyword))) {
                    throw new IllegalArgumentException("이미 등록된 키워드입니다.");
                }
                subscription.getKeywords().add(
                        Keyword.builder()
                                .subscription(subscription)
                                .value(keyword)
                                .build()
                );
            }

        } else {
            // 6. 구독이 없다면 새로 생성
            subscription = Subscription.builder()
                    .discordChannel(discordChannel)
                    .streamer(streamer)
                    .eventType(dto.getEventType())
                    .active(true)
                    .build();

            // 키워드 추가 (TOPIC 이벤트인 경우만)
            if (dto.getEventType() == EventType.TOPIC) {
                Keyword keyword = Keyword.builder()
                        .subscription(subscription)
                        .value(dto.getKeyword().trim().toLowerCase())
                        .build();
                subscription.getKeywords().add(keyword);
            }

        }
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
    public List<SubscriptionResponseDTO> getMySubscriptions(String discordChannelId, String streamerId, EventType
            eventType) {
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
    @LogExecution
    public void deactivateSubscription(SubscriptionRequestDTO dto) {
        if(dto.getEventType() == EventType.TOPIC && dto.getKeyword() != null && dto.getKeyword().trim().isEmpty()) {
            throw new IllegalArgumentException("키워드를 입력해주세요.");
        }
        // TOPIC + 키워드 지정 → 해당 키워드만 제거
        if (dto.getEventType() == EventType.TOPIC && dto.getKeyword() != null) {
            Subscription sub = subscriptionRepository
                    .findByActiveChannelAndStreamerAndEventForDeactivate(
                            dto.getDiscordChannelId(),
                            dto.getStreamerId(),
                            dto.getEventType()
                    )
                    .stream().findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("구독이 존재하지 않습니다."));

            // 키워드만 제거
            String kw = dto.getKeyword().trim().toLowerCase();
            sub.getKeywords().removeIf(k -> k.getValue().equalsIgnoreCase(kw));

            // 남은 키워드 없으면 전체 구독 비활성화
            if (sub.getKeywords().isEmpty()) {
                sub.deactivate();
            }

            subscriptionRepository.save(sub);
            return;
        }

        List<Subscription> subscriptions;
        if (dto.getEventType() == null) {
            subscriptions = subscriptionRepository.findActiveByChannel(dto.getDiscordChannelId());
        } else if (dto.getStreamerId() == null) {
            subscriptions = subscriptionRepository.findByActiveChannelAndEventType(
                    dto.getDiscordChannelId(), dto.getEventType());
        } else {
            subscriptions = subscriptionRepository.findByActiveChannelAndStreamerAndEventForDeactivate(
                    dto.getDiscordChannelId(), dto.getStreamerId(), dto.getEventType());
        }

        if (subscriptions.isEmpty()) {
            throw new IllegalArgumentException("구독이 존재하지 않습니다.");
        }

        subscriptions.forEach(sub -> {
            // 전체 해제 시 키워드 전부 삭제
            sub.getKeywords().clear();
            sub.deactivate();
        });
        subscriptionRepository.saveAll(subscriptions);
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

    @LogExecution
    public void detectTopicEvent(LiveResponseDTO dto) {
        String channelId = dto.getChannelId();

        // 1. 특정 방송자 구독
        List<Subscription> perStreamerSubs = subscriptionRepository
                .findByStreamer_ChannelIdAndEventTypeAndActiveTrue(channelId, EventType.TOPIC);

        // 2. 전체 방송자 구독
        List<Subscription> globalSubs = subscriptionRepository
                .findByStreamerIsNullAndEventTypeAndActiveTrue(EventType.TOPIC);

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

            notificationService.requestStreamTopicNotification(channelId, dto.getChannelName(), discordChannelId, matchedKeywords, dto);
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

    public boolean hasSubscribersFor(EventType eventType, String channelId) {
        return subscriptionRepository.existsActiveByEventTypeAndChannelIdOrAll(eventType, channelId);
    }


}
