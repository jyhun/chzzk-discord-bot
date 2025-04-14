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

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public ResponseEntity<String> createSubscription(@RequestBody SubscriptionRequestDTO subscriptionRequestDTO) {
        try {
            subscriptionService.createSubscription(subscriptionRequestDTO);
            return ResponseEntity.ok("구독 저장 성공");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping
    public List<SubscriptionResponseDTO> getSubscriptions(@RequestParam(required = false) String streamerId, @RequestParam EventType eventType) {
        log.info("controller streamerId:{}, eventType:{}", streamerId, eventType);
        return subscriptionService.getSubscriptions(streamerId, eventType);
    }

}
