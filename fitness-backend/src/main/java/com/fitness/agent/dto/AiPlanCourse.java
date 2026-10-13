package com.fitness.agent.dto;

import lombok.Data;

/**
 * 学习计划中的单门课程（含周次与训练天序号）
 */
@Data
public class AiPlanCourse {

    /** 训练天序号（跨周累加，1..N） */
    private Integer dayNo;

    /** 所属周次（1-4） */
    private Integer week;

    /** 该天课程组装结果 */
    private AiCourseResult course;
}
