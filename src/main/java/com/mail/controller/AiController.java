package com.mail.controller;

import com.mail.dto.response.ApiResponse;
import com.mail.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/optimize")
    public ApiResponse<Map<String, String>> optimize(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        String action = body.getOrDefault("action", "compose");
        String result = aiService.optimizeText(content, action);
        return ApiResponse.ok(Map.of("text", result));
    }
}
