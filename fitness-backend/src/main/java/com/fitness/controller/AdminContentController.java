package com.fitness.controller;

import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.CourseAdminRequest;
import com.fitness.entity.Action;
import com.fitness.entity.Article;
import com.fitness.entity.Course;
import com.fitness.entity.Food;
import com.fitness.entity.Recipe;
import com.fitness.security.RequireAdmin;
import com.fitness.service.AdminContentService;
import com.fitness.service.FileService;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 管理端：内容管理 + 审核
 */
@RestController
@RequestMapping("/api/admin")
@RequireAdmin
@RequiredArgsConstructor
public class AdminContentController {

    private final AdminContentService contentService;
    private final FileService fileService;

    // ===== 文件上传 =====
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(fileService.upload(file));
    }

    // ===== 课程 =====
    @GetMapping("/course")
    public Result<PageResult<Course>> listCourse(@RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) String category,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(contentService.listCourse(status, keyword, category, page, pageSize));
    }

    @PostMapping("/course")
    public Result<Course> createCourse(@Valid @RequestBody CourseAdminRequest req) {
        return Result.ok(contentService.createCourse(req));
    }

    @GetMapping("/course/{id}")
    public Result<Map<String, Object>> getCourse(@PathVariable Long id) {
        return Result.ok(contentService.getCourseDetail(id));
    }

    @PutMapping("/course/{id}")
    public Result<Course> updateCourse(@PathVariable Long id, @Valid @RequestBody CourseAdminRequest req) {
        return Result.ok(contentService.updateCourse(id, req));
    }

    @PutMapping("/course/{id}/status")
    public Result<Void> updateCourseStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        contentService.updateCourseStatus(id, Integer.valueOf(String.valueOf(body.get("status"))));
        return Result.ok();
    }

    @DeleteMapping("/course/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        contentService.deleteCourse(id);
        return Result.ok();
    }

    // ===== 动作 =====
    @GetMapping("/action")
    public Result<PageResult<Action>> listAction(@RequestParam(required = false) String part,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(contentService.listAction(part, status, keyword, page, pageSize));
    }

    @PostMapping("/action")
    public Result<Action> createAction(@RequestBody Action action) {
        return Result.ok(contentService.createAction(action));
    }

    @GetMapping("/action/{id}")
    public Result<Action> getAction(@PathVariable Long id) {
        return Result.ok(contentService.getAction(id));
    }

    @PutMapping("/action/{id}")
    public Result<Action> updateAction(@PathVariable Long id, @RequestBody Action action) {
        return Result.ok(contentService.updateAction(id, action));
    }

    @PutMapping("/action/{id}/status")
    public Result<Void> updateActionStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        contentService.updateActionStatus(id, Integer.valueOf(String.valueOf(body.get("status"))));
        return Result.ok();
    }

    // ===== 审核 =====
    @GetMapping("/review")
    public Result<List<Map<String, Object>>> listReview(@RequestParam(defaultValue = "moment") String type,
                                                        @RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(contentService.listReview(type, page, pageSize));
    }

    @GetMapping("/review/done")
    public Result<List<Map<String, Object>>> listReviewed(@RequestParam(defaultValue = "moment") String type) {
        return Result.ok(contentService.listReviewed(type));
    }

    @PutMapping("/review/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestParam(defaultValue = "moment") String type) {
        contentService.approve(type, id);
        return Result.ok();
    }

    @PutMapping("/review/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestParam(defaultValue = "moment") String type,
                               @RequestBody Map<String, Object> body) {
        contentService.reject(type, id, body.get("reason") == null ? null : String.valueOf(body.get("reason")));
        return Result.ok();
    }

    // ===== 食物 =====
    @GetMapping("/food")
    public Result<PageResult<Food>> listFood(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "20") long pageSize) {
        return Result.ok(contentService.listFood(keyword, category, status, page, pageSize));
    }

    @PostMapping("/food")
    public Result<Food> createFood(@RequestBody Food food) {
        return Result.ok(contentService.createFood(food));
    }

    @PutMapping("/food/{id}")
    public Result<Food> updateFood(@PathVariable Long id, @RequestBody Food food) {
        return Result.ok(contentService.updateFood(id, food));
    }

    @DeleteMapping("/food/{id}")
    public Result<Void> deleteFood(@PathVariable Long id) {
        contentService.deleteFood(id);
        return Result.ok();
    }

    @PostMapping("/food/import")
    public Result<Map<String, Object>> importFood(@RequestParam("file") MultipartFile file) {
        return Result.ok(contentService.importFood(file));
    }

    // ===== 食谱 =====
    @GetMapping("/recipe")
    public Result<PageResult<Recipe>> listRecipe(@RequestParam(required = false) Integer goal,
                                                 @RequestParam(required = false) Integer status,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(contentService.listRecipe(goal, status, page, pageSize));
    }

    @PostMapping("/recipe")
    public Result<Recipe> createRecipe(@RequestBody Recipe recipe) {
        return Result.ok(contentService.createRecipe(recipe));
    }

    @PutMapping("/recipe/{id}")
    public Result<Recipe> updateRecipe(@PathVariable Long id, @RequestBody Recipe recipe) {
        return Result.ok(contentService.updateRecipe(id, recipe));
    }

    @DeleteMapping("/recipe/{id}")
    public Result<Void> deleteRecipe(@PathVariable Long id) {
        contentService.deleteRecipe(id);
        return Result.ok();
    }

    // ===== 文章 =====
    @GetMapping("/article")
    public Result<PageResult<Article>> listArticle(@RequestParam(required = false) Integer type,
                                                   @RequestParam(required = false) Integer status,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(contentService.listArticle(type, status, keyword, page, pageSize));
    }

    @PostMapping("/article")
    public Result<Article> createArticle(@RequestBody Article article) {
        return Result.ok(contentService.createArticle(article));
    }

    @GetMapping("/article/{id}")
    public Result<Article> getArticle(@PathVariable Long id) {
        return Result.ok(contentService.getArticle(id));
    }

    @PutMapping("/article/{id}")
    public Result<Article> updateArticle(@PathVariable Long id, @RequestBody Article article) {
        return Result.ok(contentService.updateArticle(id, article));
    }

    @DeleteMapping("/article/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        contentService.deleteArticle(id);
        return Result.ok();
    }
}
