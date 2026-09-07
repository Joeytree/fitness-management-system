package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.ResultCode;
import com.fitness.entity.Food;
import com.fitness.entity.Recipe;
import com.fitness.mapper.FoodMapper;
import com.fitness.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 食物与食谱服务（查询类）
 */
@Service
@RequiredArgsConstructor
public class FoodService {

    private final FoodMapper foodMapper;
    private final RecipeMapper recipeMapper;

    public PageResult<Food> list(String keyword, String category, long page, long pageSize) {
        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Food::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Food::getName, keyword);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Food::getCategory, category);
        }
        wrapper.orderByAsc(Food::getId);
        Page<Food> result = foodMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    public PageResult<Recipe> listRecipe(Integer goal, long page, long pageSize) {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Recipe::getStatus, 1);
        if (goal != null) {
            wrapper.eq(Recipe::getGoal, goal);
        }
        wrapper.orderByAsc(Recipe::getId);
        Page<Recipe> result = recipeMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    public Recipe recipeDetail(Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "食谱不存在");
        }
        return recipe;
    }
}
