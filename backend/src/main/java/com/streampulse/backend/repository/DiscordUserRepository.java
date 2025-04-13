package com.streampulse.backend.repository;

import com.streampulse.backend.entity.DiscordUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscordUserRepository extends JpaRepository<DiscordUser, Long> {
    Optional<DiscordUser> findByDiscordUserId(String discordUserId);
}