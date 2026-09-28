package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.FeedbackRequest;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.RecommendProfileService;
import com.fitness.service.RecommendService;
import jakarta.validation.Valid;
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
    private final RecommendProfileService recommendProfileService;

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

    @GetMapping("/course")
    @RequireLogin
    public Result<List<Map<String, Object>>> courses(@RequestParam(defaultValue = "5") int limit) {
        return Result.ok(recommendService.recommendCourses(UserContext.getUserId(), limit));
    }

    @GetMapping("/profile")
    @RequireLogin
    public Result<Map<String, Object>> profile() {
        return Result.ok(recommendProfileService.profile(UserContext.getUserId()));
    }

    @PostMapping("/feedback")
    @RequireLogin
    public Result<Void> feedback(@Valid @RequestBody FeedbackRequest req) {
        recommendService.feedback(UserContext.getUserId(), req.getType(), req.getTargetId(), req.getAction());
        return Result.ok();
    }
}
