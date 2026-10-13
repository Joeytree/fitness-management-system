package com.fitness.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProfileUpdateRequest {

    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;
    private BigDecimal height;
    private Integer goal;
    private Integer level;
    /** 偏好部位，逗号分隔 */
    private String preferParts;
    /** 禁忌人群，逗号分隔（腰伤/膝伤/肩伤等，选填，AI 私教安全底线） */
    private String contraindication;
    private String intro;
}
