package com.invest.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.invest.config.InvestProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GoogleTokenVerifier {

    private static final Logger log = LoggerFactory.getLogger(GoogleTokenVerifier.class);
    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    public record GoogleUserInfo(String sub, String email, String name, String picture) {}

    private record TokenInfoResponse(
            String sub,
            String email,
            String name,
            String picture,
            String aud,
            @JsonProperty("error_description") String errorDescription) {}

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final InvestProperties investProperties;

    public GoogleTokenVerifier(ObjectMapper objectMapper, InvestProperties investProperties) {
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
        this.investProperties = investProperties;
    }

    public GoogleUserInfo verify(String idToken) {
        String body;
        try {
            body = restClient.get()
                    .uri(TOKENINFO_URL + "?id_token=" + idToken)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.warn("Google tokeninfo 요청 실패: {}", e.getMessage());
            throw new IllegalArgumentException("Google 인증 요청에 실패했습니다.");
        }

        TokenInfoResponse info;
        try {
            info = objectMapper.readValue(body, TokenInfoResponse.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Google 인증 응답 파싱에 실패했습니다.");
        }

        if (info.errorDescription() != null) {
            throw new IllegalArgumentException("유효하지 않은 Google 토큰입니다.");
        }

        String expectedClientId = investProperties.getGoogle().getClientId();
        if (expectedClientId != null && !expectedClientId.isBlank()
                && !expectedClientId.equals(info.aud())) {
            throw new IllegalArgumentException("토큰의 클라이언트 ID가 일치하지 않습니다.");
        }

        if (info.sub() == null || info.sub().isBlank()) {
            throw new IllegalArgumentException("Google 사용자 정보를 가져올 수 없습니다.");
        }

        return new GoogleUserInfo(info.sub(), info.email(), info.name(), info.picture());
    }
}
