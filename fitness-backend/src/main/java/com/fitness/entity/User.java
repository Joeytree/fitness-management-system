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
 * user 表实体
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String openid;
    private String password;
    private String nickname;
    private String avatar;
    private String intro;
    private Integer gender;
    private LocalDate birthday;
    private BigDecimal height;
    private BigDecimal targetWeight;
    private BigDecimal targetBodyFat;
    private Integer goal;
    private Integer level;
    private String tags;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;

}
