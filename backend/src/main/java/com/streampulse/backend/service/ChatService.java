package com.streampulse.backend.service;

import com.streampulse.backend.dto.ChatMessagesRequestDTO;
import com.streampulse.backend.dto.GptMessageDTO;
import com.streampulse.backend.dto.GptRequestDTO;
import com.streampulse.backend.dto.GptResponseDTO;
import com.streampulse.backend.entity.StreamEvent;
import com.streampulse.backend.entity.StreamMetrics;
import com.streampulse.backend.repository.StreamEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    @Value("${openai.api.url}")
    private String openAiApiUrl;

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${processor.url}")
    private String processorUrl;

    private final RestTemplate restTemplate;
    private final StreamEventRepository streamEventRepository;
    private final NotificationService notificationService;

    public void collectChatsForStreamEvent(StreamEvent streamEvent) {
        try {
            String channelId = streamEvent.getMetrics().getSession().getStreamer().getChannelId();
            Long id = streamEvent.getId();

            String url = processorUrl + "/crawler";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("channelId", channelId);
            requestBody.put("streamEventId", id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        } catch (Exception e) {
            throw new RuntimeException("채팅 수집 프로세스 실행 실패", e);
        }
    }

    public StreamEvent collectChats(String channelId, String streamEventId, ChatMessagesRequestDTO chatMessagesRequestDTO) {
        StreamEvent streamEvent = streamEventRepository.findById(Long.parseLong(streamEventId))
                .orElseThrow(() -> new IllegalArgumentException("하이라이트를 찾을 수 없습니다."));

        StreamMetrics metrics = streamEvent.getMetrics();
        String category = metrics.getCategory();
        String title = metrics.getTitle();
        List<String> messages = chatMessagesRequestDTO.getMessages();

        String joinedMessages = String.join(" ", messages);

        String prompt = String.format(
                "이 방송은 [%s] 장르의 \"%s\" 콘텐츠야!\n\n" +
                        "지금부터 보여줄 채팅 로그는 시청자들이 갑자기 활발하게 반응한 구간이야. 😂🔥😢😡😲\n" +
                        "이 채팅 로그를 분석해서 시청자들이 어떤 감정(예: 웃음, 놀람, 감동, 분노 등)을 느꼈는지 분류하고,\n" +
                        "왜 그렇게 반응했는지 한두 문장으로 요약해줘.\n" +
                        "예상치 못한 상황, 멋진 플레이, 유머러스한 장면, 감동적인 순간 등 무엇이 원인인지 분석해서 알려줘.\n" +
                        "마치 하이라이트 영상 클립 설명처럼 짧고 강렬하게 정리해줘.\n" +
                        "너무 길게 말하지 말고 핵심만 빠르게 알려줘!\n\n" +
                        "[채팅 로그 시작]\n%s\n[채팅 로그 끝]",
                category,
                title,
                joinedMessages
        );

        GptRequestDTO gptRequestDTO = GptRequestDTO.builder()
                .model("gpt-3.5-turbo")
                .temperature(0.9)
                .max_tokens(250)
                .messages(
                        List.of(
                                new GptMessageDTO("system",
                                        "You are an energetic AI that analyzes live chat reactions from streaming broadcasts. " +
                                                "Your job is to explain in a short and exciting way what kind of emotions the viewers felt (such as excitement, laughter, surprise, anger, or sadness) and why they reacted that way. " +
                                                "Keep it short, punchy, and fun like a StreamEvent clip description."
                                ),
                                new GptMessageDTO("assistant",
                                        "시청자들은 예상치 못한 순간에 폭발적으로 반응했어! 멋진 플레이, 웃긴 장면, 감동적인 순간 등 무엇이든 시청자들의 감정을 자극하는 순간에 채팅이 폭발했지. 그 순간을 짧고 강렬하게 요약하면 바로 하이라이트야!"
                                ),
                                new GptMessageDTO("user", prompt)
                        )
                )
                .build();

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setBearerAuth(openAiApiKey);
        HttpEntity<GptRequestDTO> httpEntity = new HttpEntity<>(gptRequestDTO, httpHeaders);

        try {
            ResponseEntity<GptResponseDTO> response = restTemplate.exchange(
                    openAiApiUrl,
                    HttpMethod.POST,
                    httpEntity,
                    GptResponseDTO.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String summary = response.getBody().getChoices().get(0).getMessage().getContent();
                streamEvent.updateSummary(summary);
            } else {
                throw new RuntimeException("GPT API 호출 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("GPT API 호출 중 오류 발생", e);
        }

        notificationService.requestStreamHotNotification(streamEvent);

        return streamEvent;

    }

}
