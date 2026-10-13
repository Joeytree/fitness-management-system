package com.fitness.agent.dto;

import lombok.Data;

import java.util.List;

/**
 * LLM 意图识别结果（结构化槽位）
 */
@Data
public class IntentResult {

    /** 意图：recommend_course / create_plan / ask_question / chat */
    private String intent;
    /** 1增肌/2减脂/3塑形/4保持 */
    private Integer goal;
    /** 1新手/2初级/3进阶 */
    private Integer level;
    private List<String> muscles;
    private List<String> equipment;
    /** 单次时长（分钟） */
    private Integer duration;
    /** 学习计划周数 */
    private Integer weeks;
    /** 知识问答的问题 */
    private String question;
    /** 禁忌项（如 ["腰伤"]，用户话术或画像中识别） */
    private List<String> contraindications;
}
