package com.fitness.agent.safety;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 安全兜底（第一层关键词拦截，硬编码、快、保底）
 * <p>命中高风险话题返回固定话术，不进入 LLM、不落库；返回 null 表示通过。
 */
@Component
public class SafetyGuard {

    /** 高风险话题关键词（伤病处理/用药补剂/极端减食/未成年/孕产） */
    private static final List<String> HIGH_RISK = List.of(
            // 伤病处理
            "受伤", "骨折", "扭伤", "拉伤", "脱臼", "半月板", "腰椎间盘", "肌腱炎", "韧带",
            "腰伤", "膝伤", "肩伤", "肘伤", "腕伤", "踝伤", "背伤", "脚伤", "伤到", "伤了", "弄伤", "崴了", "疼得厉害", "剧痛",
            // 用药/补剂
            "吃药", "药物", "类固醇", "兴奋剂", "处方药", "减肥药", "止疼药", "打针",
            // 极端减食
            "绝食", "断食", "催吐", "节食减肥", "暴瘦", "一天只吃一顿",
            // 未成年
            "未成年", "儿童", "小孩", "小学生", "中学生训练", "青少年练",
            // 孕产
            "怀孕", "孕期", "孕妇", "产妇", "产后"
    );

    /** 检查输入，命中返回安全话术，否则返回 null */
    public String check(String input) {
        if (input == null || input.isBlank()) return null;
        for (String kw : HIGH_RISK) {
            if (input.contains(kw)) {
                return "该话题涉及健康与安全，建议咨询专业医生或教练，我无法提供相关建议。";
            }
        }
        return null;
    }
}
