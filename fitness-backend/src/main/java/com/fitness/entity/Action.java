package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * action 表实体
 */
@Data
@TableName("action")
public class Action {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String part;
    private Integer difficulty;
    private String equipment;
    private String steps;
    private String tips;
    private String errors;
    private String breath;
    private Integer mediaType;
    private String mediaUrl;
    /** UGC 自定义动作提交者，管理端创建为空 */
    private Long userId;
    private Integer status;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

}
