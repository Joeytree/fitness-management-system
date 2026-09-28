package com.fitness.controller;

import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.dto.AdminCreateRequest;
import com.fitness.dto.AdminUpdateRequest;
import com.fitness.dto.PermissionAssignRequest;
import com.fitness.dto.RoleRequest;
import com.fitness.dto.StatusRequest;
import com.fitness.entity.Admin;
import com.fitness.entity.Permission;
import com.fitness.entity.Role;
import com.fitness.entity.User;
import com.fitness.security.RequireAdmin;
import com.fitness.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
 * 管理端：仪表盘、用户管理、RBAC
 */
@RestController
@RequestMapping("/api/admin")
@RequireAdmin
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(adminService.dashboard());
    }

    @GetMapping("/review/count")
    public Result<Map<String, Long>> pendingCount() {
        return Result.ok(adminService.pendingCountOverview());
    }

    @GetMapping("/user")
    public Result<PageResult<User>> listUsers(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(required = false) Integer goal,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(adminService.listUsers(keyword, status, goal, page, pageSize));
    }

    @PutMapping("/user/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        adminService.updateUserStatus(id, req.getStatus());
        return Result.ok();
    }

    @GetMapping("/role")
    public Result<List<Role>> listRoles() {
        return Result.ok(adminService.listRoles());
    }

    @PostMapping("/role")
    public Result<Role> createRole(@Valid @RequestBody RoleRequest req) {
        return Result.ok(adminService.createRole(req.getName(), req.getCode(), req.getDescription()));
    }

    @PutMapping("/role/{id}")
    public Result<Role> updateRole(@PathVariable Long id, @Valid @RequestBody RoleRequest req) {
        return Result.ok(adminService.updateRole(id, req.getName(), req.getCode(), req.getDescription()));
    }

    @PutMapping("/role/{id}/permission")
    public Result<Void> assignPermission(@PathVariable Long id, @Valid @RequestBody PermissionAssignRequest req) {
        adminService.assignPermission(id, req.getPermissionIds());
        return Result.ok();
    }

    @GetMapping("/role/{id}/permission")
    public Result<List<Long>> getRolePermission(@PathVariable Long id) {
        return Result.ok(adminService.getRolePermissionIds(id));
    }

    @GetMapping("/permission")
    public Result<List<Permission>> listPermissions() {
        return Result.ok(adminService.listPermissions());
    }

    @GetMapping("/admin")
    public Result<List<Admin>> listAdmins() {
        return Result.ok(adminService.listAdmins());
    }

    @PostMapping("/admin")
    public Result<Admin> createAdmin(@Valid @RequestBody AdminCreateRequest req) {
        return Result.ok(adminService.createAdmin(
                req.getUsername(), req.getPassword(), req.getNickname(), req.getRoleId()));
    }

    @PutMapping("/admin/{id}")
    public Result<Admin> updateAdmin(@PathVariable Long id, @Valid @RequestBody AdminUpdateRequest req) {
        return Result.ok(adminService.updateAdmin(id, req.getNickname(), req.getRoleId(), req.getStatus()));
    }
}
