package com.fitness.agent.dto;

import lombok.Data;

/**
 * AI 私教训练反馈请求（F5 闭环：自评难度回灌用户画像）
 */
@Data
public class AiFeedbackRequest {

    /** 训练记录 id（可选，用于关联） */
    private Long recordId;

    /** 自评难度：1 太简单 / 2 适中 / 3 太难 */
    private Integer difficulty;

    /** 完成度 0-100（可选，低于 60 视为偏低，回灌下调难度） */
    private Integer completion;
}
