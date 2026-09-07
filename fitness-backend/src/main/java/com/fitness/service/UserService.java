package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ResultCode;
import com.fitness.dto.LoginRequest;
import com.fitness.dto.ProfileUpdateRequest;
import com.fitness.dto.RegisterRequest;
import com.fitness.entity.User;
import com.fitness.mapper.UserMapper;
import com.fitness.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 用户服务：注册、登录、微信登录、资料管理
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${fitness.wechat.appid}")
    private String wxAppid;

    @Value("${fitness.wechat.secret}")
    private String wxSecret;

    public User register(RegisterRequest req) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS, "手机号已注册");
        }
        User user = new User();
        user.setPhone(req.getPhone());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(StringUtils.hasText(req.getNickname())
                ? req.getNickname()
                : "用户" + (1000 + new Random().nextInt(9000)));
        user.setStatus(1);
        userMapper.insert(user);
        return user;
    }

    public Map<String, Object> login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED, "手机号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), "user");
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return data;
    }

    /**
     * 微信一键登录（REQ-USER-007/008）：code 换 openid，无则自动注册
     */
    public Map<String, Object> wxLogin(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED, "微信登录失败，请重试");
        }
        String openid;
        try {
            String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + wxAppid
                    + "&secret=" + wxSecret + "&js_code=" + code + "&grant_type=authorization_code";
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());
            if (json == null || !json.has("openid")) {
                throw new BusinessException(ResultCode.WX_LOGIN_FAILED, "微信登录失败，请重试");
            }
            openid = json.get("openid").asText();
        } catch (Exception e) {
            throw new BusinessException(ResultCode.WX_LOGIN_FAILED, "微信登录失败，请重试");
        }

        // 查 openid，无则自动注册
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname("微信用户" + (1000 + new Random().nextInt(9000)));
            user.setStatus(1);
            userMapper.insert(user);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED, "账号已禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), "user");
        user.setPassword(null);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return data;
    }

    public User getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    public User updateProfile(Long userId, ProfileUpdateRequest req) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getAvatar() != null) user.setAvatar(req.getAvatar());
        if (req.getIntro() != null) user.setIntro(req.getIntro());
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getBirthday() != null) user.setBirthday(req.getBirthday());
        if (req.getHeight() != null) user.setHeight(req.getHeight());
        if (req.getGoal() != null) user.setGoal(req.getGoal());
        if (req.getLevel() != null) user.setLevel(req.getLevel());
        // 更新推荐标签
        user.setTags(buildTags(user.getGoal(), user.getLevel()));
        userMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    /**
     * 根据目标与水平生成推荐标签
     */
    private String buildTags(Integer goal, Integer level) {
        StringBuilder sb = new StringBuilder();
        if (goal != null) {
            String g = switch (goal) {
                case 1 -> "增肌";
                case 2 -> "减脂";
                case 3 -> "塑形";
                case 4 -> "保持";
                default -> null;
            };
            if (g != null) sb.append(g);
        }
        if (level != null) {
            String l = switch (level) {
                case 1 -> "新手";
                case 2 -> "初级";
                case 3 -> "进阶";
                default -> null;
            };
            if (l != null) {
                if (sb.length() > 0) sb.append(",");
                sb.append(l);
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }
}
