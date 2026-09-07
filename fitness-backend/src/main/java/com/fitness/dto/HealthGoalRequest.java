package com.fitness.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class HealthGoalRequest {

    private BigDecimal targetWeight;
    private BigDecimal targetBodyFat;
}
