package com.streampulse.backend.entity;

import com.streampulse.backend.dto.StreamerRequestDTO;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Streamer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String channelId;

    private String nickname;

    private boolean live;

    public void updateLive(boolean live) {
        this.live = live;
    }

    public void update(StreamerRequestDTO streamerRequestDTO) {
        if(streamerRequestDTO.getChannelId() != null) {
            this.channelId = streamerRequestDTO.getChannelId();
        }
        if(streamerRequestDTO.getNickname() != null) {
            this.nickname = streamerRequestDTO.getNickname();
        }
    }
}
