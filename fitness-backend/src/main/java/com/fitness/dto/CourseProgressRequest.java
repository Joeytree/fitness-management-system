package com.fitness.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 学习进度更新请求（ISS-014：status 限定 1/2/3）
 */
@Data
public class CourseProgressRequest {

    /** 进度状态：1未开始 / 2进行中 / 3已完成；空则默认 2 */
    @Min(value = 1, message = "进度状态取值 1/2/3")
    @Max(value = 3, message = "进度状态取值 1/2/3")
    private Integer status;
}
