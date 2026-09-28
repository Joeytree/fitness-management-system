package com.fitness.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 管理员更新请求（仅更新传入字段）
 */
@Data
public class AdminUpdateRequest {

    private String nickname;

    private Long roleId;

    @Min(value = 0, message = "状态取值 0/1")
    @Max(value = 1, message = "状态取值 0/1")
    private Integer status;
}
