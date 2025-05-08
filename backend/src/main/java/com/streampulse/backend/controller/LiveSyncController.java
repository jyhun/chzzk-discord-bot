package com.streampulse.backend.controller;

import com.streampulse.backend.service.LiveSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class LiveSyncController {

    private final LiveSyncService liveSyncService;

    @GetMapping("/lives")
    public ResponseEntity<String> syncLives() {
        liveSyncService.syncLiveBroadcasts();
        return ResponseEntity.ok("Live 방송 및 스트리머 동기화");
    }


}
