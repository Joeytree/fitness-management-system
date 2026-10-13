package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ai_session 表实体：AI 私教会话记录
 */
@Data
@TableName("ai_session")
public class AiSession {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 会话标识（Redis 缓存键） */
    private String sessionId;
    /** 对话消息（JSON，近 N 轮） */
    private String messages;
    private LocalDateTime createTime;
}
