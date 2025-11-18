package com.giapho.coffee_shop_backend.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.giapho.coffee_shop_backend.dto.analytics.AdminAnalyticsRequest;
import com.giapho.coffee_shop_backend.dto.analytics.DashboardMetricsDTO;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PromptBuilder {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static String buildPrompt(AdminAnalyticsRequest request, DashboardMetricsDTO metrics) {
        String metricsJson = toJson(metrics);
        return "Phân tích dữ liệu quản lý quán cà phê:\n" +
                "- Khoảng thời gian: " + request.from() + " đến " + request.to() + "\n" +
                "- Câu hỏi của quản trị viên: \"" + request.question() + "\"\n" +
                "- Dữ liệu thống kê (JSON):\n" + metricsJson + "\n" +
                "Yêu cầu: Trả lời bằng tiếng Việt, trình bày Markdown với các phần: Tổng quan, Xu hướng chính, Cảnh báo/Rủi ro, Gợi ý hành động, Trả lời trực tiếp câu hỏi người dùng.\n" +
                "Nếu dữ liệu thiếu, hãy nêu rõ giả định và giới hạn. Không bịa đặt số liệu.\n";
    }

    private static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Không thể chuyển đổi metrics sang JSON", e);
            return "{}";
        }
    }
}
