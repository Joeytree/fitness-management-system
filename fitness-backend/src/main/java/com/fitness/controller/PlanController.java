package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.PlanCreateRequest;
import com.fitness.entity.TrainingPlan;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 训练计划接口
 */
@RestController
@RequestMapping("/api/plan")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @RequireLogin
    public Result<List<TrainingPlan>> listMine() {
        return Result.ok(planService.listMine(UserContext.getUserId()));
    }

    @GetMapping("/templates")
    public Result<List<TrainingPlan>> templates() {
        return Result.ok(planService.listTemplates());
    }

    @PostMapping
    @RequireLogin
    public Result<TrainingPlan> create(@Valid @RequestBody PlanCreateRequest req) {
        return Result.ok(planService.create(UserContext.getUserId(), req));
    }

    @PostMapping("/template/{id}/adopt")
    @RequireLogin
    public Result<TrainingPlan> adopt(@PathVariable Long id) {
        return Result.ok(planService.adoptTemplate(UserContext.getUserId(), id));
    }

    @GetMapping("/calendar")
    @RequireLogin
    public Result<List<LocalDate>> calendar(@RequestParam(required = false) Integer year,
                                            @RequestParam(required = false) Integer month) {
        return Result.ok(planService.calendar(UserContext.getUserId(), year, month));
    }

    @GetMapping("/{id}")
    @RequireLogin
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(planService.detail(UserContext.getUserId(), id));
    }

    @PutMapping("/{id}")
    @RequireLogin
    public Result<TrainingPlan> update(@PathVariable Long id, @Valid @RequestBody PlanCreateRequest req) {
        return Result.ok(planService.update(UserContext.getUserId(), id, req));
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    public Result<Void> delete(@PathVariable Long id) {
        planService.delete(UserContext.getUserId(), id);
        return Result.ok();
    }
}
