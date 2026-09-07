package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * body_data 表实体
 */
@Data
@TableName("body_data")
public class BodyData {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private BigDecimal weight;
    private BigDecimal bodyFat;
    private BigDecimal bmi;
    private BigDecimal chest;
    private BigDecimal waist;
    private BigDecimal hip;
    private BigDecimal arm;
    private BigDecimal muscle;
    private BigDecimal water;
    private LocalDate recordDate;
    private LocalDateTime createTime;

}
