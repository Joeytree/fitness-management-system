package com.fitness.dto;

import lombok.Data;

/**
 * 打卡请求（planId / duration / calorie 均可选）
 */
@Data
public class CheckInRequest {

    private Long planId;
    private Integer duration;
    private Integer calorie;
}
