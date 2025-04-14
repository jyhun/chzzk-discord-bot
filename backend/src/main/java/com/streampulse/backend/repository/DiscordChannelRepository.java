package com.streampulse.backend.repository;

import com.streampulse.backend.entity.DiscordChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscordChannelRepository extends JpaRepository<DiscordChannel, Long> {
    Optional<DiscordChannel> findByDiscordChannelId(String discordChannelId);
}