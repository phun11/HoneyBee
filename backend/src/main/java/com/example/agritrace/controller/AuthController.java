package com.example.agritrace.controller;

import com.example.agritrace.dto.ApiResponse;
import com.example.agritrace.dto.LoginRequest;
import com.example.agritrace.repository.AuthRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthRepository repo;
    public AuthController(AuthRepository repo) { this.repo = repo; }

    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return ApiResponse.ok(repo.login(request.username, request.password, servletRequest.getRemoteAddr()));
    }
}
