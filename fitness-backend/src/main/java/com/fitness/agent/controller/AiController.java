package com.fitness.agent.controller;

import com.fitness.agent.dto.AgentResponse;
import com.fitness.agent.dto.AiCourseRequest;
import com.fitness.agent.dto.AiCourseResult;
import com.fitness.agent.dto.AiFeedbackRequest;
import com.fitness.agent.orchestrate.AgentOrchestrator;
import com.fitness.agent.session.AiSessionService;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.common.ResultCode;
import com.fitness.security.RequireLogin;
import com.fitness.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 私教 REST 接口（非流式补充；核心流式对话走 WebSocket /ws/ai/chat）
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AgentOrchestrator orchestrator;
    private final AiSessionService sessionService;

    /** 统一对话（非流式） */
    @PostMapping("/chat")
    @RequireLogin
    public Result<AgentResponse> chat(@RequestBody Map<String, String> body) {
        String content = body == null ? null : body.get("content");
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请输入内容");
        }
        return Result.ok(orchestrator.handle(UserContext.getUserId(), content));
    }

    /** 直接生成单次课程（跳过意图识别，按结构化参数组装） */
    @PostMapping("/course")
    @RequireLogin
    public Result<AiCourseResult> course(@RequestBody AiCourseRequest req) {
        return Result.ok(orchestrator.course(UserContext.getUserId(), req));
    }

    /** 生成多周学习计划（非流式，F7） */
    @PostMapping("/plan")
    @RequireLogin
    public Result<Map<String, Object>> plan(@RequestBody AiCourseRequest req) {
        return Result.ok(orchestrator.plan(UserContext.getUserId(), req));
    }

    /** 训练反馈：自评难度回灌画像（F5 闭环） */
    @PostMapping("/feedback")
    @RequireLogin
    public Result<Map<String, Object>> feedback(@RequestBody AiFeedbackRequest req) {
        return Result.ok(orchestrator.feedback(UserContext.getUserId(), req));
    }

    /** 知识问答（非流式，强制走知识问答，不识别课程/计划意图） */
    @GetMapping("/qa")
    @RequireLogin
    public Result<AgentResponse> qa(@RequestParam String q) {
        if (q == null || q.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请输入问题");
        }
        return Result.ok(orchestrator.answer(UserContext.getUserId(), q));
    }

    /** 查询会话历史（Redis 近 N 轮） */
    @GetMapping("/session")
    @RequireLogin
    public Result<List<Map<String, String>>> session() {
        return Result.ok(sessionService.load(UserContext.getUserId()));
    }

    /** 会话列表（分页，摘要） */
    @GetMapping("/session/list")
    @RequireLogin
    public Result<PageResult<Map<String, Object>>> sessionList(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(sessionService.listSessions(UserContext.getUserId(), page, pageSize));
    }

    /** 清空会话 */
    @DeleteMapping("/session")
    @RequireLogin
    public Result<Void> clearSession() {
        sessionService.clear(UserContext.getUserId());
        return Result.ok();
    }
}
