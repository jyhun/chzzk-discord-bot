package com.streampulse.backend.controller;

import com.streampulse.backend.dto.DiscordChannelRequestDTO;
import com.streampulse.backend.service.DiscordChannelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discord-channels")
@RequiredArgsConstructor
public class DiscordChannelController {

    private final DiscordChannelService discordChannelService;

    @PostMapping
    public ResponseEntity<String> saveDiscordChannel(@RequestBody DiscordChannelRequestDTO request) {
        discordChannelService.saveDiscordChannel(request);
        return ResponseEntity.ok("디스코드 유저 저장 성공");
    }
}