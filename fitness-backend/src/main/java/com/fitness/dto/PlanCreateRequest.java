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

    @NotEmpty(message = "请至少添加一个动作")
    private List<PlanActionItem> actions;

    @Data
    public static class PlanActionItem {
        @NotNull
        private Long actionId;
        /** 训练天序号（1-7），缺省为 1 */
        private Integer dayNo;
        @NotNull
        private Integer sets;
        @NotNull
        private Integer reps;
        /** 默认负重（kg），空=自重 */
        private BigDecimal weight;
        private Integer rest;
        private Integer sort;
    }
}
