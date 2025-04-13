package com.streampulse.backend.service;

import com.streampulse.backend.dto.DiscordUserRequestDTO;
import com.streampulse.backend.entity.DiscordUser;
import com.streampulse.backend.repository.DiscordUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DiscordUserService {

    private final DiscordUserRepository discordUserRepository;

    public void saveDiscordUser(DiscordUserRequestDTO request) {
        DiscordUser user = discordUserRepository.findByDiscordUserId(request.getDiscordUserId()).orElse(null);

        if (user != null) {
            if (!user.isActive()) {
                user.activate();
            }
            return;
        }

        DiscordUser newUser = DiscordUser
                .builder()
                .discordUserId(request.getDiscordUserId())
                .username(request.getUsername())
                .active(true)
                .build();

        discordUserRepository.save(newUser);
    }

}