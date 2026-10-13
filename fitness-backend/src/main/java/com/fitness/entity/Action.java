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
    /** 次肌群，逗号分隔（AI 私教：避免连续重复刺激同肌群） */
    private String secondaryMuscles;
    private Integer difficulty;
    /** 动作类型：1复合/2孤立（AI 私教：复合优先排序） */
    private Integer mechanic;
    /** 发力类型：1推/2拉/3静态（AI 私教：推拉平衡） */
    private Integer forceType;
    /** 禁忌人群，逗号分隔（腰伤/膝伤/肩伤等，AI 私教安全底线） */
    private String contraindication;
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
