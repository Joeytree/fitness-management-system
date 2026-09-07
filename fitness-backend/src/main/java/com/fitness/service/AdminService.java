package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.ResultCode;
import com.fitness.entity.Action;
import com.fitness.entity.Admin;
import com.fitness.entity.CheckIn;
import com.fitness.entity.Course;
import com.fitness.entity.CourseComment;
import com.fitness.entity.Moment;
import com.fitness.entity.MomentComment;
import com.fitness.entity.Permission;
import com.fitness.entity.Role;
import com.fitness.entity.RolePermission;
import com.fitness.entity.User;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.AdminMapper;
import com.fitness.mapper.CheckInMapper;
import com.fitness.mapper.CourseCommentMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.MomentCommentMapper;
import com.fitness.mapper.MomentMapper;
import com.fitness.mapper.PermissionMapper;
import com.fitness.mapper.RoleMapper;
import com.fitness.mapper.RolePermissionMapper;
import com.fitness.mapper.UserMapper;
import com.fitness.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 管理端服务：登录、用户管理、仪表盘、RBAC
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminMapper adminMapper;
    private final UserMapper userMapper;
    private final CourseMapper courseMapper;
    private final CheckInMapper checkInMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final MomentMapper momentMapper;
    private final MomentCommentMapper momentCommentMapper;
    private final CourseCommentMapper courseCommentMapper;
    private final ActionMapper actionMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Map<String, Object> login(String username, String password) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username));
        if (admin == null || !encoder.matches(password, admin.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED, "账号或密码错误");
        }
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已禁用");
        }
        String token = jwtUtil.generateToken(admin.getId(), "admin");
        admin.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("admin", admin);
        return data;
    }

    /**
     * 当前登录管理员信息 + 权限编码（用于前端 RBAC 菜单过滤）
     */
    public Map<String, Object> me(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "管理员不存在");
        }
        admin.setPassword(null);
        Role role = admin.getRoleId() != null ? roleMapper.selectById(admin.getRoleId()) : null;
        List<String> perms = new ArrayList<>();
        if (role != null && "SUPER_ADMIN".equals(role.getCode())) {
            perms.add("all");
        } else if (role != null) {
            List<RolePermission> rps = rolePermissionMapper.selectList(
                    new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, role.getId()));
            List<Long> permIds = rps.stream().map(RolePermission::getPermissionId).toList();
            if (!permIds.isEmpty()) {
                perms = permissionMapper.selectBatchIds(permIds).stream()
                        .map(Permission::getCode).toList();
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("admin", admin);
        data.put("role", role);
        data.put("permissions", perms);
        return data;
    }

    public PageResult<User> listUsers(String keyword, Integer status, Integer goal, long page, long pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(User::getPhone, keyword).or().like(User::getNickname, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        if (goal != null) {
            wrapper.eq(User::getGoal, goal);
        }
        wrapper.orderByDesc(User::getId);
        Page<User> result = userMapper.selectPage(new Page<>(page, pageSize), wrapper);
        result.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    public void updateUserStatus(Long id, Integer status) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    /**
     * 数据仪表盘：统计卡片 + 图表数据（按月聚合）
     */
    public Map<String, Object> dashboard() {
        LocalDate today = LocalDate.now();
        long userCount = userMapper.selectCount(null);
        long todayCheckIn = checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getCheckDate, today));
        long courseCount = courseMapper.selectCount(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 2));
        long pendingCount = pendingCount();

        // 课程热度 Top6
        List<Course> hotCourses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .orderByDesc(Course::getViewCount).last("LIMIT 6"));

        Map<String, Object> data = new HashMap<>();
        data.put("userCount", userCount);
        data.put("todayCheckIn", todayCheckIn);
        data.put("checkInRate", userCount > 0 ? Math.round(todayCheckIn * 1000.0 / userCount) / 10.0 : 0);
        data.put("courseCount", courseCount);
        data.put("pendingCount", pendingCount);
        data.put("hotCourses", hotCourses.stream().map(c -> Map.of(
                "id", c.getId(), "name", c.getName(), "viewCount", c.getViewCount() == null ? 0 : c.getViewCount(),
                "favoriteCount", c.getFavoriteCount() == null ? 0 : c.getFavoriteCount())).toList());
        data.put("categoryShare", categoryShare());
        data.put("userGrowth", userGrowth(12));
        data.put("checkinTrend", checkinTrend(12));
        return data;
    }

    private long pendingCount() {
        return pendingCountOverview().values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * 各类型待审核数量（用于顶栏红点与审核页 Tab 徽标）
     */
    public Map<String, Long> pendingCountOverview() {
        long moment = momentMapper.selectCount(new LambdaQueryWrapper<Moment>().eq(Moment::getStatus, 0));
        long comment = courseCommentMapper.selectCount(new LambdaQueryWrapper<CourseComment>().eq(CourseComment::getStatus, 0));
        long momentComment = momentCommentMapper.selectCount(new LambdaQueryWrapper<MomentComment>().eq(MomentComment::getStatus, 0));
        long action = actionMapper.selectCount(new LambdaQueryWrapper<Action>().eq(Action::getStatus, 1));
        long course = courseMapper.selectCount(new LambdaQueryWrapper<Course>().eq(Course::getStatus, 1));
        Map<String, Long> map = new HashMap<>();
        map.put("moment", moment);
        map.put("comment", comment);
        map.put("momentComment", momentComment);
        map.put("action", action);
        map.put("course", course);
        map.put("total", moment + comment + momentComment + action + course);
        return map;
    }

    private List<Map<String, Object>> categoryShare() {
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 2));
        Map<String, Integer> count = new TreeMap<>();
        for (Course c : courses) {
            count.merge(c.getCategory() == null ? "其他" : c.getCategory(), 1, Integer::sum);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        count.forEach((k, v) -> list.add(Map.of("name", k, "value", v)));
        return list;
    }

    private List<Integer> userGrowth(int months) {
        DateTimeFormatter fm = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Integer> monthNew = new TreeMap<>();
        for (User u : userMapper.selectList(null)) {
            if (u.getCreateTime() == null) continue;
            monthNew.merge(u.getCreateTime().format(fm), 1, Integer::sum);
        }
        List<Integer> growth = new ArrayList<>();
        int acc = 0;
        YearMonth cursor = YearMonth.now().minusMonths(months - 1);
        for (int i = 0; i < months; i++) {
            acc += monthNew.getOrDefault(cursor.toString(), 0);
            growth.add(acc);
            cursor = cursor.plusMonths(1);
        }
        return growth;
    }

    private List<Integer> checkinTrend(int months) {
        DateTimeFormatter fm = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, Integer> monthCount = new TreeMap<>();
        for (CheckIn c : checkInMapper.selectList(null)) {
            if (c.getCheckDate() == null) continue;
            monthCount.merge(c.getCheckDate().format(fm), 1, Integer::sum);
        }
        List<Integer> trend = new ArrayList<>();
        YearMonth cursor = YearMonth.now().minusMonths(months - 1);
        for (int i = 0; i < months; i++) {
            trend.add(monthCount.getOrDefault(cursor.toString(), 0));
            cursor = cursor.plusMonths(1);
        }
        return trend;
    }

    public List<Role> listRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getId));
    }

    public Role createRole(String name, String code, String description) {
        Role role = new Role();
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        roleMapper.insert(role);
        return role;
    }

    public Role updateRole(Long id, String name, String code, String description) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "角色不存在");
        }
        role.setName(name);
        role.setCode(code);
        role.setDescription(description);
        roleMapper.updateById(role);
        return role;
    }

    public List<Permission> listPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSort));
    }

    public void assignPermission(Long roleId, List<Long> permissionIds) {
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>()
                .eq(RolePermission::getRoleId, roleId));
        for (Long pid : permissionIds) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(roleId);
            rp.setPermissionId(pid);
            rolePermissionMapper.insert(rp);
        }
    }

    public List<Long> getRolePermissionIds(Long roleId) {
        List<RolePermission> rps = rolePermissionMapper.selectList(
                new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));
        return rps.stream().map(RolePermission::getPermissionId).toList();
    }

    public List<Admin> listAdmins() {
        List<Admin> admins = adminMapper.selectList(new LambdaQueryWrapper<Admin>().orderByAsc(Admin::getId));
        admins.forEach(a -> a.setPassword(null));
        return admins;
    }

    public Admin createAdmin(String username, String password, String nickname, Long roleId) {
        Long cnt = adminMapper.selectCount(new LambdaQueryWrapper<Admin>()
                .eq(Admin::getUsername, username));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS, "账号已存在");
        }
        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPassword(encoder.encode(password));
        admin.setNickname(nickname);
        admin.setRoleId(roleId);
        admin.setStatus(1);
        adminMapper.insert(admin);
        return admin;
    }

    public Admin updateAdmin(Long id, String nickname, Long roleId, Integer status) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "管理员不存在");
        }
        if (nickname != null) admin.setNickname(nickname);
        if (roleId != null) admin.setRoleId(roleId);
        if (status != null) admin.setStatus(status);
        adminMapper.updateById(admin);
        return admin;
    }
}
