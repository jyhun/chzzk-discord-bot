package com.streampulse.backend.controller;

import com.streampulse.backend.dto.NotificationRequestDTO;
import com.streampulse.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<String> saveNotification(@RequestBody NotificationRequestDTO notificationRequestDTO) {
        notificationService.saveNotification(notificationRequestDTO);
        return ResponseEntity.ok("알림 저장 성공");
    }

}
