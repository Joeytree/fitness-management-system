package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * course 表实体
 */
@Data
@TableName("course")
public class Course {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    /** 部位：胸/背/肩/手臂/腿/核心/全身 */
    private String part;
    private String cover;
    private String intro;
    private Integer difficulty;
    private Integer duration;
    private Integer calorie;
    /** UGC 自定义课程提交者，管理端创建为空 */
    private Long userId;
    private Integer status;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer likeCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

}
