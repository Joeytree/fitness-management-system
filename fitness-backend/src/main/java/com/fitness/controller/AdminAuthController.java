package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.AdminLoginRequest;
import com.fitness.security.RequireAdmin;
import com.fitness.security.UserContext;
import com.fitness.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 管理员认证接口
 */
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminService adminService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody AdminLoginRequest req) {
        return Result.ok(adminService.login(req.getUsername(), req.getPassword()));
    }

    @GetMapping("/me")
    @RequireAdmin
    public Result<Map<String, Object>> me() {
        return Result.ok(adminService.me(UserContext.getUserId()));
    }
}
