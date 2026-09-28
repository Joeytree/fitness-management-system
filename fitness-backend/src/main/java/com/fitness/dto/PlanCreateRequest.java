package com.fitness.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PlanCreateRequest {

    @NotBlank(message = "计划名称不能为空")
    private String name;

    private Integer goal;
    private Integer level;
    private String cycle;
    private String tags;

    @NotEmpty(message = "请至少添加一门课程")
    private List<CourseItem> courses;

    @Data
    public static class CourseItem {
        @NotNull
        private Long courseId;
        /** 训练天序号（1-7），每天一门课，缺省为 1 */
        private Integer dayNo;
        private Integer sort;
    }
}
