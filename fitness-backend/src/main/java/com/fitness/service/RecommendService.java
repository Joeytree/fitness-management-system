package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.entity.Recipe;
import com.fitness.entity.RecommendRecord;
import com.fitness.entity.TrainingPlan;
import com.fitness.entity.User;
import com.fitness.mapper.RecipeMapper;
import com.fitness.mapper.RecommendRecordMapper;
import com.fitness.mapper.TrainingPlanMapper;
import com.fitness.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能推荐服务（规则引擎 + 标签匹配）
 */
@Service
@RequiredArgsConstructor
public class RecommendService {

    private final UserMapper userMapper;
    private final TrainingPlanMapper planMapper;
    private final RecipeMapper recipeMapper;
    private final RecommendRecordMapper recommendRecordMapper;

    /**
     * 推荐计划模板
     */
    public List<Map<String, Object>> recommendPlans(Long userId, int limit) {
        User user = userMapper.selectById(userId);
        List<TrainingPlan> templates = planMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getIsTemplate, 1));
        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainingPlan plan : templates) {
            ScoreResult sr = scorePlan(user, plan);
            Map<String, Object> m = new HashMap<>();
            m.put("id", plan.getId());
            m.put("name", plan.getName());
            m.put("goal", plan.getGoal());
            m.put("level", plan.getLevel());
            m.put("cycle", plan.getCycle());
            m.put("tags", plan.getTags());
            m.put("score", sr.score);
            m.put("reasons", sr.reasons);
            result.add(m);
            saveRecord(userId, 1, plan.getId(), sr.score);
        }
        result.sort(Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("score")).reversed());
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    /**
     * 推荐食谱
     */
    public List<Map<String, Object>> recommendRecipes(Long userId, int limit) {
        User user = userMapper.selectById(userId);
        List<Recipe> recipes = recipeMapper.selectList(new LambdaQueryWrapper<Recipe>()
                .eq(Recipe::getStatus, 1));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            ScoreResult sr = scoreRecipe(user, recipe);
            Map<String, Object> m = new HashMap<>();
            m.put("id", recipe.getId());
            m.put("name", recipe.getName());
            m.put("goal", recipe.getGoal());
            m.put("calorie", recipe.getCalorie());
            m.put("tags", recipe.getTags());
            m.put("score", sr.score);
            m.put("reasons", sr.reasons);
            result.add(m);
            saveRecord(userId, 2, recipe.getId(), sr.score);
        }
        result.sort(Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("score")).reversed());
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    public void feedback(Long userId, int type, Long targetId, String action) {
        RecommendRecord record = new RecommendRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setTargetId(targetId);
        record.setScore(0);
        record.setClicked("click".equals(action) ? 1 : 0);
        record.setAdopted("adopt".equals(action) ? 1 : 0);
        recommendRecordMapper.insert(record);
    }

    private ScoreResult scorePlan(User user, TrainingPlan plan) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (user.getGoal() != null && user.getGoal().equals(plan.getGoal())) {
            score += 3;
            reasons.add("目标匹配");
        }
        if (user.getLevel() != null && user.getLevel().equals(plan.getLevel())) {
            score += 2;
            reasons.add("水平匹配");
        }
        if (StringUtils.hasText(plan.getTags()) && StringUtils.hasText(user.getTags())) {
            for (String t : plan.getTags().split(",")) {
                if (t != null && user.getTags().contains(t) && !"增肌减脂塑形保持新手初级进阶".contains(t)) {
                    score += 1;
                    reasons.add("部位匹配");
                    break;
                }
            }
        }
        if (score == 0) {
            reasons.add("热门推荐");
        }
        return new ScoreResult(score, reasons);
    }

    private ScoreResult scoreRecipe(User user, Recipe recipe) {
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (user.getGoal() != null && recipe.getGoal() != null) {
            boolean match = (user.getGoal() == 1 || user.getGoal() == 3) && recipe.getGoal() == 1
                    || (user.getGoal() == 2 || user.getGoal() == 4) && recipe.getGoal() == 2;
            if (user.getGoal() == 1 && recipe.getGoal() == 1 || user.getGoal() == 2 && recipe.getGoal() == 2) {
                match = true;
                score += 3;
                reasons.add("目标匹配");
            }
        }
        if (score == 0) {
            reasons.add("热门推荐");
        }
        return new ScoreResult(score, reasons);
    }

    private void saveRecord(Long userId, int type, Long targetId, int score) {
        RecommendRecord record = new RecommendRecord();
        record.setUserId(userId);
        record.setType(type);
        record.setTargetId(targetId);
        record.setScore(score);
        recommendRecordMapper.insert(record);
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
