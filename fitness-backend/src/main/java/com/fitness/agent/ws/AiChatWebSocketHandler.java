package com.fitness.agent.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitness.agent.dto.AgentResponse;
import com.fitness.agent.orchestrate.AgentOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 私教 WebSocket 处理器（/ws/ai/chat，流式对话）
 * <p>协议：客户端发 {type:"message", content:"..."} / {type:"ping"}；
 * 服务端回 {type:"chunk", content:"..."} 分段 + {type:"done", data:{type,content,data}}；
 * 错误 {type:"error", message}。
 */
@Component
@RequiredArgsConstructor
public class AiChatWebSocketHandler extends TextWebSocketHandler {

    private final AgentOrchestrator orchestrator;
    private final ObjectMapper objectMapper;

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            session.sendMessage(new TextMessage(json("error", "未登录或会话已过期", null)));
            return;
        }

        String content;
        String type;
        try {
            JsonNode node = objectMapper.readTree(message.getPayload());
            type = node.path("type").asText("");
            content = node.path("content").asText("");
        } catch (Exception e) {
            type = "message";
            content = message.getPayload();
        }

        if ("ping".equals(type)) {
            session.sendMessage(new TextMessage(json("pong", null, null)));
            return;
        }
        if (content == null || content.isBlank()) {
            session.sendMessage(new TextMessage(json("error", "请输入内容", null)));
            return;
        }

        AgentResponse resp = orchestrator.handle(userId, content);
        stream(session, resp);
    }

    /** 分段发送 chunk（模拟流式），最后发 done 帧带结构化数据 */
    private void stream(WebSocketSession session, AgentResponse resp) throws Exception {
        String content = resp.getContent() == null ? "" : resp.getContent();
        String[] segs = content.split("(?<=[。！？\\n])");
        for (String seg : segs) {
            if (seg == null || seg.isBlank()) continue;
            session.sendMessage(new TextMessage(json("chunk", seg, null)));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", resp.getType());
        data.put("content", content);
        data.put("data", resp.getData());
        session.sendMessage(new TextMessage(json("done", null, data)));
    }

    private String json(String type, String content, Object data) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        if (content != null) m.put("content", content);
        if (data != null) m.put("data", data);
        try {
            return objectMapper.writeValueAsString(m);
        } catch (Exception e) {
            return "{\"type\":\"error\"}";
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // 无资源需释放
    }
}
