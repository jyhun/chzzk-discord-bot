//package com.streampulse.backend.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Service;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class NotificationAsyncService {
//
//    private final RestTemplate restTemplate;
//
//    @Value("${processor.url}")
//    private String processorUrl;
//
//    @Async("taskExecutor")
//    public void sendAsync(String endpoint, Map<String, Object> payload) {
//        restTemplate.postForEntity(processorUrl + endpoint, payload, Void.class);
//    }
//
//}
