package com.fitness.controller;

import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.entity.Action;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.ActionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 动作接口
 */
@RestController
@RequestMapping("/api/action")
@RequiredArgsConstructor
public class ActionController {

    private final ActionService actionService;

    @GetMapping
    public Result<PageResult<Action>> list(@RequestParam(required = false) String part,
                                           @RequestParam(required = false) Integer difficulty,
                                           @RequestParam(required = false) String equipment,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(actionService.list(part, difficulty, equipment, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Action> detail(@PathVariable Long id) {
        return Result.ok(actionService.detail(id));
    }

    /** UGC 自定义动作提交（REQ-COURSE-007） */
    @PostMapping("/ugc")
    @RequireLogin
    public Result<Action> submitUgc(@RequestBody Action req) {
        return Result.ok(actionService.submitUgc(UserContext.getUserId(), req));
    }
}
