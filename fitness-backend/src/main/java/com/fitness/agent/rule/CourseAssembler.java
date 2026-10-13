package com.fitness.agent.rule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.agent.dto.AiCourseAction;
import com.fitness.agent.dto.AiCourseRequest;
import com.fitness.agent.dto.AiCourseResult;
import com.fitness.entity.Action;
import com.fitness.mapper.ActionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 课程组装规则引擎（确定性，不依赖 LLM）
 * <p>按 目标/水平/器械/时长/部位/禁忌 组装单次课程：
 * 结构 = 热身1 + 主项2（复合优先）+ 辅助2（孤立）+ 收尾1；禁忌零容忍；难度不越级。
 */
@Service
@RequiredArgsConstructor
public class CourseAssembler {

    private final ActionMapper actionMapper;

    /** goal -> {主项sets, 主项reps, 主项rest秒, 辅助sets, 辅助reps, 辅助rest秒} */
    private static final Map<Integer, int[]> VOLUME = Map.of(
            1, new int[]{4, 10, 75, 3, 12, 60},   // 增肌
            2, new int[]{3, 15, 45, 3, 15, 30},   // 减脂
            3, new int[]{3, 12, 60, 3, 15, 45},   // 塑形
            4, new int[]{3, 10, 60, 3, 12, 45}    // 保持
    );

    /** 每组动作执行估算耗时（秒），用于总时长估算 */
    private static final int SET_TIME_SECONDS = 30;

    public AiCourseResult assemble(AiCourseRequest req) {
        int goal = nvl(req.getGoal(), 1);
        int level = clamp(nvl(req.getLevel(), 1), 1, 3);
        int targetDuration = nvl(req.getDuration(), 30);
        List<String> muscles = emptyTo(req.getMuscles(), "全身");
        List<String> equipment = req.getEquipment();
        List<String> contra = req.getContraindications() == null ? List.of() : req.getContraindications();

        // 难度映射：数据集 difficulty 分布失衡（1 仅 21 条、2 为主流标准动作 1302 条、3 仅 2 条），
        // 故新手/初级开放 1~2，仅进阶(level=3)开放 3，避免 level=1 候选不足。
        int maxDiff = Math.min(3, Math.max(2, level));

        List<String> note = new ArrayList<>();

        // 1. 候选筛选：part + 难度（不越级）+ 禁忌过滤
        List<Action> pool = queryPool(muscles, maxDiff, contra);

        // 2. 器械过滤（动作器械在用户列表内，或"无"器械永远可用）；候选不足则放宽器械
        List<Action> matched = filterEquipment(pool, equipment);
        if (matched.size() < 4) {
            matched = pool;
            note.add("候选不足，已放宽器械条件");
        }

        // 3. 结构编排（全程去重：同一动作在一门课中只能出现一次，符合 course_action 唯一约束）
        List<Action> main = new ArrayList<>();
        List<Action> assist = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (Action a : matched) {
            if (isCompound(a) && main.size() < 2 && used.add(a.getId())) main.add(a);
        }
        for (Action a : matched) {
            if (!isCompound(a) && main.size() < 2 && used.add(a.getId())) main.add(a);
        }
        for (Action a : matched) {
            if (!isCompound(a) && assist.size() < 2 && used.add(a.getId())) assist.add(a);
        }
        for (Action a : matched) {
            if (assist.size() < 2 && used.add(a.getId())) assist.add(a);
        }

        Action warmup = selectRole(muscles, contra, equipment, "热身", used);
        if (warmup != null) used.add(warmup.getId());
        Action finisher = selectRole(muscles, contra, equipment, "收尾", used);

        // 4. 容量计算
        int[] v = VOLUME.getOrDefault(goal, VOLUME.get(1));
        int mainSets = v[0], mainReps = v[1], mainRest = v[2];
        int assistSets = v[3], assistReps = v[4], assistRest = v[5];

        // 5. 组装 + 时长微调
        AiCourseResult result = buildResult(muscles, goal, main, assist, warmup, finisher,
                mainSets, mainReps, mainRest, assistSets, assistReps, assistRest);

        // 时长落在目标 ±15% 之外则微调辅助组数（2~4 组）
        int estimated = result.getDuration();
        if (estimated > targetDuration * 1.15 && assistSets > 2) {
            result = buildResult(muscles, goal, main, assist, warmup, finisher,
                    mainSets, mainReps, mainRest, 2, assistReps, assistRest);
        } else if (estimated < targetDuration * 0.85 && assistSets < 4) {
            result = buildResult(muscles, goal, main, assist, warmup, finisher,
                    mainSets, mainReps, mainRest, assistSets + 1, assistReps, assistRest);
        }

        if (!note.isEmpty()) {
            result.setNote(String.join("；", note));
        }
        return result;
    }

