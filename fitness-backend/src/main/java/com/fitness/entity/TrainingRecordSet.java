package com.fitness.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * training_record_set 表实体：训练逐组明细
 */
@Data
@TableName("training_record_set")
public class TrainingRecordSet {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recordId;
    private Long actionId;
    /** 第几组 */
    private Integer setNo;
    /** 实际负重（kg，空=自重） */
    private BigDecimal weight;
    /** 实际次数 */
    private Integer reps;
    /** 是否完成 0 未完成 / 1 完成 */
    private Integer done;
    private LocalDateTime createTime;

}
