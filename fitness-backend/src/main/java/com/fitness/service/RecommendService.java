package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.entity.BodyData;
import com.fitness.entity.Course;
import com.fitness.entity.PlanCourse;
import com.fitness.entity.Recipe;
import com.fitness.entity.RecommendRecord;
import com.fitness.entity.TrainingPlan;
import com.fitness.entity.User;
import com.fitness.mapper.BodyDataMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.PlanCourseMapper;
import com.fitness.mapper.RecipeMapper;
import com.fitness.mapper.RecommendRecordMapper;
import com.fitness.mapper.TrainingPlanMapper;
import com.fitness.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 智能推荐服务（多维可解释规则引擎：画像 → 召回 → 过滤 → 打分 → 重排）
 */
@Service
@RequiredArgsConstructor
public class RecommendService {

    public static final int TYPE_PLAN = 1;
    public static final int TYPE_RECIPE = 2;
    public static final int TYPE_COURSE = 3;

    private final UserMapper userMapper;
    private final TrainingPlanMapper planMapper;
    private final RecipeMapper recipeMapper;
    private final CourseMapper courseMapper;
    private final PlanCourseMapper planCourseMapper;
    private final RecommendRecordMapper recommendRecordMapper;
    private final BodyDataMapper bodyDataMapper;
    private final RecommendProfileService recommendProfileService;

