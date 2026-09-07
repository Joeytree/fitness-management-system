package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.BodyDataRequest;
import com.fitness.dto.HealthGoalRequest;
import com.fitness.entity.BodyData;
import com.fitness.entity.CheckIn;
import com.fitness.entity.User;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.HealthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 健康管理接口
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    @PostMapping("/body")
    @RequireLogin
    public Result<BodyData> record(@Valid @RequestBody BodyDataRequest req) {
        return Result.ok(healthService.record(UserContext.getUserId(), req));
    }

    @GetMapping("/body")
    @RequireLogin
    public Result<List<BodyData>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return Result.ok(healthService.list(UserContext.getUserId(), start, end));
    }

    @GetMapping("/trend")
    @RequireLogin
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "weight") String metric,
                                                   @RequestParam(defaultValue = "30d") String range) {
        return Result.ok(healthService.trend(UserContext.getUserId(), metric, range));
    }

    @GetMapping("/assessment")
    @RequireLogin
    public Result<Map<String, Object>> assessment() {
        return Result.ok(healthService.assessment(UserContext.getUserId()));
    }

    @PutMapping("/goal")
    @RequireLogin
    public Result<User> setGoal(@RequestBody HealthGoalRequest req) {
        return Result.ok(healthService.setGoal(UserContext.getUserId(), req.getTargetWeight(), req.getTargetBodyFat()));
    }

    @GetMapping("/goal")
    @RequireLogin
    public Result<Map<String, Object>> goalProgress() {
        return Result.ok(healthService.goalProgress(UserContext.getUserId()));
    }

    @PostMapping("/checkin")
    @RequireLogin
    public Result<CheckIn> checkIn(@RequestBody Map<String, Object> body) {
        Long planId = body.get("planId") != null ? Long.valueOf(body.get("planId").toString()) : null;
        Integer duration = body.get("duration") != null ? Integer.valueOf(body.get("duration").toString()) : null;
        Integer calorie = body.get("calorie") != null ? Integer.valueOf(body.get("calorie").toString()) : null;
        return Result.ok(healthService.checkIn(UserContext.getUserId(), planId, duration, calorie));
    }

    @GetMapping("/checkin")
    @RequireLogin
    public Result<Map<String, Object>> checkInInfo() {
        return Result.ok(healthService.checkInInfo(UserContext.getUserId()));
    }
}
