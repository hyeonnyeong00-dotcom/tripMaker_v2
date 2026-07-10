package com.tripplanner.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Anthropic Messages API 호출 전용 클라이언트. 저비용 모델(claude-haiku 계열) 사용을 전제로 한다. */
@Component
public class AnthropicClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public AnthropicClient(
            ObjectMapper objectMapper,
            @Value("${ai.api-key}") String apiKey,
            @Value("${ai.model}") String model,
            @Value("${ai.base-url}") String baseUrl) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
    }

    /**
     * 시스템/유저 프롬프트로 Messages API를 호출해 첫 텍스트 블록을 반환한다.
     * 네트워크/타임아웃/비정상 HTTP 상태는 모두 {@link AiCallException}으로 통일한다.
     */
    public String complete(String systemPrompt, String userPrompt, int maxTokens, Duration timeout) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", systemPrompt);
        ArrayNode messages = body.putArray("messages");
        ObjectNode userMessage = messages.addObject();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(timeout)
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .header("content-type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(
                            objectMapper.writeValueAsBytes(body)))
                    .build();
        } catch (IOException e) {
            throw new AiCallException("AI 요청 본문 직렬화 실패", e);
        }

        HttpResponse<byte[]> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AiCallException("AI API 호출 실패(네트워크/타임아웃)", e);
        }

        if (response.statusCode() / 100 != 2) {
            String responseBody = new String(response.body(), StandardCharsets.UTF_8);
            log.warn("Anthropic API 비정상 응답 status={} body={}", response.statusCode(), responseBody);
            throw new AiCallException("AI API가 비정상 상태코드를 반환함: " + response.statusCode(), null);
        }

        try {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty()) {
                throw new AiCallException("AI API 응답에 content가 없음", null);
            }
            return content.get(0).path("text").asText();
        } catch (IOException e) {
            throw new AiCallException("AI API 응답 파싱 실패", e);
        }
    }
}
