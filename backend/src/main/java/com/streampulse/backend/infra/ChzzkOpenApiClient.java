package com.streampulse.backend.infra;

import com.streampulse.backend.dto.ChzzkRootResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChzzkOpenApiClient {

    private final RestTemplate restTemplate;

    @Value("${chzzk.api-base-url}")
    private String chzzkBaseUrl;

    @Value("${chzzk.client-id}")
    private String clientId;

    @Value("${chzzk.client-secret}")
    private String clientSecret;

    public ChzzkRootResponseDTO fetchPage(String next) {
        String url = chzzkBaseUrl + "/open/v1/lives?size=20" + (next != null && !next.isEmpty() ? "&next=" + next : "");
        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-Id", clientId);
        headers.set("Client-Secret", clientSecret);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ChzzkRootResponseDTO> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ChzzkRootResponseDTO.class
            );
            return resp.getBody();
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("❗치지직 API 호출 제한 (429): {}", e.getMessage());
            }
        } catch (HttpServerErrorException e) {
            log.warn("❗치지직 API 서버 오류 ({}): {}", e.getStatusCode(), e.getMessage());
        } catch (ResourceAccessException e) {
            log.warn("❗치지직 API 네트워크 연결 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("❗치지직 API 호출 실패: {}", e.getMessage(), e);
        }
        return null;
    }
}
