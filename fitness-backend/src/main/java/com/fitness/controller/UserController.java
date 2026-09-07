package com.fitness.controller;

import com.fitness.common.Result;
import com.fitness.dto.ProfileUpdateRequest;
import com.fitness.entity.User;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import com.fitness.service.FollowService;
import com.fitness.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final FollowService followService;

    @GetMapping("/profile")
    @RequireLogin
    public Result<User> profile() {
        return Result.ok(userService.getProfile(UserContext.getUserId()));
    }

    @PutMapping("/profile")
    @RequireLogin
    public Result<User> updateProfile(@Valid @RequestBody ProfileUpdateRequest req) {
        return Result.ok(userService.updateProfile(UserContext.getUserId(), req));
    }

    @GetMapping("/{id}")
    @RequireLogin
    public Result<Map<String, Object>> getUser(@PathVariable Long id) {
        return Result.ok(followService.userProfile(id, UserContext.getUserId()));
    }

    @GetMapping("/{id}/following")
    @RequireLogin
    public Result<List<Map<String, Object>>> following(@PathVariable Long id) {
        return Result.ok(followService.listFollowing(id, UserContext.getUserId()));
    }

    @GetMapping("/{id}/followers")
    @RequireLogin
    public Result<List<Map<String, Object>>> followers(@PathVariable Long id) {
        return Result.ok(followService.listFollowers(id, UserContext.getUserId()));
    }

    @PostMapping("/{id}/follow")
    @RequireLogin
    public Result<Void> follow(@PathVariable Long id) {
        followService.follow(UserContext.getUserId(), id);
        return Result.ok();
    }

    @DeleteMapping("/{id}/follow")
    @RequireLogin
    public Result<Void> unfollow(@PathVariable Long id) {
        followService.unfollow(UserContext.getUserId(), id);
        return Result.ok();
    }
}
