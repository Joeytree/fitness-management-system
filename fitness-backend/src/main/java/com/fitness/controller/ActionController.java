package com.fitness.controller;

import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.entity.Action;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.ActionService;
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

import java.util.List;

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
                                           @RequestParam(required = false) boolean includeMine,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(actionService.list(part, difficulty, equipment, keyword,
                includeMine, UserContext.getUserId(), page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Action> detail(@PathVariable Long id) {
        return Result.ok(actionService.detail(id, UserContext.getUserId()));
    }

    /** UGC 自定义动作提交（REQ-COURSE-007） */
    @PostMapping("/ugc")
    @RequireLogin
    public Result<Action> submitUgc(@RequestBody Action req) {
        return Result.ok(actionService.submitUgc(UserContext.getUserId(), req));
    }

    /** 我的自定义动作（全状态） */
    @GetMapping("/mine")
    @RequireLogin
    public Result<List<Action>> mine() {
        return Result.ok(actionService.listMine(UserContext.getUserId()));
    }

    /** 编辑自己的自定义动作（驳回后重提） */
    @PutMapping("/ugc/{id}")
    @RequireLogin
    public Result<Action> updateUgc(@PathVariable Long id, @RequestBody Action req) {
        return Result.ok(actionService.updateUgc(UserContext.getUserId(), id, req));
    }

    /** 删除自己的自定义动作 */
    @DeleteMapping("/ugc/{id}")
    @RequireLogin
    public Result<Boolean> deleteUgc(@PathVariable Long id) {
        actionService.deleteUgc(UserContext.getUserId(), id);
        return Result.ok(true);
    }
}
