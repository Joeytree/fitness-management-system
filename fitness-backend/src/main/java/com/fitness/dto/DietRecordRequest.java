package com.fitness.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DietRecordRequest {

    @NotNull(message = "餐次不能为空")
    private Integer mealType;

    @NotNull(message = "食物不能为空")
    private Long foodId;

    @NotNull(message = "摄入量不能为空")
    @DecimalMin(value = "0.1", message = "摄入量必须大于0")
    private java.math.BigDecimal amount;

    private LocalDate recordDate;
}
