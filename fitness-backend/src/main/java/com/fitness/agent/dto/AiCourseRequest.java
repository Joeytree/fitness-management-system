package com.fitness.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 私教课程生成/组装请求（REST 接口入参 + 规则引擎输入）
 */
@Data
public class AiCourseRequest {

    /** 训练目标：1增肌/2减脂/3塑形/4保持 */
    private Integer goal;
    /** 用户水平：1新手/2初级/3进阶 */
    private Integer level;
    /** 可用器械（中文，如 ["哑铃"]；空表示不限） */
    private List<String> equipment;
    /** 单次时长（分钟） */
    private Integer duration;
    /** 目标部位（如 ["胸"]；空默认全身） */
    private List<String> muscles;
    /** 禁忌项（如 ["腰伤"]；从用户画像补全） */
    private List<String> contraindications;
    /** 学习计划周数（F7，1-12，默认 4；仅 create_plan 用） */
    private Integer weeks;
}
