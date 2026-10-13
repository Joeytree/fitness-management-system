package com.fitness.agent.rule;

import com.fitness.agent.dto.AiCourseAction;
import com.fitness.agent.dto.AiCourseRequest;
import com.fitness.agent.dto.AiCourseResult;
import com.fitness.agent.dto.AiPlanCourse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 学习计划规则引擎（F7，确定性，不依赖 LLM）
 * <p>支持 1-12 周（默认 4）：适应(0.7) → 递增(+5%/周) → 减载(0.6)；
 * 渐进超负荷通过「组次随周递增」体现，具体动作由 {@link CourseAssembler} 组装。
 */
@Service
@RequiredArgsConstructor
public class PlanAssembler {

    private final CourseAssembler courseAssembler;

    public static final int DEFAULT_WEEKS = 4;
    public static final int MIN_WEEKS = 1;
    public static final int MAX_WEEKS = 12;

    /** 生成 N 周学习计划（跨周累加的训练天，每天一门课） */
    public List<AiPlanCourse> assemble(Integer goal, Integer level, List<String> equipment,
                                       List<String> muscles, List<String> contraindications,
                                       Integer duration, Integer weeks) {
        int lv = clamp(level == null ? 1 : level, 1, 3);
        int totalWeeks = clamp(weeks == null ? DEFAULT_WEEKS : weeks, MIN_WEEKS, MAX_WEEKS);
        int baseDuration = duration == null || duration <= 0 ? 45 : duration;
        String[][] split = splitDays(lv);
        List<AiPlanCourse> result = new ArrayList<>();
        int dayNo = 1;
        for (int week = 1; week <= totalWeeks; week++) {
            for (String[] dayMuscles : split) {
                AiCourseRequest req = new AiCourseRequest();
                req.setGoal(goal);
                req.setLevel(lv);
                req.setEquipment(equipment);
                req.setMuscles(List.of(dayMuscles));
                req.setContraindications(contraindications);
                req.setDuration(baseDuration);
                AiCourseResult course = courseAssembler.assemble(req);
                applyProgressiveOverload(course, week, totalWeeks);
                AiPlanCourse pc = new AiPlanCourse();
                pc.setDayNo(dayNo++);
                pc.setWeek(week);
                pc.setCourse(course);
                result.add(pc);
            }
        }
        return result;
    }

    /** 渐进超负荷：组次随周调整（第 1 周适应 0.7，末周减载 0.6，中间每周 +5%），并重算时长/消耗 */
    private void applyProgressiveOverload(AiCourseResult course, int week, int totalWeeks) {
        if (totalWeeks <= 2) return;
        double factor = weekFactor(week, totalWeeks);
        for (AiCourseAction a : course.getActions()) {
            if ("主项".equals(a.getRole()) || "辅助".equals(a.getRole())) {
                a.setSets(Math.max(2, (int) Math.round(a.getSets() * factor)));
                a.setReps(Math.max(6, (int) Math.round(a.getReps() * factor)));
            }
        }
        int totalSeconds = 0;
        for (AiCourseAction a : course.getActions()) {
            totalSeconds += a.getSets() * (a.getRest() + 30);
        }
        course.setDuration(Math.max(1, (int) Math.round(totalSeconds / 60.0)));
        course.setCalorie(course.getDuration() * 7);
    }

    /** 容量系数：第 1 周适应 0.7，末周减载 0.6，中间每周 +5% */
    private double weekFactor(int week, int totalWeeks) {
        if (week == 1) return 0.7;
        if (week == totalWeeks) return 0.6;
        return 1.0 + (week - 2) * 0.05;
    }

    /** 分化模板：每周训练日 + 每天目标部位（按水平） */
    private String[][] splitDays(int level) {
        return switch (level) {
            case 3 -> new String[][]{{"胸"}, {"背"}, {"腿"}, {"肩", "手臂"}};                 // 四分化
            case 2 -> new String[][]{{"胸", "肩"}, {"腿"}, {"背", "手臂"}, {"腿", "核心"}};   // 上下肢
            default -> new String[][]{{"全身"}, {"全身"}, {"全身"}};                          // 全身 3 天
        };
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
