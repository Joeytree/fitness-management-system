package com.fitness.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DietRecordRequest {

    @NotNull(message = "餐次不能为空")
    @Min(value = 1, message = "餐次取值 1-4")
    @Max(value = 4, message = "餐次取值 1-4")
    private Integer mealType;

    @NotNull(message = "食物不能为空")
    private Long foodId;

    @NotNull(message = "摄入量不能为空")
    @DecimalMin(value = "0.1", message = "摄入量必须大于0")
    @DecimalMax(value = "9999.9", message = "摄入量过大")
    private java.math.BigDecimal amount;

    private LocalDate recordDate;
}
