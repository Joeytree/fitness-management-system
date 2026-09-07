package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.DietRecordRequest;
import com.fitness.entity.DietRecord;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.DietService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
 * 饮食记录接口
 */
@RestController
@RequestMapping("/api/diet")
@RequiredArgsConstructor
public class DietController {

    private final DietService dietService;

    @PostMapping("/record")
    @RequireLogin
    public Result<DietRecord> add(@Valid @RequestBody DietRecordRequest req) {
        return Result.ok(dietService.add(UserContext.getUserId(), req));
    }

    @GetMapping("/record")
    @RequireLogin
    public Result<List<Map<String, Object>>> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(dietService.listByDate(UserContext.getUserId(), date));
    }

    @PutMapping("/record/{id}")
    @RequireLogin
    public Result<DietRecord> update(@PathVariable Long id, @Valid @RequestBody DietRecordRequest req) {
        return Result.ok(dietService.update(UserContext.getUserId(), id, req));
    }

    @DeleteMapping("/record/{id}")
    @RequireLogin
    public Result<Void> delete(@PathVariable Long id) {
        dietService.delete(UserContext.getUserId(), id);
        return Result.ok();
    }

    @GetMapping("/summary")
    @RequireLogin
    public Result<Map<String, Object>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(dietService.summary(UserContext.getUserId(), date));
    }
}
