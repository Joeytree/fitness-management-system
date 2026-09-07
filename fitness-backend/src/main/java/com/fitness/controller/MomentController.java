package com.fitness.controller;

import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.MomentRequest;
import com.fitness.entity.Moment;
import com.fitness.entity.MomentComment;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.MomentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 动态接口
 */
@RestController
@RequestMapping("/api/moment")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;

    @PostMapping
    @RequireLogin
    public Result<Moment> publish(@Valid @RequestBody MomentRequest req) {
        String images = req.getImages() == null || req.getImages().isEmpty()
                ? null : String.join(",", req.getImages());
        return Result.ok(momentService.publish(UserContext.getUserId(), req.getContent(), images));
    }

    @GetMapping
    @RequireLogin
    public Result<PageResult<Map<String, Object>>> feed(
            @RequestParam(defaultValue = "recommend") String tab,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(momentService.feed(UserContext.getUserId(), tab, page, pageSize));
    }

    @GetMapping("/{id}")
    @RequireLogin
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(momentService.detail(UserContext.getUserId(), id));
    }

    @DeleteMapping("/{id}")
    @RequireLogin
    public Result<Void> delete(@PathVariable Long id) {
        momentService.delete(UserContext.getUserId(), id);
        return Result.ok();
    }

    @PostMapping("/{id}/like")
    @RequireLogin
    public Result<Void> like(@PathVariable Long id) {
        momentService.like(UserContext.getUserId(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/like")
    @RequireLogin
    public Result<Void> unlike(@PathVariable Long id) {
        momentService.unlike(UserContext.getUserId(), id);
        return Result.ok();
    }

    @GetMapping("/{id}/comments")
    @RequireLogin
    public Result<List<Map<String, Object>>> comments(@PathVariable Long id) {
        return Result.ok(momentService.listComments(id, UserContext.getUserId()));
    }

    @PostMapping("/{id}/comments")
    @RequireLogin
    public Result<MomentComment> addComment(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String content = (String) body.get("content");
        Long replyTo = body.get("replyTo") != null ? Long.valueOf(body.get("replyTo").toString()) : null;
        return Result.ok(momentService.addComment(UserContext.getUserId(), id, content, replyTo));
    }
}
