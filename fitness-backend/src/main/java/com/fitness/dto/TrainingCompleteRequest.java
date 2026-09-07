package com.fitness.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 完成训练请求
 */
@Data
public class TrainingCompleteRequest {

    private Long recordId;

    /** 训练时长（分钟），缺省按起止时间推算 */
    private Integer duration;

    private List<SetItem> sets;

    @Data
    public static class SetItem {
        private Long actionId;
        private Integer setNo;
        private BigDecimal weight;
        private Integer reps;
        /** 0 未完成 / 1 完成，缺省 1 */
        private Integer done;
    }
}
