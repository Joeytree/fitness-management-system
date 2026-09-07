package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.ResultCode;
import com.fitness.dto.CourseActionItem;
import com.fitness.dto.CourseAdminRequest;
import com.fitness.entity.Action;
import com.fitness.entity.Article;
import com.fitness.entity.Course;
import com.fitness.entity.CourseAction;
import com.fitness.entity.CourseComment;
import com.fitness.entity.Food;
import com.fitness.entity.Moment;
import com.fitness.entity.MomentComment;
import com.fitness.entity.Recipe;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.ArticleMapper;
import com.fitness.mapper.CourseActionMapper;
import com.fitness.mapper.CourseCommentMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.FoodMapper;
import com.fitness.mapper.MomentCommentMapper;
import com.fitness.mapper.MomentMapper;
import com.fitness.mapper.RecipeMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端内容服务：课程/动作/食物/食谱/文章 CRUD + 审核
 */
@Service
@RequiredArgsConstructor
public class AdminContentService {

    private final CourseMapper courseMapper;
    private final CourseActionMapper courseActionMapper;
    private final ActionMapper actionMapper;
    private final FoodMapper foodMapper;
    private final RecipeMapper recipeMapper;
    private final ArticleMapper articleMapper;
    private final CourseCommentMapper courseCommentMapper;
    private final MomentMapper momentMapper;
    private final MomentCommentMapper momentCommentMapper;
    private final com.fitness.mapper.UserMapper userMapper;

    // ==================== 课程 ====================

