package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * training_record 表实体：单次训练记录
 */
@Data
@TableName("training_record")
public class TrainingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long planId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    /** 训练时长（分钟） */
    private Integer duration;
    /** 估算消耗（千卡） */
    private Integer calorie;
    /** 完成动作数 */
    private Integer completedCount;
    private LocalDateTime createTime;

}
