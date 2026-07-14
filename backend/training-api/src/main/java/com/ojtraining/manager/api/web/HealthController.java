package com.ojtraining.manager.api.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @GetMapping({"/health", "/api/health"})
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.ok("服务正常", Map.of("status", "UP"));
    }
}
