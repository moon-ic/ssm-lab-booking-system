package com.lab.booking.controller;

import com.lab.booking.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<Map<String, String>> health() {
        return Result.success(Map.of(
                "service", "admin-server",
                "status", "UP"
        ));
    }
}

