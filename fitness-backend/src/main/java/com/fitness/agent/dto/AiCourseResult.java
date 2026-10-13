package com.fitness.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 私教课程组装结果（规则引擎输出）
 */
@Data
public class AiCourseResult {

    /** 落库后的课程 ID（未落库时为 null） */
    private Long courseId;
    private String name;
    /** 预计时长（分钟） */
    private Integer duration;
    /** 预估消耗（千卡，粗略） */
    private Integer calorie;
    private List<AiCourseAction> actions;
    /** 肌群覆盖 */
    private List<String> coveredParts;
    /** 降级/说明（如候选不足放宽） */
    private String note;
}
