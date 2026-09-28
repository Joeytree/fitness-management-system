package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    /** 可见性（仅提交参数，不入库）：private=仅自用 / public=申请公开 */
    @TableField(exist = false)
    private String visibility;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

}
