package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * course_progress 课程学习进度表（REQ-COURSE-008）
 */
@Data
@TableName("course_progress")
public class CourseProgress {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long courseId;
    /** 1未开始/2进行中/3已完成 */
    private Integer status;
    /** 学习次数 */
    private Integer learnCount;
    private LocalDateTime lastLearnTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
