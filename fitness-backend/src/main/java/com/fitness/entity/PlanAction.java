package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * plan_action 表实体
 */
@Data
@TableName("plan_action")
public class PlanAction {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long planId;
    private Long actionId;
    /** 训练天序号（1-7），计划按天分组 */
    private Integer dayNo;
    private Integer sets;
    private Integer reps;
    /** 默认负重（kg），空表示自重 */
    private BigDecimal weight;
    private Integer rest;
    private Integer sort;

}
