package com.lab.booking.controller;

import com.lab.booking.common.Result;
import com.lab.booking.dto.AuthDtos;
import com.lab.booking.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.success(authService.me());
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody AuthDtos.ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.success();
    }
}
