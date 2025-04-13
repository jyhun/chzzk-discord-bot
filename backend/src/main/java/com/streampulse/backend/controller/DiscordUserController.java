package com.streampulse.backend.controller;

import com.streampulse.backend.dto.DiscordUserRequestDTO;
import com.streampulse.backend.service.DiscordUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discord-users")
@RequiredArgsConstructor
public class DiscordUserController {

    private final DiscordUserService discordUserService;

    @PostMapping
    public ResponseEntity<String> saveDiscordUser(@RequestBody DiscordUserRequestDTO request) {
        discordUserService.saveDiscordUser(request);
        return ResponseEntity.ok("디스코드 유저 저장 성공");
    }
}