    private List<Action> queryPool(List<String> muscles, int maxDiff, List<String> contra) {
        LambdaQueryWrapper<Action> qw = new LambdaQueryWrapper<Action>()
                .eq(Action::getStatus, 2)
                .le(Action::getDifficulty, maxDiff);
        if (muscles != null && !muscles.isEmpty() && !muscles.contains("全身")) {
            qw.in(Action::getPart, muscles);
        }
        List<Action> all = actionMapper.selectList(qw);
        List<Action> filtered = new ArrayList<>();
        for (Action a : all) {
            if (!hitContra(a, contra)) filtered.add(a);
        }
        return filtered;
    }

    private List<Action> filterEquipment(List<Action> pool, List<String> equipment) {
        if (equipment == null || equipment.isEmpty()) return new ArrayList<>(pool);
        List<Action> out = new ArrayList<>();
        for (Action a : pool) {
            String eq = a.getEquipment();
            if (eq == null || "无".equals(eq) || equipment.contains(eq)) out.add(a);
        }
        return out;
    }

    private boolean isCompound(Action a) {
        return a.getMechanic() != null && a.getMechanic() == 1;
    }

    private boolean hitContra(Action a, List<String> contra) {
        if (contra.isEmpty() || a.getContraindication() == null || a.getContraindication().isBlank()) return false;
        String c = a.getContraindication();
        return contra.stream().anyMatch(c::contains);
    }

    /** 选热身/收尾动作：低难度、无器械或拉伸类，排除禁忌与已选动作 */
    private Action selectRole(List<String> muscles, List<String> contra, List<String> equipment, String role, Set<Long> used) {
        LambdaQueryWrapper<Action> qw = new LambdaQueryWrapper<Action>()
                .eq(Action::getStatus, 2)
                .le(Action::getDifficulty, 2);
        if ("热身".equals(role)) {
            qw.and(w -> w.eq(Action::getPart, "全身").or().eq(Action::getPart, "核心"));
        } else {
            qw.in(Action::getPart, "核心", "全身");
        }
        List<Action> all = actionMapper.selectList(qw);
        for (Action a : all) {
            if (used.contains(a.getId())) continue;
            if (hitContra(a, contra)) continue;
            String eq = a.getEquipment();
            boolean ok = eq == null || "无".equals(eq) || "拉伸".equals(eq)
                    || (equipment != null && equipment.contains(eq));
            if (ok) return a;
        }
        // 兜底：任取一个低难度、无禁忌、未选用的动作
        for (Action a : all) {
            if (used.contains(a.getId())) continue;
            if (!hitContra(a, contra)) return a;
        }
        return null;
    }

    private AiCourseResult buildResult(List<String> muscles, int goal,
                                       List<Action> main, List<Action> assist,
                                       Action warmup, Action finisher,
                                       int mainSets, int mainReps, int mainRest,
                                       int assistSets, int assistReps, int assistRest) {
        List<AiCourseAction> actions = new ArrayList<>();
        Set<String> parts = new LinkedHashSet<>();
        if (warmup != null) {
            actions.add(toAction(warmup, 1, 12, 30, "热身"));
            parts.add(warmup.getPart());
        }
        for (Action a : main) {
            actions.add(toAction(a, mainSets, mainReps, mainRest, "主项"));
            parts.add(a.getPart());
        }
        for (Action a : assist) {
            actions.add(toAction(a, assistSets, assistReps, assistRest, "辅助"));
            parts.add(a.getPart());
        }
        if (finisher != null) {
            actions.add(toAction(finisher, 2, 12, 30, "收尾"));
            parts.add(finisher.getPart());
        }

        int totalSeconds = 0;
        for (AiCourseAction ac : actions) {
            totalSeconds += ac.getSets() * (ac.getRest() + SET_TIME_SECONDS);
        }
        int durationMin = Math.max(1, (int) Math.round(totalSeconds / 60.0));

        AiCourseResult result = new AiCourseResult();
        result.setName(buildName(muscles, goal));
        result.setDuration(durationMin);
        result.setCalorie(durationMin * 7); // 力量训练约 7 千卡/分钟（粗略）
        result.setActions(actions);
        result.setCoveredParts(new ArrayList<>(parts));
        return result;
    }

    private AiCourseAction toAction(Action a, int sets, int reps, int rest, String role) {
        AiCourseAction ac = new AiCourseAction();
        ac.setActionId(a.getId());
        ac.setName(a.getName());
        ac.setPart(a.getPart());
        ac.setEquipment(a.getEquipment());
        ac.setDifficulty(a.getDifficulty());
        ac.setSets(sets);
        ac.setReps(reps);
        ac.setRest(rest);
        ac.setRole(role);
        return ac;
    }

    private String buildName(List<String> muscles, int goal) {
        String part = (muscles == null || muscles.isEmpty() || muscles.contains("全身")) ? "全身" : String.join("+", muscles);
        String goalName = switch (goal) {
            case 2 -> "减脂";
            case 3 -> "塑形";
            case 4 -> "保持";
            default -> "增肌";
        };
        return goalName + "·" + part + "训练";
    }

    private static int nvl(Integer v, int dft) {
        return v == null ? dft : v;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static List<String> emptyTo(List<String> list, String dft) {
        return (list == null || list.isEmpty()) ? List.of(dft) : list;
    }
}
