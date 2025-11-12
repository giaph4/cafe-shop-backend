package com.giapho.coffee_shop_backend.integration.gemini;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class GeminiClient {

    private final WebClient webClient;
    private final String apiKey;
    private final String model;
    private final double temperature;

    public GeminiClient(
            WebClient.Builder webClientBuilder,
            @Value("${gemini.api-key}") String apiKey,
            @Value("${gemini.model:gemini-1.5-flash}") String model,
            @Value("${gemini.temperature:0.3}") double temperature
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured. Please set GEMINI_API_KEY environment variable.");
        }
        this.apiKey = apiKey;
        this.model = model;
        this.temperature = temperature;
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Optional<String> generateContent(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return Optional.empty();
        }

        GenerateContentRequest requestBody = GenerateContentRequest.builder()
                .contents(List.of(new Content(List.of(new Part(prompt)))))
                .generationConfig(new GenerationConfig(temperature))
                .build();

        try {
            GenerateContentResponse response = webClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models/{model}:generateContent")
                            .queryParam("key", apiKey)
                            .build(model))
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(GenerateContentResponse.class)
                    .onErrorResume(error -> {
                        log.error("Gemini API call failed", error);
                        return Mono.empty();
                    })
                    .block();

            if (response == null || response.candidates == null || response.candidates.isEmpty()) {
                return Optional.empty();
            }

            return response.candidates.stream()
                    .map(candidate -> candidate.content)
                    .filter(content -> content != null && content.parts != null)
                    .flatMap(content -> content.parts.stream())
                    .map(part -> part.text)
                    .filter(text -> text != null && !text.isBlank())
                    .findFirst()
                    .map(String::trim);
        } catch (Exception ex) {
            log.error("Gemini generateContent failed", ex);
            return Optional.empty();
        }
    }

    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private record GenerateContentRequest(
            List<Content> contents,
            @JsonProperty("generationConfig") GenerationConfig generationConfig
    ) {
    }

    private record Content(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GenerationConfig(@JsonProperty("temperature") double temperature) {
    }

    private record GenerateContentResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }
}
