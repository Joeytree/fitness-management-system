package com.fitness.dto;

import lombok.Data;

/**
 * 课程动作关联明细（组数/次数/重量）
 */
@Data
public class CourseActionItem {

    private Long actionId;
    /** 组数 */
    private Integer sets;
    /** 每组次数 */
    private Integer reps;
    /** 重量(kg)，徒手动作可空 */
    private Integer weight;
}
