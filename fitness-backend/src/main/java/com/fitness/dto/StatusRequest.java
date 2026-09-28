package com.fitness.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 通用状态更新请求（内容 status 0-4 / 用户 status 0-1）
 */
@Data
public class StatusRequest {

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态取值非法")
    @Max(value = 4, message = "状态取值非法")
    private Integer status;
}
