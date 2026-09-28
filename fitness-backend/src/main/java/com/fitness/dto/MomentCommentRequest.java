package com.fitness.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 动态评论请求
 */
@Data
public class MomentCommentRequest {

    @NotBlank(message = "评论内容不能为空")
    private String content;

    /** 回复的评论 id，可为空（表示直接评论动态） */
    private Long replyTo;
}
