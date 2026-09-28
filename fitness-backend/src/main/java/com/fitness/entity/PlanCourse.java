package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * plan_course 表实体：计划-课程关联（多课程组合，每天一门课）
 */
@Data
@TableName("plan_course")
public class PlanCourse {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long courseId;
    /** 训练天序号（1-7），每天一门课 */
    private Integer dayNo;
    private Integer sort;
    private LocalDateTime createTime;

}
