package com.fitness.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BodyDataRequest {

    @NotNull(message = "体重不能为空")
    private BigDecimal weight;

    private BigDecimal bodyFat;
    private BigDecimal chest;
    private BigDecimal waist;
    private BigDecimal hip;
    private BigDecimal arm;
    private BigDecimal muscle;
    private BigDecimal water;
    private LocalDate recordDate;
}
