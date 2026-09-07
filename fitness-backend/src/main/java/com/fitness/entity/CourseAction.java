package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * course_action 表实体
 */
@Data
@TableName("course_action")
public class CourseAction {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long actionId;
    private Integer sort;
    /** 组数 */
    private Integer sets;
    /** 每组次数 */
    private Integer reps;
    /** 重量(kg)，徒手动作可空 */
    private Integer weight;

}
