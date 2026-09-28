package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * training_plan 表实体
 */
@Data
@TableName("training_plan")
public class TrainingPlan {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String name;
    private Integer goal;
    private Integer level;
    private String cycle;
    private String tags;
    /** 封面图（相对路径 /upload/xxx，套用模板时从模板继承） */
    private String cover;
    private Integer isTemplate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

}
