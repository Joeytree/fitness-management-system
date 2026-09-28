package com.fitness.controller;

import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.CommentRequest;
import com.fitness.dto.CourseProgressRequest;
import com.fitness.dto.CourseUgcRequest;
import com.fitness.entity.Course;
import com.fitness.entity.CourseComment;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.CourseService;
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

import java.util.List;
import java.util.Map;

/**
 * 课程接口
 */
@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public Result<PageResult<Course>> list(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) String part,
                                           @RequestParam(required = false) Integer difficulty,
                                           @RequestParam(required = false) Integer minDuration,
                                           @RequestParam(required = false) Integer maxDuration,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(courseService.list(category, part, difficulty, minDuration, maxDuration, keyword, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(courseService.detail(id, UserContext.getUserId()));
    }

    @PostMapping("/{id}/favorite")
    @RequireLogin
    public Result<Boolean> favorite(@PathVariable Long id) {
        return Result.ok(courseService.favorite(id, UserContext.getUserId()));
    }

    @GetMapping("/favorites")
    @RequireLogin
    public Result<List<Course>> myFavorites() {
        return Result.ok(courseService.listMyFavorites(UserContext.getUserId()));
    }

    @DeleteMapping("/{id}/favorite")
    @RequireLogin
    public Result<Boolean> unfavorite(@PathVariable Long id) {
        return Result.ok(courseService.unfavorite(id, UserContext.getUserId()));
    }

    @PostMapping("/{id}/like")
    @RequireLogin
    public Result<Boolean> like(@PathVariable Long id) {
        return Result.ok(courseService.like(id, UserContext.getUserId()));
    }

    @GetMapping("/{id}/comments")
    public Result<List<Map<String, Object>>> comments(@PathVariable Long id) {
        return Result.ok(courseService.listComments(id));
    }

    @PostMapping("/{id}/comments")
    @RequireLogin
    public Result<CourseComment> addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest req) {
        return Result.ok(courseService.addComment(id, UserContext.getUserId(), req.getContent(), req.getRating()));
    }

    // ===== 学习进度（REQ-COURSE-008） =====

    @GetMapping("/progress")
    @RequireLogin
    public Result<List<Map<String, Object>>> myProgress() {
        return Result.ok(courseService.listMyProgress(UserContext.getUserId()));
    }

    @GetMapping("/{id}/progress")
    @RequireLogin
    public Result<Map<String, Object>> courseProgress(@PathVariable Long id) {
        return Result.ok(courseService.getCourseProgress(UserContext.getUserId(), id));
    }

    @PutMapping("/{id}/progress")
    @RequireLogin
    public Result<Map<String, Object>> recordProgress(@PathVariable Long id, @Valid @RequestBody CourseProgressRequest req) {
        Integer status = req.getStatus() == null ? 2 : req.getStatus();
        return Result.ok(courseService.recordProgress(UserContext.getUserId(), id, status));
    }

    // ===== UGC 自定义课程提交（REQ-COURSE-007） =====

    @PostMapping("/ugc")
    @RequireLogin
    public Result<Course> submitUgc(@RequestBody CourseUgcRequest req) {
        return Result.ok(courseService.submitUgcCourse(UserContext.getUserId(), req));
    }

    /** 我的自定义课程（全状态） */
    @GetMapping("/mine")
    @RequireLogin
    public Result<List<Course>> mine() {
        return Result.ok(courseService.listMine(UserContext.getUserId()));
    }

    /** 编辑自己的自定义课程（驳回后重提） */
    @PutMapping("/ugc/{id}")
    @RequireLogin
    public Result<Course> updateUgc(@PathVariable Long id, @RequestBody CourseUgcRequest req) {
        return Result.ok(courseService.updateUgc(UserContext.getUserId(), id, req));
    }

    /** 删除自己的自定义课程 */
    @DeleteMapping("/ugc/{id}")
    @RequireLogin
    public Result<Boolean> deleteUgc(@PathVariable Long id) {
        courseService.deleteUgc(UserContext.getUserId(), id);
        return Result.ok(true);
    }
}
