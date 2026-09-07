package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.NotifyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 审核通知接口
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotifyController {

    private final NotifyService notifyService;

    @GetMapping
    @RequireLogin
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(notifyService.listNotifications(UserContext.getUserId()));
    }
}
