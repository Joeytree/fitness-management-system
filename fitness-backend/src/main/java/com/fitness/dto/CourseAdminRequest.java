package com.fitness.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CourseAdminRequest {

    @NotBlank(message = "课程名不能为空")
    private String name;

    @NotBlank(message = "分类不能为空")
    private String category;

    private String part;
    private String cover;
    private String intro;
    private Integer difficulty;
    private Integer duration;
    private Integer calorie;
    private Integer status;
    private List<Long> actionIds;
    /** 动作关联明细（含组数/次数/重量），优先于 actionIds */
    private List<CourseActionItem> actionItems;
}
