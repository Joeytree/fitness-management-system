package com.fitness.agent.session;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.common.PageResult;
import com.fitness.entity.AiSession;
import com.fitness.mapper.AiSessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 私教会话上下文（Redis 热数据 + MySQL ai_session 持久化）
 * <p>Redis 故障时静默降级为空上下文；MySQL 持久化保证 Redis 过期后历史不丢。
 */
@Service
@RequiredArgsConstructor
public class AiSessionService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AiSessionMapper aiSessionMapper;

    private static final String KEY_PREFIX = "ai:chat:";
    private static final int MAX_HISTORY = 20;
    private static final Duration TTL = Duration.ofHours(24);

    public void append(Long userId, String role, String content) {
        List<Map<String, String>> msgs = new ArrayList<>();
        // 1. 写 Redis 热数据
        try {
            String key = KEY_PREFIX + userId;
            msgs = load(userId);
            Map<String, String> m = new LinkedHashMap<>();
            m.put("role", role);
            m.put("content", content == null ? "" : content);
            msgs.add(m);
            while (msgs.size() > MAX_HISTORY) {
                msgs.remove(0);
            }
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(msgs), TTL);
        } catch (Exception ignored) {
            // Redis 不可用，静默降级
        }
        // 2. 持久化到 MySQL（Redis 过期后仍可查历史）
        try {
            if (msgs.isEmpty()) {
                msgs = load(userId);
            }
            persist(userId, objectMapper.writeValueAsString(msgs));
        } catch (Exception ignored) {
        }
    }

    public List<Map<String, String>> load(Long userId) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + userId);
            if (json == null || json.isBlank()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<List<Map<String, String>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void clear(Long userId) {
        try {
            redisTemplate.delete(KEY_PREFIX + userId);
        } catch (Exception ignored) {
        }
    }

    /** 落库 ai_session（按 user_id + session_id 幂等更新） */
    private void persist(Long userId, String messagesJson) {
        String sessionId = "user-" + userId;
        AiSession exist = aiSessionMapper.selectOne(new LambdaQueryWrapper<AiSession>()
                .eq(AiSession::getUserId, userId)
                .eq(AiSession::getSessionId, sessionId)
                .last("LIMIT 1"));
        if (exist != null) {
            exist.setMessages(messagesJson);
            aiSessionMapper.updateById(exist);
        } else {
            AiSession s = new AiSession();
            s.setUserId(userId);
            s.setSessionId(sessionId);
            s.setMessages(messagesJson);
            aiSessionMapper.insert(s);
        }
    }

    /** 会话列表（分页，摘要不含 messages 大字段） */
    public PageResult<Map<String, Object>> listSessions(Long userId, long page, long pageSize) {
        Page<AiSession> result = aiSessionMapper.selectPage(new Page<>(page, pageSize),
                new LambdaQueryWrapper<AiSession>().eq(AiSession::getUserId, userId).orderByDesc(AiSession::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (AiSession s : result.getRecords()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("sessionId", s.getSessionId());
            m.put("createTime", s.getCreateTime());
            m.put("messageCount", countMessages(s.getMessages()));
            list.add(m);
        }
        return PageResult.of(list, result.getTotal(), page, pageSize);
    }

    private int countMessages(String messagesJson) {
        try {
            JsonNode arr = objectMapper.readTree(messagesJson);
            return arr.isArray() ? arr.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
