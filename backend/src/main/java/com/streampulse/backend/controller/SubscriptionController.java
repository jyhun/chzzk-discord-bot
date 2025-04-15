package com.streampulse.backend.controller;

import com.streampulse.backend.dto.SubscriptionRequestDTO;
import com.streampulse.backend.dto.SubscriptionResponseDTO;
import com.streampulse.backend.enums.EventType;
import com.streampulse.backend.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // 구독 생성
    @PostMapping
    public ResponseEntity<Map<String, String>> createSubscription(@RequestBody SubscriptionRequestDTO request) {
        try {
            subscriptionService.createSubscription(request);
            return ResponseEntity.ok(Map.of("message", "구독이 성공적으로 저장되었습니다."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 구독 해제
    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteSubscription(@RequestBody SubscriptionRequestDTO request) {
        try {
            subscriptionService.deactivateSubscription(request);
            return ResponseEntity.ok(Map.of("message", "구독이 성공적으로 해제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // (봇 + 내부용) 구독 목록 조회 (디스코드 채널 ID 있으면 봇용 / 없으면 내부용)
    @GetMapping
    public List<SubscriptionResponseDTO> getSubscriptions(
            @RequestParam(required = false) String discordChannelId,
            @RequestParam(required = false) String streamerId,
            @RequestParam(required = false) EventType eventType
    ) {
        if (discordChannelId != null) {
            return subscriptionService.getMySubscriptions(discordChannelId, streamerId, eventType);
        } else {
            return subscriptionService.getSubscriptions(streamerId, eventType);
        }
    }

}
