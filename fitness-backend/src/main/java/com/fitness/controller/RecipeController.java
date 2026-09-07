package com.fitness.controller;

import com.fitness.common.PageResult;
import com.fitness.common.Result;
import com.fitness.entity.Recipe;
import com.fitness.service.FoodService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 食谱接口
 */
@RestController
@RequestMapping("/api/recipe")
@RequiredArgsConstructor
public class RecipeController {

    private final FoodService foodService;

    @GetMapping
    public Result<PageResult<Recipe>> list(@RequestParam(required = false) Integer goal,
                                           @RequestParam(defaultValue = "1") long page,
                                           @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(foodService.listRecipe(goal, page, pageSize));
    }

    @GetMapping("/{id}")
    public Result<Recipe> detail(@PathVariable Long id) {
        return Result.ok(foodService.recipeDetail(id));
    }
}
