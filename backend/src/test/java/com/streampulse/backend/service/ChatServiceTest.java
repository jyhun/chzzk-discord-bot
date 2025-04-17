package com.streampulse.backend.service;

import com.streampulse.backend.dto.ChatMessagesRequestDTO;
import com.streampulse.backend.dto.GptMessageDTO;
import com.streampulse.backend.dto.GptResponseDTO;
import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.entity.StreamSession;
import com.streampulse.backend.entity.Streamer;
import com.streampulse.backend.repository.StreamEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private StreamEventRepository streamEventRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        // @Value로 주입받는 값 설정
        ReflectionTestUtils.setField(chatService, "openAiApiUrl", "https://mock.api/openai");
        ReflectionTestUtils.setField(chatService, "openAiApiKey", "mock-api-key");
    }

    @Test
    void collectChats_shouldCallGptAndSaveSummary() {
        // given
        String streamEventId = "1";
        String channelId = "abc123";
        String category = "게임";
        String title = "멋진 방송";
        List<String> messages = List.of("와", "대박", "레전드");

        ChatMessagesRequestDTO chatRequestDTO = new ChatMessagesRequestDTO();
        chatRequestDTO.setMessages(messages);

        Streamer streamer = Streamer.builder().channelId(channelId).build();
        StreamSession session = StreamSession.builder().streamer(streamer).build();
        StreamMetrics metrics = StreamMetrics.builder().category(category).title(title).streamSession(session).build();
        StreamEvent streamEvent = StreamEvent.builder().id(1L).streamMetrics(metrics).build();

        GptMessageDTO messageDTO = new GptMessageDTO();
        messageDTO.setContent("요약 내용");

        GptResponseDTO.Choice choice = new GptResponseDTO.Choice();
        choice.setMessage(messageDTO);

        GptResponseDTO responseDTO = GptResponseDTO.builder()
                .choices(List.of(choice))
                .build();

        ResponseEntity<GptResponseDTO> responseEntity = new ResponseEntity<>(responseDTO, HttpStatus.OK);

        when(streamEventRepository.findById(1L)).thenReturn(Optional.of(streamEvent));
        when(restTemplate.exchange(
                eq("https://mock.api/openai"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(GptResponseDTO.class)
        )).thenReturn(responseEntity);

        // when
        StreamEvent result = chatService.collectChats(channelId, streamEventId, chatRequestDTO);

        // then
        assertEquals("요약 내용", result.getSummary());
        assertEquals(3, result.getChatCount());

        verify(streamEventRepository).save(streamEvent);
        verify(notificationService).requestStreamHotNotification(streamEvent);
    }
}
