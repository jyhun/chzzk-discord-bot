package com.streampulse.backend.service;

import com.streampulse.backend.aop.LogExecution;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;


@Service
@Transactional
@RequiredArgsConstructor
public class StreamerService {

    private final StreamerRepository streamerRepository;
    private final ChunkService chunkService;

    @Transactional(readOnly = true)
    public Streamer findByChannelId(String channelId) {
        return streamerRepository.findByChannelId(channelId).orElse(null);
    }

    @LogExecution
    public void updateLiveStatus(Streamer streamer, boolean isLive) {
        if (streamer.isLive() != isLive) {
            streamer.updateLive(isLive);
            streamerRepository.save(streamer);
        }
    }

    @Transactional(readOnly = true)
    public List<Streamer> findAllByChannelIdIn(Collection<String> channelIds) {
        return streamerRepository.findAllByChannelIdIn(channelIds);
    }

    public void markOffline(Set<String> endIds) {
        streamerRepository.markOffline(endIds);
    }

    public void saveStreamersInChunks(List<Streamer> streamers, int chunkSize) {
        for (int i = 0; i < streamers.size(); i += chunkSize) {
            chunkService.saveStreamersChunk(streamers.subList(i, Math.min(i + chunkSize, streamers.size())));
        }
    }

}
