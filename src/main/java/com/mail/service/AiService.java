package com.mail.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.glm.api-key}")
    private String apiKey;

    @Value("${app.glm.model}")
    private String model;

    private static final String API_URL = "https://wincode.winning.com.cn/ai/v1/messages";

    /**
     * 使用 AI 优化邮件文本
     */
    public String optimizeText(String content, String action) {
        try {
            String prompt = buildPrompt(content, action);

            // Anthropic Messages API 格式
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("max_tokens", 2048);
            requestBody.put("system", "你是一个专业的邮件写作助手。请根据用户的要求优化邮件内容，保持原意不变，使语言更加得体、流畅、专业。直接输出优化后的邮件内容，不要添加任何解释。");

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode userMsg = objectMapper.createObjectNode();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            requestBody.set("messages", messages);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    API_URL, HttpMethod.POST, entity, String.class);

            log.info("AI API 响应状态: {}", response.getStatusCode());

            // 解析响应：在 content 数组中找到第一个 type=text 的项
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentArray = root.path("content");

            String text = "";
            if (contentArray.isArray()) {
                for (JsonNode item : contentArray) {
                    if ("text".equals(item.path("type").asText())) {
                        text = item.path("text").asText();
                        break;
                    }
                }
            }

            // 如果没找到，尝试其他格式
            if (text.isEmpty()) {
                text = root.path("choices").path(0).path("message").path("content").asText();
            }
            if (text.isEmpty()) {
                text = root.path("text").asText();
            }
            if (text.isEmpty()) {
                log.warn("AI返回内容为空，原始响应: {}", response.getBody());
                text = "AI返回内容为空，请重试";
            }

            return text;

        } catch (Exception e) {
            log.error("AI优化失败", e);
            throw new RuntimeException("AI优化失败: " + e.getMessage());
        }
    }

    private String buildPrompt(String content, String action) {
        String actionDesc = switch (action) {
            case "reply" -> "这是一封回复邮件，请优化内容使其更加得体专业";
            case "forward" -> "这是一封转发邮件，请优化内容使其简洁明了";
            default -> "请优化以下邮件内容，使其更加流畅、专业、得体";
        };
        return actionDesc + "：\n\n" + content;
    }
}
