package com.aris.controller;
import com.aris.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
public class HealthController {
    @GetMapping("/api/health")
    public ApiResponse<String> health() {
        return new ApiResponse<>(
                true,
                "ARIS Backend is healthy",
                "ARIS Backend is running"
        );
    }
}