    public PageResult<Course> listCourse(Integer status, String keyword, String category, long page, long pageSize) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Course::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Course::getName, keyword);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Course::getCategory, category);
        }
        wrapper.orderByDesc(Course::getId);
        Page<Course> result = courseMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    public Map<String, Object> getCourseDetail(Long id) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        List<CourseAction> relations = courseActionMapper.selectList(
                new LambdaQueryWrapper<CourseAction>().eq(CourseAction::getCourseId, id)
                        .orderByAsc(CourseAction::getSort));
        List<CourseActionItem> actionItems = relations.stream().map(r -> {
            CourseActionItem item = new CourseActionItem();
            item.setActionId(r.getActionId());
            item.setSets(r.getSets());
            item.setReps(r.getReps());
            item.setWeight(r.getWeight());
            return item;
        }).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("course", course);
        data.put("actionItems", actionItems);
        return data;
    }

    @Transactional
    public Course createCourse(CourseAdminRequest req) {
        Course course = new Course();
        applyCourse(course, req);
        if (req.getStatus() == null) {
            course.setStatus(0);
        }
        courseMapper.insert(course);
        saveCourseActions(course.getId(), req.getActionItems(), req.getActionIds());
        return course;
    }

    @Transactional
    public Course updateCourse(Long id, CourseAdminRequest req) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        applyCourse(course, req);
        courseMapper.updateById(course);
        courseActionMapper.delete(new LambdaQueryWrapper<CourseAction>().eq(CourseAction::getCourseId, id));
        saveCourseActions(id, req.getActionItems(), req.getActionIds());
        return course;
    }

    public void updateCourseStatus(Long id, Integer status) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        course.setStatus(status);
        courseMapper.updateById(course);
    }

    @Transactional
    public void deleteCourse(Long id) {
        courseMapper.deleteById(id);
        courseActionMapper.delete(new LambdaQueryWrapper<CourseAction>().eq(CourseAction::getCourseId, id));
    }

    private void applyCourse(Course c, CourseAdminRequest req) {
        c.setName(req.getName());
        c.setCategory(req.getCategory());
        c.setPart(req.getPart());
        c.setCover(req.getCover());
        c.setIntro(req.getIntro());
        c.setDifficulty(req.getDifficulty());
        c.setDuration(req.getDuration());
        c.setCalorie(req.getCalorie());
        if (req.getStatus() != null) {
            c.setStatus(req.getStatus());
        }
    }

    private void saveCourseActions(Long courseId, List<CourseActionItem> actionItems, List<Long> actionIds) {
        int sort = 1;
        // 优先使用明细列表（含组数/次数/重量）
        if (actionItems != null && !actionItems.isEmpty()) {
            for (CourseActionItem item : actionItems) {
                if (item.getActionId() == null) {
                    continue;
                }
                CourseAction ca = new CourseAction();
                ca.setCourseId(courseId);
                ca.setActionId(item.getActionId());
                ca.setSort(sort++);
                ca.setSets(item.getSets());
                ca.setReps(item.getReps());
                ca.setWeight(item.getWeight());
                courseActionMapper.insert(ca);
            }
            return;
        }
        // 兼容旧字段：仅 actionIds（无组数/次数/重量）
        if (actionIds == null) {
            return;
        }
        for (Long actionId : actionIds) {
            CourseAction ca = new CourseAction();
            ca.setCourseId(courseId);
            ca.setActionId(actionId);
            ca.setSort(sort++);
            courseActionMapper.insert(ca);
        }
    }

    // ==================== 动作 ====================

    public PageResult<Action> listAction(String part, Integer status, String keyword, long page, long pageSize) {
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(part)) {
            wrapper.eq(Action::getPart, part);
        }
        if (status != null) {
            wrapper.eq(Action::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Action::getName, keyword);
        }
        wrapper.orderByDesc(Action::getId);
        Page<Action> result = actionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    public Action createAction(Action action) {
        if (action.getStatus() == null) {
            action.setStatus(2);
        }
        actionMapper.insert(action);
        return action;
    }

    public Action getAction(Long id) {
        Action action = actionMapper.selectById(id);
        if (action == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动作不存在");
        }
        return action;
    }

    public Action updateAction(Long id, Action action) {
        Action exist = actionMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动作不存在");
        }
        action.setId(id);
        actionMapper.updateById(action);
        return action;
    }

    public void updateActionStatus(Long id, Integer status) {
        Action action = actionMapper.selectById(id);
        if (action == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动作不存在");
        }
        action.setStatus(status);
        actionMapper.updateById(action);
    }

    // ==================== 食物 ====================

    public PageResult<Food> listFood(String keyword, String category, Integer status, long page, long pageSize) {
        LambdaQueryWrapper<Food> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Food::getName, keyword);
        }
        if (StringUtils.hasText(category)) {
            wrapper.eq(Food::getCategory, category);
        }
        if (status != null) {
            wrapper.eq(Food::getStatus, status);
        }
        wrapper.orderByAsc(Food::getId);
        Page<Food> result = foodMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    public Food createFood(Food food) {
        if (food.getStatus() == null) {
            food.setStatus(1);
        }
        foodMapper.insert(food);
        return food;
    }

    public Food updateFood(Long id, Food food) {
        Food exist = foodMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "食物不存在");
        }
        food.setId(id);
        foodMapper.updateById(food);
        return food;
    }

    public void deleteFood(Long id) {
        Food exist = foodMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "食物不存在");
        }
        foodMapper.deleteById(id);
    }

    /**
     * Excel 批量导入食物，返回成功/失败明细
     */
    public Map<String, Object> importFood(MultipartFile file) {
        int success = 0;
        List<String> errors = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                try {
                    String name = getCell(row, 0);
                    String category = getCell(row, 1);
                    BigDecimal calorie = new BigDecimal(getCell(row, 2));
                    BigDecimal protein = new BigDecimal(getCell(row, 3));
                    BigDecimal carb = new BigDecimal(getCell(row, 4));
                    BigDecimal fat = new BigDecimal(getCell(row, 5));
                    String unit = getCell(row, 6);
                    if (!StringUtils.hasText(name)) {
                        throw new IllegalArgumentException("名称为空");
                    }
                    Food food = new Food();
                    food.setName(name);
                    food.setCategory(StringUtils.hasText(category) ? category : "其他");
                    food.setCalorie(calorie);
                    food.setProtein(protein);
                    food.setCarb(carb);
                    food.setFat(fat);
                    food.setUnit(unit);
                    food.setStatus(1);
                    foodMapper.insert(food);
                    success++;
                } catch (Exception e) {
                    errors.add("第" + (i + 1) + "行: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "Excel 解析失败: " + e.getMessage());
        }
        Map<String, Object> data = new HashMap<>();
        data.put("success", success);
        data.put("failed", errors.size());
        data.put("errors", errors);
        return data;
    }

    private String getCell(Row row, int index) {
        if (row.getCell(index) == null) {
            return "";
        }
        return row.getCell(index).toString().trim();
    }

    // ==================== 食谱 ====================

    public PageResult<Recipe> listRecipe(Integer goal, Integer status, long page, long pageSize) {
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();
        if (goal != null) {
            wrapper.eq(Recipe::getGoal, goal);
        }
        if (status != null) {
            wrapper.eq(Recipe::getStatus, status);
        }
        wrapper.orderByDesc(Recipe::getId);
        Page<Recipe> result = recipeMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    public Recipe createRecipe(Recipe recipe) {
        if (recipe.getStatus() == null) {
            recipe.setStatus(1);
        }
        recipeMapper.insert(recipe);
        return recipe;
    }

    public Recipe updateRecipe(Long id, Recipe recipe) {
        Recipe exist = recipeMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "食谱不存在");
        }
        recipe.setId(id);
        recipeMapper.updateById(recipe);
        return recipe;
    }

    public void deleteRecipe(Long id) {
        recipeMapper.deleteById(id);
    }

    // ==================== 文章 ====================

    public PageResult<Article> listArticle(Integer type, Integer status, String keyword, long page, long pageSize) {
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        if (type != null) {
            wrapper.eq(Article::getType, type);
        }
        if (status != null) {
            wrapper.eq(Article::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Article::getTitle, keyword);
        }
        wrapper.orderByDesc(Article::getId);
        Page<Article> result = articleMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    public Article createArticle(Article article) {
        if (article.getStatus() == null) {
            article.setStatus(1);
        }
        articleMapper.insert(article);
        return article;
    }

    public Article getArticle(Long id) {
        Article article = articleMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        return article;
    }

    public Article updateArticle(Long id, Article article) {
        Article exist = articleMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        article.setId(id);
        articleMapper.updateById(article);
        return article;
    }

    public void deleteArticle(Long id) {
        articleMapper.deleteById(id);
    }

    // ==================== 审核 ====================

    /**
     * 待审核内容列表（status = 待审核），返回统一结构（含提交人昵称）
     */
    public List<Map<String, Object>> listReview(String type, long page, long pageSize) {
        List<Map<String, Object>> result = new ArrayList<>();
        switch (type == null ? "moment" : type) {
            case "action" -> actionMapper.selectList(new LambdaQueryWrapper<Action>()
                    .eq(Action::getStatus, 1).orderByAsc(Action::getId))
                    .forEach(a -> result.add(Map.of("id", a.getId(), "type", "action", "name", a.getName() == null ? "" : a.getName(),
                            "content", a.getSteps() == null ? "" : a.getSteps(),
                            "extra", (a.getPart() == null ? "" : a.getPart()) + " · " + (a.getEquipment() == null ? "徒手" : a.getEquipment()),
                            "createTime", a.getCreateTime() == null ? "" : a.getCreateTime(),
                            "status", 0, "rejectReason", "", "userName", "用户投稿")));
            case "course" -> courseMapper.selectList(new LambdaQueryWrapper<Course>()
                    .eq(Course::getStatus, 1).orderByAsc(Course::getId))
                    .forEach(c -> result.add(Map.of("id", c.getId(), "type", "course", "name", c.getName() == null ? "" : c.getName(),
                            "content", c.getIntro() == null ? "" : c.getIntro(),
                            "extra", c.getCategory() == null ? "" : c.getCategory(),
                            "createTime", c.getCreateTime() == null ? "" : c.getCreateTime(),
                            "status", 0, "rejectReason", "", "userName", "用户投稿")));
            case "comment" -> courseCommentMapper.selectList(new LambdaQueryWrapper<CourseComment>()
                    .eq(CourseComment::getStatus, 0).orderByAsc(CourseComment::getId))
                    .forEach(c -> result.add(buildCommentRow(c.getId(),
                            c.getContent() == null ? "" : c.getContent(), c.getRating(),
                            c.getCourseId(), c.getUserId(), c.getCreateTime())));
            case "moment_comment" -> momentCommentMapper.selectList(new LambdaQueryWrapper<MomentComment>()
                    .eq(MomentComment::getStatus, 0).orderByAsc(MomentComment::getId))
                    .forEach(c -> result.add(Map.of("id", c.getId(), "type", "moment_comment", "name", "动态评论",
                            "content", c.getContent() == null ? "" : c.getContent(),
                            "createTime", c.getCreateTime() == null ? "" : c.getCreateTime(),
                            "status", 0, "rejectReason", "", "userName", userName(c.getUserId()))));
            default -> momentMapper.selectList(new LambdaQueryWrapper<Moment>()
                    .eq(Moment::getStatus, 0).orderByAsc(Moment::getId))
                    .forEach(m -> result.add(Map.of("id", m.getId(), "type", "moment", "name", "动态",
                            "content", m.getContent() == null ? "" : m.getContent(),
                            "images", m.getImages() == null ? "" : m.getImages(),
                            "createTime", m.getCreateTime() == null ? "" : m.getCreateTime(),
                            "status", 0, "rejectReason", "", "userName", userName(m.getUserId()))));
        }
        int from = (int) ((page - 1) * pageSize);
        if (from >= result.size()) {
            return List.of();
        }
        int to = Math.min((int) (from + pageSize), result.size());
        return result.subList(from, to);
    }

    /**
     * 已处理内容列表（已通过 / 已驳回）
     */
    public List<Map<String, Object>> listReviewed(String type) {
        List<Map<String, Object>> result = new ArrayList<>();
        switch (type == null ? "moment" : type) {
            case "action" -> actionMapper.selectList(new LambdaQueryWrapper<Action>()
                    .ne(Action::getStatus, 1).orderByDesc(Action::getId))
                    .forEach(a -> result.add(Map.of("id", a.getId(), "type", "action", "name", a.getName() == null ? "" : a.getName(),
                            "content", a.getSteps() == null ? "" : a.getSteps(),
                            "createTime", a.getCreateTime() == null ? "" : a.getCreateTime(),
                            "status", a.getStatus() == 2 ? 1 : 2,
                            "rejectReason", a.getRejectReason() == null ? "" : a.getRejectReason(),
                            "userName", "用户投稿")));
            case "course" -> courseMapper.selectList(new LambdaQueryWrapper<Course>()
                    .ne(Course::getStatus, 1).orderByDesc(Course::getId))
                    .forEach(c -> result.add(Map.of("id", c.getId(), "type", "course", "name", c.getName() == null ? "" : c.getName(),
                            "content", c.getIntro() == null ? "" : c.getIntro(),
                            "createTime", c.getCreateTime() == null ? "" : c.getCreateTime(),
                            "status", c.getStatus() == 2 ? 1 : 2,
                            "rejectReason", "", "userName", "用户投稿")));
            case "comment" -> courseCommentMapper.selectList(new LambdaQueryWrapper<CourseComment>()
                    .ne(CourseComment::getStatus, 0).orderByDesc(CourseComment::getId))
                    .forEach(c -> result.add(Map.of("id", c.getId(), "type", "comment", "name", "课程评论",
                            "content", c.getContent() == null ? "" : c.getContent(),
                            "createTime", c.getCreateTime() == null ? "" : c.getCreateTime(),
                            "status", c.getStatus(),
                            "rejectReason", c.getRejectReason() == null ? "" : c.getRejectReason(),
                            "userName", userName(c.getUserId()))));
            case "moment_comment" -> momentCommentMapper.selectList(new LambdaQueryWrapper<MomentComment>()
                    .ne(MomentComment::getStatus, 0).orderByDesc(MomentComment::getId))
                    .forEach(c -> result.add(Map.of("id", c.getId(), "type", "moment_comment", "name", "动态评论",
                            "content", c.getContent() == null ? "" : c.getContent(),
                            "createTime", c.getCreateTime() == null ? "" : c.getCreateTime(),
                            "status", c.getStatus(),
                            "rejectReason", c.getRejectReason() == null ? "" : c.getRejectReason(),
                            "userName", userName(c.getUserId()))));
            default -> momentMapper.selectList(new LambdaQueryWrapper<Moment>()
                    .ne(Moment::getStatus, 0).orderByDesc(Moment::getId))
                    .forEach(m -> result.add(Map.of("id", m.getId(), "type", "moment", "name", "动态",
                            "content", m.getContent() == null ? "" : m.getContent(),
                            "createTime", m.getCreateTime() == null ? "" : m.getCreateTime(),
                            "status", m.getStatus(),
                            "rejectReason", m.getRejectReason() == null ? "" : m.getRejectReason(),
                            "userName", userName(m.getUserId()))));
        }
        return result;
    }

    private Map<String, Object> buildCommentRow(Long id, String content, Integer rating, Long courseId, Long userId, java.time.LocalDateTime createTime) {
        Course course = courseId != null ? courseMapper.selectById(courseId) : null;
        String courseName = course != null ? course.getName() : ("课程#" + courseId);
        return Map.of("id", id, "type", "comment", "name", "课程评论", "content", content,
                "extra", courseName + (rating != null && rating > 0 ? " · 评分 " + rating + "★" : ""),
                "createTime", createTime == null ? "" : createTime,
                "status", 0, "rejectReason", "", "userName", userName(userId));
    }

    private String userName(Long userId) {
        if (userId == null) {
            return "用户";
        }
        com.fitness.entity.User u = userMapper.selectById(userId);
        return u != null ? u.getNickname() : ("用户" + userId);
    }

    public void approve(String type, Long id) {
        switch (type) {
            case "action" -> updateActionStatus(id, 2);
            case "course" -> updateCourseStatus(id, 2);
            case "comment" -> {
                CourseComment c = courseCommentMapper.selectById(id);
                if (c == null) throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
                c.setStatus(1);
                courseCommentMapper.updateById(c);
            }
            case "moment_comment" -> {
                MomentComment c = momentCommentMapper.selectById(id);
                if (c == null) throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
                c.setStatus(1);
                momentCommentMapper.updateById(c);
            }
            default -> {
                Moment m = momentMapper.selectById(id);
                if (m == null) throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
                m.setStatus(1);
                momentMapper.updateById(m);
            }
        }
    }

    public void reject(String type, Long id, String reason) {
        switch (type) {
            case "action" -> {
                Action a = actionMapper.selectById(id);
                if (a == null) throw new BusinessException(ResultCode.NOT_FOUND, "动作不存在");
                a.setStatus(3);
                a.setRejectReason(reason);
                actionMapper.updateById(a);
            }
            case "course" -> updateCourseStatus(id, 3);
            case "comment" -> {
                CourseComment c = courseCommentMapper.selectById(id);
                if (c == null) throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
                c.setStatus(2);
                c.setRejectReason(reason);
                courseCommentMapper.updateById(c);
            }
            case "moment_comment" -> {
                MomentComment c = momentCommentMapper.selectById(id);
                if (c == null) throw new BusinessException(ResultCode.NOT_FOUND, "评论不存在");
                c.setStatus(2);
                c.setRejectReason(reason);
                momentCommentMapper.updateById(c);
            }
            default -> {
                Moment m = momentMapper.selectById(id);
                if (m == null) throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
                m.setStatus(2);
                m.setRejectReason(reason);
                momentMapper.updateById(m);
            }
        }
    }
}
