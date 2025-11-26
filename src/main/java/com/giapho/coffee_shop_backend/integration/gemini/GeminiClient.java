package com.giapho.coffee_shop_backend.integration.gemini;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.giapho.coffee_shop_backend.config.GeminiConfig;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final GeminiConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Timeout configuration
    private static final Duration CONNECTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration RESPONSE_TIMEOUT = Duration.ofSeconds(60);

    public GeminiClient(GeminiConfig config) {
        this.config = config;
        
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            log.warn("Gemini API key is not configured. GeminiClient will return empty responses.");
        }
        
        // Configure HTTP client with timeouts
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) CONNECTION_TIMEOUT.toMillis());
        requestFactory.setReadTimeout((int) RESPONSE_TIMEOUT.toMillis());
        
        this.restClient = RestClient.builder()
                .baseUrl(config.baseUrl() + "/" + config.apiVersion())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Liệt kê tất cả các model có sẵn từ API
     * @return Danh sách tên model
     */
    public List<String> listAvailableModels() {
        try {
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/models")
                            .queryParam("key", config.apiKey())
                            .build())
                    .retrieve()
                    .body(String.class);
            
            log.info("Available models: {}", response);
            
            // Parse response to extract model names
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            List<String> modelNames = new ArrayList<>();
            
            if (root.has("models")) {
                for (JsonNode modelNode : root.get("models")) {
                    if (modelNode.has("name")) {
                        modelNames.add(modelNode.get("name").asText());
                    }
                }
            }
            
            return modelNames;
        } catch (Exception e) {
            log.error("Failed to list available models: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public Optional<String> generateContent(String prompt) {
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            log.warn("Gemini API key is not configured");
            return Optional.empty();
        }

        if (prompt == null || prompt.isBlank()) {
            log.warn("Prompt cannot be empty");
            return Optional.empty();
        }
        
        // 1. Sử dụng model gemini-2.0-flash - model ổn định hơn
        String modelName = "gemini-2.0-flash";
        
        // 2. Log thông tin kết nối
        log.info("Using Gemini model: {}", modelName);
        log.info("Using API version: {}", config.apiVersion());
        
        // 3. In URL để debug
        String debugUrl = String.format("%s/%s/models/%s:generateContent?key=%s", 
            config.baseUrl(), 
            config.apiVersion(),
            modelName,
            "[API_KEY_HIDDEN]");
            
        log.info("Gemini API URL: {}", debugUrl);

        // Chuẩn bị request body
        GenerateContentRequest requestBody = GenerateContentRequest.builder()
                .contents(List.of(Content.builder()
                        .parts(List.of(Part.builder().text(prompt).build()))
                        .role("user")
                        .build()))
                .generationConfig(GenerationConfig.builder()
                        .temperature(config.temperature())
                        .topP(0.95f)  // Thêm topP để kiểm soát chất lượng đầu ra
                        .topK(40)      // Thêm topK cho đa dạng đầu ra
                        .maxOutputTokens(2048)  // Giới hạn độ dài phản hồi
                        .build())
                .safetySettings(List.of(  // Thêm cài đặt an toàn
                        SafetySetting.of("HARM_CATEGORY_HARASSMENT", "BLOCK_NONE"),
                        SafetySetting.of("HARM_CATEGORY_HATE_SPEECH", "BLOCK_NONE"),
                        SafetySetting.of("HARM_CATEGORY_SEXUALLY_EXPLICIT", "BLOCK_NONE"),
                        SafetySetting.of("HARM_CATEGORY_DANGEROUS_CONTENT", "BLOCK_NONE")
                ))
                .build();

        try {
            log.debug("Sending request to Gemini API with model: {}", config.model());
            
            // Xây dựng URI với model và API key
            String requestPath = String.format("/models/%s:generateContent", modelName);
            
            GenerateContentResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(requestPath)
                            .queryParam("key", config.apiKey())
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), (req, res) -> {
                        try {
                            String errorBody = "";
                            try {
                                errorBody = new String(res.getBody().readAllBytes(), "UTF-8");
                            } catch (Exception e) {
                                log.warn("Could not read error response body: {}", e.getMessage());
                            }
                            
                            String errorMessage = String.format("Gemini API error (%d): %s", 
                                res.getStatusCode().value(), 
                                errorBody.isEmpty() ? "No error details provided" : errorBody);
                                
                            log.error(errorMessage);
                            
                            // Provide specific guidance based on error code
                            int statusCode = res.getStatusCode().value();
                            switch (statusCode) {
                                case 400 -> log.error("Bad request. Please verify your request parameters and model configuration.");
                                case 401 -> log.error("Unauthorized. Please verify your API key in application.properties or environment variables.");
                                case 403 -> log.error("Permission denied. Your API key may not have access to this model or the model may not exist.");
                                case 404 -> log.error("Model not found. Available models can be listed with: {}?key=YOUR_API_KEY", 
                                    String.format("%s/%s/models", config.baseUrl(), config.apiVersion()));
                                case 408, 429, 500, 502, 503, 504 -> 
                                    log.error("Temporary server issue. Please try again later. Status: {}", statusCode);
                                default -> log.error("Unexpected error occurred. Status: {}", statusCode);
                            }
                            
                            throw new RuntimeException(String.format("Gemini API request failed with status %d: %s", 
                                statusCode, 
                                errorBody.isEmpty() ? "No additional details" : errorBody));
                                
                        } catch (Exception e) {
                            String errorMsg = String.format("Error processing Gemini API response: %s", e.getMessage());
                            log.error(errorMsg, e);
                            throw new RuntimeException(errorMsg, e);
                        }
                    })
                    .body(GenerateContentResponse.class);

            if (response == null) {
                log.warn("Gemini API returned null response");
                return Optional.empty();
            }
            
            if (response.candidates == null || response.candidates.isEmpty()) {
                log.warn("No content generated by Gemini API. Response: {}", objectMapper.writeValueAsString(response));
                return Optional.empty();
            }

            return response.candidates.stream()
                    .filter(candidate -> candidate != null && candidate.content != null)
                    .flatMap(candidate -> {
                        if (candidate.content.parts == null) {
                            return Stream.empty();
                        }
                        return candidate.content.parts.stream()
                                .filter(part -> part != null && part.text != null && !part.text.isBlank())
                                .map(part -> part.text.trim());
                    })
                    .findFirst();
        } catch (Exception ex) {
            String errorMsg = String.format("Gemini generateContent failed: %s", ex.getMessage());
            log.error(errorMsg, ex);
            
            if (ex.getCause() != null) {
                log.error("Root cause: {}", ex.getCause().getMessage());
            }
            
            return Optional.empty();
        }
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GenerateContentRequest {
        private final List<Content> contents;
        private final GenerationConfig generationConfig;
        private final List<SafetySetting> safetySettings;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Content {
        private final List<Part> parts;
        private final String role;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Part {
        private final String text;
        private final String inlineData; // For multimodal input
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GenerationConfig {
        private final Double temperature;
        private final Integer topK;
        private final Float topP;
        private final Integer maxOutputTokens;
        private final List<String> stopSequences;
    }

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SafetySetting {
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("threshold")
        private String threshold;
        
        @JsonCreator
        public SafetySetting(
            @JsonProperty("category") String category,
            @JsonProperty("threshold") String threshold
        ) {
            this.category = category;
            this.threshold = threshold;
        }
        
        // Factory method for easier creation
        public static SafetySetting of(String category, String threshold) {
            return new SafetySetting(category, threshold);
        }
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GenerateContentResponse {
        private List<Candidate> candidates;
        private PromptFeedback promptFeedback;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Candidate {
        private Content content;
        private String finishReason;
        private Integer index;
        private List<SafetyRating> safetyRatings;
        private List<CitationMetadata> citationMetadata;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SafetyRating {
        private String category;
        private String probability;
        private Boolean blocked;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CitationMetadata {
        private List<CitationSource> citationSources;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CitationSource {
        private String startIndex;
        private String endIndex;
        private String uri;
        private String license;
    }

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PromptFeedback {
        private List<SafetyRating> safetyRatings;
        private String blockReason;
    }
}
