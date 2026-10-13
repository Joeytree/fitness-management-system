package com.fitness.agent.dto;

import lombok.Data;

/**
 * AI 私教课程结果中的单个动作
 */
@Data
public class AiCourseAction {

    private Long actionId;
    private String name;
    private String part;
    private String equipment;
    private Integer difficulty;
    private Integer sets;
    private Integer reps;
    /** 组间休息（秒） */
    private Integer rest;
    /** 动作角色：热身/主项/辅助/收尾 */
    private String role;
}
