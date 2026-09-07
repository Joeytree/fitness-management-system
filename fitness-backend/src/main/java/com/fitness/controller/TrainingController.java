package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.TrainingCompleteRequest;
import com.fitness.entity.TrainingRecord;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.TrainingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 训练执行接口（ISS-002）
 */
@RestController
@RequestMapping("/api/training")
@RequiredArgsConstructor
public class TrainingController {

    private final TrainingService trainingService;

    @PostMapping("/start")
    @RequireLogin
    public Result<TrainingRecord> start(@RequestBody(required = false) Map<String, Object> body) {
        Long planId = body != null && body.get("planId") != null
                ? Long.valueOf(body.get("planId").toString()) : null;
        return Result.ok(trainingService.start(UserContext.getUserId(), planId));
    }

    @PostMapping("/complete")
    @RequireLogin
    public Result<Map<String, Object>> complete(@RequestBody TrainingCompleteRequest req) {
        return Result.ok(trainingService.complete(UserContext.getUserId(), req));
    }

    @GetMapping
    @RequireLogin
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(trainingService.list(UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    @RequireLogin
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(trainingService.detail(UserContext.getUserId(), id));
    }
}
