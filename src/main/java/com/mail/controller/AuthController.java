package com.mail.controller;

import com.mail.dto.request.ChangePasswordRequest;
import com.mail.dto.request.LoginRequest;
import com.mail.dto.request.RegisterRequest;
import com.mail.dto.response.ApiResponse;
import com.mail.dto.response.AuthResponse;
import com.mail.security.UserPrincipal;
import com.mail.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.ok(authService.login(req));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ChangePasswordRequest req) {
        authService.changePassword(user.getId(), req);
        return ApiResponse.ok();
    }

    @PutMapping("/username")
    public ApiResponse<AuthResponse> updateUsername(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody java.util.Map<String, String> body) {
        return ApiResponse.ok(authService.updateUsername(user.getId(), body.get("username")));
    }
}
