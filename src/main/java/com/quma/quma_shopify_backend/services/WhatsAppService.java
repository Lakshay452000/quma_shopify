package com.quma.quma_shopify_backend.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WhatsAppService {

    @Value("${whatsapp.api-url}")
    private String whatsappApiUrl;

    @Value("${whatsapp.access-token}")
    private String accessToken;

    public void sendOtp(String phoneNumber, String otp) {
        RestTemplate restTemplate = new RestTemplate();

        // Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Body
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", phoneNumber);
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", "sendotp"); // Template name

        Map<String, String> language = new HashMap<>();
        language.put("code", "en_US");
        template.put("language", language);

        Map<String, Object> otpParam = new HashMap<>();
        otpParam.put("type", "text");
        otpParam.put("text", otp); // This will replace {{1}} in template

        Map<String, Object> bodyComponent = new HashMap<>();
        bodyComponent.put("type", "body");
        bodyComponent.put("parameters", List.of(otpParam));

        template.put("components", List.of(bodyComponent));
        payload.put("template", template);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(whatsappApiUrl, entity, String.class);
        System.out.println("WhatsApp Response: " + response.getBody());
    }
}
