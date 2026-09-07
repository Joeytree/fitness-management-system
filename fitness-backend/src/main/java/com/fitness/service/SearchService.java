package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.entity.Action;
import com.fitness.entity.Article;
import com.fitness.entity.Course;
import com.fitness.entity.Food;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.ArticleMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.FoodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一搜索服务：聚合动作/课程/食物/文章四类结果
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ActionMapper actionMapper;
    private final CourseMapper courseMapper;
    private final FoodMapper foodMapper;
    private final ArticleMapper articleMapper;

    public Map<String, Object> search(String keyword, int limit) {
        Map<String, Object> data = new HashMap<>();
        if (!StringUtils.hasText(keyword)) {
            data.put("actions", List.of());
            data.put("courses", List.of());
            data.put("foods", List.of());
            data.put("articles", List.of());
            return data;
        }
        String kw = keyword.trim();
        int cap = Math.max(1, Math.min(limit, 50));

        List<Action> actions = actionMapper.selectList(new LambdaQueryWrapper<Action>()
                .eq(Action::getStatus, 2)
                .like(Action::getName, kw)
                .orderByAsc(Action::getId)
                .last("LIMIT " + cap));
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 2)
                .like(Course::getName, kw)
                .orderByAsc(Course::getId)
                .last("LIMIT " + cap));
        List<Food> foods = foodMapper.selectList(new LambdaQueryWrapper<Food>()
                .eq(Food::getStatus, 1)
                .like(Food::getName, kw)
                .orderByAsc(Food::getId)
                .last("LIMIT " + cap));
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .like(Article::getTitle, kw)
                .orderByAsc(Article::getId)
                .last("LIMIT " + cap));

        data.put("actions", actions);
        data.put("courses", courses);
        data.put("foods", foods);
        data.put("articles", articles);
        return data;
    }
}
