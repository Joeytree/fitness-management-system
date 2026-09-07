package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * recommend_record 表实体
 */
@Data
@TableName("recommend_record")
public class RecommendRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Integer type;
    private Long targetId;
    private Integer score;
    private Integer clicked;
    private Integer adopted;
    private LocalDateTime createTime;

}
