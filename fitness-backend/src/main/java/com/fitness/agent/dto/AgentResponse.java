package com.fitness.agent.dto;

import lombok.Data;

/**
 * AI 私教编排结果（REST/WebSocket 统一返回）
 */
@Data
public class AgentResponse {

    /** 类型：text 文本 / course 课程 / plan 计划 / error 错误 */
    private String type;
    /** 自然语言内容 */
    private String content;
    /** 结构化数据（课程/计划） */
    private Object data;

    public static AgentResponse of(String type, String content, Object data) {
        AgentResponse r = new AgentResponse();
        r.setType(type);
        r.setContent(content);
        r.setData(data);
        return r;
    }
}