    /**
     * 推荐计划模板（多维打分）
     */
    public List<Map<String, Object>> recommendPlans(Long userId, int limit) {
        User user = userMapper.selectById(userId);
        List<String> preferParts = recommendProfileService.mergedParts(user);
        Set<Long> adopted = recommendProfileService.adoptedIds(userId, TYPE_PLAN);
        Set<Integer> adoptedGoals = adoptedGoals(adopted);
        int frequency = recommendProfileService.trainFrequency(userId);

        List<TrainingPlan> templates = planMapper.selectList(
                new LambdaQueryWrapper<TrainingPlan>().eq(TrainingPlan::getIsTemplate, 1));
        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainingPlan plan : templates) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", plan.getId());
            m.put("name", plan.getName());
            m.put("goal", plan.getGoal());
            m.put("level", plan.getLevel());
            m.put("cycle", plan.getCycle());
            m.put("tags", plan.getTags());
            m.put("cover", plan.getCover());
            ScoreResult sr = scorePlan(user, plan, preferParts, adopted, adoptedGoals, frequency);
            m.put("score", sr.score);
            m.put("reasons", sr.reasons);
            result.add(m);
        }
        result.sort(Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("score")).reversed());
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    /**
     * 推荐食谱（修复 goal 映射 bug + 降级映射 + 热量/营养/历史因子）
     */
    public List<Map<String, Object>> recommendRecipes(Long userId, int limit) {
        User user = userMapper.selectById(userId);
        int mealTarget = mealTargetByGoal(user);
        Set<Long> adoptedRecipes = recommendProfileService.adoptedIds(userId, TYPE_RECIPE);
        Set<Integer> adoptedGoals = adoptedRecipeGoals(adoptedRecipes);

        List<Recipe> recipes = recipeMapper.selectList(
                new LambdaQueryWrapper<Recipe>().eq(Recipe::getStatus, 1));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", recipe.getId());
            m.put("name", recipe.getName());
            m.put("goal", recipe.getGoal());
            m.put("calorie", recipe.getCalorie());
            m.put("tags", recipe.getTags());
            ScoreResult sr = scoreRecipe(user, recipe, mealTarget, adoptedGoals);
            m.put("score", sr.score);
            m.put("reasons", sr.reasons);
            result.add(m);
        }
        result.sort(Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("score")).reversed());
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    /**
     * 推荐课程（新增）
     */
    public List<Map<String, Object>> recommendCourses(Long userId, int limit) {
        User user = userMapper.selectById(userId);
        List<String> preferParts = recommendProfileService.mergedParts(user);
        int avgDuration = recommendProfileService.avgDuration(userId);

        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getStatus, 2));
        int maxHeat = 0;
        for (Course c : courses) {
            maxHeat = Math.max(maxHeat, heat(c));
        }
        final int heatThreshold = maxHeat > 0 ? Math.max(1, (int) (maxHeat * 0.3)) : 0;

        List<Map<String, Object>> result = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", course.getId());
            m.put("name", course.getName());
            m.put("category", course.getCategory());
            m.put("part", course.getPart());
            m.put("difficulty", course.getDifficulty());
            m.put("duration", course.getDuration());
            m.put("cover", course.getCover());
            m.put("viewCount", course.getViewCount());
            m.put("favoriteCount", course.getFavoriteCount());
            ScoreResult sr = scoreCourse(user, course, preferParts, avgDuration, heatThreshold);
            m.put("score", sr.score);
            m.put("reasons", sr.reasons);
            result.add(m);
        }
        result.sort(Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("score")).reversed());
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    /**
     * 行为反馈：按 (userId, type, targetId) 去重更新（不再每次插新记录）
     */
    public void feedback(Long userId, int type, Long targetId, String action) {
        RecommendRecord exist = recommendRecordMapper.selectOne(
                new LambdaQueryWrapper<RecommendRecord>()
                        .eq(RecommendRecord::getUserId, userId)
                        .eq(RecommendRecord::getType, type)
                        .eq(RecommendRecord::getTargetId, targetId)
                        .last("LIMIT 1"));
        boolean click = "click".equals(action);
        boolean adopt = "adopt".equals(action);
        if (exist != null) {
            if (click) {
                exist.setClicked(1);
            }
            if (adopt) {
                exist.setAdopted(1);
            }
            recommendRecordMapper.updateById(exist);
        } else {
            RecommendRecord record = new RecommendRecord();
            record.setUserId(userId);
            record.setType(type);
            record.setTargetId(targetId);
            record.setScore(0);
            record.setClicked(click ? 1 : 0);
            record.setAdopted(adopt ? 1 : 0);
            recommendRecordMapper.insert(record);
        }
    }

    // ==================== 计划打分 ====================

    private ScoreResult scorePlan(User user, TrainingPlan plan, List<String> preferParts,
                                  Set<Long> adopted, Set<Integer> adoptedGoals, int frequency) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        // 目标匹配 +3
        if (user.getGoal() != null && user.getGoal().equals(plan.getGoal())) {
            score += 3;
            reasons.add("目标匹配");
        }
        // 水平匹配 +2
        if (user.getLevel() != null && user.getLevel().equals(plan.getLevel())) {
            score += 2;
            reasons.add("水平匹配");
        }
        // 偏好部位命中 +2（计划所含课程 part ∩ 偏好部位）
        List<String> planParts = planParts(plan.getId());
        if (!preferParts.isEmpty() && !planParts.isEmpty()
                && planParts.stream().anyMatch(preferParts::contains)) {
            score += 2;
            reasons.add("常练部位");
        }
        // 训练频率适配 +1
        if (frequency > 0 && cycleMatched(plan.getCycle(), frequency)) {
            score += 1;
            reasons.add("节奏匹配");
        }
        // 历史偏好 +1（曾采纳同 goal 计划）
        if (adoptedGoals.contains(plan.getGoal())) {
            score += 1;
            reasons.add("你的偏好");
        }
        // 热度 +1（该模板被采纳过）
        if (adoptCount(TYPE_PLAN, plan.getId()) > 0) {
            score += 1;
            reasons.add("热门计划");
        }
        // 已采纳降权 -3
        if (adopted.contains(plan.getId())) {
            score -= 3;
            reasons.add("已套用");
        }
        if (score == 0) {
            reasons.add("热门推荐");
        }
        return new ScoreResult(score, reasons);
    }

    // ==================== 食谱打分 ====================

    private ScoreResult scoreRecipe(User user, Recipe recipe, int mealTarget, Set<Integer> adoptedGoals) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        Integer goal = user.getGoal();
        if (goal != null && recipe.getGoal() != null) {
            if (goal.equals(recipe.getGoal())) {
                score += 3;
                reasons.add("目标匹配");
            } else if ((goal == 3 || goal == 4) && recipe.getGoal() == 2) {
                // 塑形/保持 → 减脂食谱（降级，弱匹配 +1）
                score += 1;
                reasons.add("相近目标");
            }
        }
        // 热量适配 +2（单餐热量贴近每日需求/3，容差 100 千卡）
        if (recipe.getCalorie() != null && mealTarget > 0
                && Math.abs(recipe.getCalorie() - mealTarget) <= 100) {
            score += 2;
            reasons.add("热量贴合");
        }
        // 营养适配 +1（增肌→高蛋白，减脂→低脂）
        if (goal != null && nutritionMatched(goal, recipe)) {
            score += 1;
            reasons.add("营养匹配");
        }
        // 历史偏好 +1（曾采纳同类 goal 食谱）
        if (recipe.getGoal() != null && adoptedGoals.contains(recipe.getGoal())) {
            score += 1;
            reasons.add("你的偏好");
        }
        if (score == 0) {
            reasons.add("热门推荐");
        }
        return new ScoreResult(score, reasons);
    }

    // ==================== 课程打分 ====================

    private ScoreResult scoreCourse(User user, Course course, List<String> preferParts,
                                    int avgDuration, int heatThreshold) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        // 目标-分类匹配 +3
        if (user.getGoal() != null && course.getCategory() != null
                && categoryMatches(user.getGoal(), course.getCategory())) {
            score += 3;
            reasons.add("目标匹配");
        }
        // 难度-水平匹配 +2
        if (user.getLevel() != null && course.getDifficulty() != null
                && user.getLevel().equals(course.getDifficulty())) {
            score += 2;
            reasons.add("水平匹配");
        }
        // 偏好部位命中 +2
        if (!preferParts.isEmpty() && course.getPart() != null && preferParts.contains(course.getPart())) {
            score += 2;
            reasons.add("常练部位");
        }
        // 时长适配 +1
        if (avgDuration > 0 && course.getDuration() != null
                && Math.abs(avgDuration - course.getDuration()) <= 10) {
            score += 1;
            reasons.add("时长贴合");
        }
        // 热度 +1（viewCount+favoriteCount 归一化，Top 30%）
        if (heatThreshold > 0 && heat(course) >= heatThreshold) {
            score += 1;
            reasons.add("热门课程");
        }
        if (score == 0) {
            reasons.add("热门推荐");
        }
        return new ScoreResult(score, reasons);
    }

    // ==================== 辅助 ====================

    /** 计划所含课程部位集合 */
    private List<String> planParts(Long planId) {
        List<PlanCourse> pcs = planCourseMapper.selectList(
                new LambdaQueryWrapper<PlanCourse>().eq(PlanCourse::getPlanId, planId));
        List<Long> courseIds = pcs.stream().map(PlanCourse::getCourseId).toList();
        if (courseIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> parts = new ArrayList<>();
        for (Course c : courseMapper.selectBatchIds(courseIds)) {
            if (c.getPart() != null && !c.getPart().isBlank()) {
                parts.add(c.getPart());
            }
        }
        return parts;
    }

    /** cycle（每周N练）与近30天训练频率是否贴近（容差 4 次/月） */
    private boolean cycleMatched(String cycle, int frequency) {
        if (!StringUtils.hasText(cycle)) {
            return false;
        }
        int n = 0;
        for (char c : cycle.toCharArray()) {
            if (Character.isDigit(c)) {
                n = c - '0';
                break;
            }
        }
        if (n <= 0) {
            return false;
        }
        return Math.abs(frequency - n * 4) <= 4;
    }

    /** 营养适配：增肌→高蛋白(>=40g)，减脂→低脂(<=12g) */
    private boolean nutritionMatched(int goal, Recipe recipe) {
        if (goal == 1) {
            return recipe.getProtein() != null && recipe.getProtein().doubleValue() >= 40;
        }
        if (goal == 2) {
            return recipe.getFat() != null && recipe.getFat().doubleValue() <= 12;
        }
        return false;
    }

    /** 目标 → 课程分类映射 */
    private boolean categoryMatches(int goal, String category) {
        return switch (goal) {
            case 1 -> "增肌".equals(category);
            case 2 -> "减脂".equals(category) || "HIIT".equals(category);
            case 3 -> "塑形".equals(category) || "瑜伽".equals(category);
            case 4 -> "康复拉伸".equals(category) || "瑜伽".equals(category);
            default -> false;
        };
    }

    private int heat(Course c) {
        return (c.getViewCount() == null ? 0 : c.getViewCount())
                + (c.getFavoriteCount() == null ? 0 : c.getFavoriteCount());
    }

    /** 已采纳计划的 goal 集合 */
    private Set<Integer> adoptedGoals(Set<Long> adopted) {
        Set<Integer> goals = new HashSet<>();
        for (Long tid : adopted) {
            TrainingPlan tp = planMapper.selectById(tid);
            if (tp != null && tp.getGoal() != null) {
                goals.add(tp.getGoal());
            }
        }
        return goals;
    }

    /** 已采纳食谱的 goal 集合 */
    private Set<Integer> adoptedRecipeGoals(Set<Long> adopted) {
        Set<Integer> goals = new HashSet<>();
        for (Long id : adopted) {
            Recipe r = recipeMapper.selectById(id);
            if (r != null && r.getGoal() != null) {
                goals.add(r.getGoal());
            }
        }
        return goals;
    }

    /** 某对象被采纳次数 */
    private long adoptCount(int type, Long targetId) {
        Long c = recommendRecordMapper.selectCount(
                new LambdaQueryWrapper<RecommendRecord>()
                        .eq(RecommendRecord::getType, type)
                        .eq(RecommendRecord::getTargetId, targetId)
                        .eq(RecommendRecord::getAdopted, 1));
        return c == null ? 0 : c;
    }

    /**
     * 单餐热量基准（按训练目标，千卡）。
     * 每餐摄入不平均，不做「每日总量 ÷ 餐数」的平均拆分；
     * 直接按目标给合理单餐参考值：增肌偏高、减脂偏低。
     */
    private int mealTargetByGoal(User user) {
        if (user == null || user.getGoal() == null) {
            return 0;
        }
        return switch (user.getGoal()) {
            case 1 -> 600;   // 增肌
            case 2 -> 400;   // 减脂
            case 3 -> 450;   // 塑形
            case 4 -> 500;   // 保持
            default -> 0;
        };
    }

    private static class ScoreResult {
        int score;
        List<String> reasons;

        ScoreResult(int score, List<String> reasons) {
            this.score = score;
            this.reasons = reasons;
        }
    }
}
