package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 智能推荐接口
 */
@RestController
@RequestMapping("/api/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    @GetMapping("/plan")
    @RequireLogin
    public Result<List<Map<String, Object>>> plans(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(recommendService.recommendPlans(UserContext.getUserId(), limit));
    }

    @GetMapping("/recipe")
    @RequireLogin
    public Result<List<Map<String, Object>>> recipes(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(recommendService.recommendRecipes(UserContext.getUserId(), limit));
    }

    @PostMapping("/feedback")
    @RequireLogin
    public Result<Void> feedback(@RequestBody Map<String, Object> body) {
        int type = Integer.parseInt(String.valueOf(body.get("type")));
        Long targetId = Long.valueOf(String.valueOf(body.get("targetId")));
        String action = String.valueOf(body.get("action"));
        recommendService.feedback(UserContext.getUserId(), type, targetId, action);
        return Result.ok();
    }
}
