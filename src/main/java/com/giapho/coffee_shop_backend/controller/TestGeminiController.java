package com.giapho.coffee_shop_backend.controller;

import com.giapho.coffee_shop_backend.integration.gemini.GeminiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/test/gemini")
@RequiredArgsConstructor
@Tag(name = "Test Gemini API", description = "APIs để kiểm tra tích hợp Gemini")
public class TestGeminiController {

    private final GeminiClient geminiClient;

    @PostMapping("/generate")
    @Operation(summary = "Gửi yêu cầu tới Gemini", 
              description = "Gửi prompt tới Gemini API và nhận phản hồi")
    public ResponseEntity<String> testGemini(@RequestParam String prompt) {
        return geminiClient.generateContent(prompt)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.badRequest().body("Không thể tạo nội dung"));
    }
    
    @GetMapping("/models")
    @Operation(summary = "Liệt kê các model có sẵn",
              description = "Lấy danh sách tất cả các model có sẵn từ Gemini API")
    public ResponseEntity<List<String>> listAvailableModels() {
        List<String> models = geminiClient.listAvailableModels();
        if (models.isEmpty()) {
            return ResponseEntity.status(500).body(Collections.singletonList("Không thể lấy danh sách model. Vui lòng kiểm tra log để biết thêm chi tiết."));
        }
        return ResponseEntity.ok(models);
    }
}
