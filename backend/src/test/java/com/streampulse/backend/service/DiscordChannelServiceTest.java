package com.streampulse.backend.service;

import com.streampulse.backend.dto.DiscordChannelRequestDTO;
import com.streampulse.backend.entity.DiscordChannel;
import com.streampulse.backend.repository.DiscordChannelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscordChannelServiceTest {

    @Mock
    private DiscordChannelRepository discordChannelRepository;

    @InjectMocks
    private DiscordChannelService discordChannelService;

    @Test
    void saveDiscordChannel_shouldActivateIfAlreadyExistsButInactive() {
        // given
        DiscordChannelRequestDTO request = new DiscordChannelRequestDTO();
        request.setDiscordChannelId("abc123");
        request.setDiscordGuildId("guild1");

        DiscordChannel existingChannel = mock(DiscordChannel.class);
        when(existingChannel.isActive()).thenReturn(false);

        when(discordChannelRepository.findByDiscordChannelId("abc123"))
                .thenReturn(Optional.of(existingChannel));

        // when
        discordChannelService.saveDiscordChannel(request);

        // then
        verify(existingChannel).activate();
        verify(discordChannelRepository, never()).save(any());
    }

    @Test
    void saveDiscordChannel_shouldCreateNewIfNotExists() {
        // given
        DiscordChannelRequestDTO request = new DiscordChannelRequestDTO();
        request.setDiscordChannelId("xyz999");
        request.setDiscordGuildId("guild2");

        when(discordChannelRepository.findByDiscordChannelId("xyz999"))
                .thenReturn(Optional.empty());

        // when
        discordChannelService.saveDiscordChannel(request);

        // then
        verify(discordChannelRepository).save(argThat(channel ->
                channel.getDiscordChannelId().equals("xyz999") &&
                        channel.getDiscordGuildId().equals("guild2") &&
                        channel.isActive()
        ));
    }
}
