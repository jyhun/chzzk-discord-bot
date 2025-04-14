package com.streampulse.backend.service;

import com.streampulse.backend.dto.DiscordChannelRequestDTO;
import com.streampulse.backend.entity.DiscordChannel;
import com.streampulse.backend.repository.DiscordChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class DiscordChannelService {

    private final DiscordChannelRepository discordChannelRepository;

    public void saveDiscordChannel(DiscordChannelRequestDTO request) {
        DiscordChannel discordChannel = discordChannelRepository.findByDiscordChannelId(request.getDiscordChannelId()).orElse(null);

        if (discordChannel != null) {
            if (!discordChannel.isActive()) {
                discordChannel.activate();
            }
            return;
        }

        DiscordChannel newUser = DiscordChannel
                .builder()
                .discordChannelId(request.getDiscordChannelId())
                .discordGuildId(request.getDiscordGuildId())
                .active(true)
                .build();

        discordChannelRepository.save(newUser);
    }

}