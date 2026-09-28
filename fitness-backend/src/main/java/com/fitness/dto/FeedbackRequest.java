package com.fitness.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 推荐反馈请求（type: 1计划 / 2食谱 / 3课程）
 */
@Data
public class FeedbackRequest {

    @NotNull(message = "反馈类型不能为空")
    @Min(value = 1, message = "反馈类型取值 1-3")
    @Max(value = 3, message = "反馈类型取值 1-3")
    private Integer type;

    @NotNull(message = "目标不能为空")
    private Long targetId;

    @NotBlank(message = "反馈动作不能为空")
    private String action;
}
