package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.ResultCode;
import com.fitness.entity.Action;
import com.fitness.entity.Course;
import com.fitness.entity.CourseAction;
import com.fitness.entity.CourseComment;
import com.fitness.entity.CourseFavorite;
import com.fitness.entity.CourseLike;
import com.fitness.entity.User;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.CourseActionMapper;
import com.fitness.mapper.CourseCommentMapper;
import com.fitness.mapper.CourseFavoriteMapper;
import com.fitness.mapper.CourseLikeMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.CourseProgressMapper;
import com.fitness.mapper.UserMapper;
import com.fitness.entity.CourseProgress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 课程服务
 */
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseActionMapper courseActionMapper;
    private final CourseFavoriteMapper courseFavoriteMapper;
    private final CourseLikeMapper courseLikeMapper;
    private final CourseCommentMapper courseCommentMapper;
    private final ActionMapper actionMapper;
    private final UserMapper userMapper;
    private final CourseProgressMapper courseProgressMapper;

    public PageResult<Course> list(String category, String part, Integer difficulty,
                                   Integer minDuration, Integer maxDuration,
                                   String keyword, long page, long pageSize) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getStatus, 2);
        if (StringUtils.hasText(category)) {
            wrapper.eq(Course::getCategory, category);
        }
        if (StringUtils.hasText(part)) {
            wrapper.eq(Course::getPart, part);
        }
        if (difficulty != null) {
            wrapper.eq(Course::getDifficulty, difficulty);
        }
        if (minDuration != null) {
            wrapper.ge(Course::getDuration, minDuration);
        }
        if (maxDuration != null) {
            wrapper.le(Course::getDuration, maxDuration);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Course::getName, keyword);
        }
        wrapper.orderByDesc(Course::getId);
        Page<Course> result = courseMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    @Transactional
    public Map<String, Object> detail(Long id, Long currentUserId) {
        Course course = courseMapper.selectById(id);
        if (course == null || course.getStatus() == null || course.getStatus() != 2) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在或已下架");
        }
        // 浏览数 +1
        course.setViewCount(course.getViewCount() == null ? 1 : course.getViewCount() + 1);
        courseMapper.updateById(course);

        // 关联动作列表（含组数/次数）
        List<CourseAction> relations = courseActionMapper.selectList(
                new LambdaQueryWrapper<CourseAction>().eq(CourseAction::getCourseId, id)
                        .orderByAsc(CourseAction::getSort));
        Map<Long, CourseAction> relMap = relations.stream()
                .collect(Collectors.toMap(CourseAction::getActionId, Function.identity(), (a, b) -> a));
        List<Long> actionIds = relations.stream().map(CourseAction::getActionId).toList();
        List<Action> actions = actionIds.isEmpty() ? List.of()
                : actionMapper.selectBatchIds(actionIds);
        Map<Long, Action> actionMap = actions.stream()
                .collect(Collectors.toMap(Action::getId, Function.identity()));
        List<Map<String, Object>> actionVOs = new ArrayList<>();
        for (CourseAction rel : relations) {
            Action a = actionMap.get(rel.getActionId());
            if (a == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.getId());
            m.put("name", a.getName());
            m.put("part", a.getPart());
            m.put("difficulty", a.getDifficulty());
            m.put("equipment", a.getEquipment());
            m.put("mediaType", a.getMediaType());
            m.put("mediaUrl", a.getMediaUrl());
            m.put("steps", a.getSteps());
            m.put("tips", a.getTips());
            m.put("breath", a.getBreath());
            m.put("errors", a.getErrors());
            m.put("sets", rel.getSets());
            m.put("reps", rel.getReps());
            m.put("weight", rel.getWeight());
            actionVOs.add(m);
        }

        boolean favorite = false;
        boolean liked = false;
        if (currentUserId != null) {
            favorite = courseFavoriteMapper.selectCount(new LambdaQueryWrapper<CourseFavorite>()
                    .eq(CourseFavorite::getCourseId, id).eq(CourseFavorite::getUserId, currentUserId)) > 0;
            liked = courseLikeMapper.selectCount(new LambdaQueryWrapper<CourseLike>()
                    .eq(CourseLike::getCourseId, id).eq(CourseLike::getUserId, currentUserId)) > 0;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("course", course);
        data.put("actions", actionVOs);
        data.put("favorite", favorite);
        data.put("liked", liked);
        return data;
    }

    @Transactional
    public boolean favorite(Long courseId, Long userId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        Long cnt = courseFavoriteMapper.selectCount(new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getCourseId, courseId).eq(CourseFavorite::getUserId, userId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_OPERATION, "已收藏该课程");
        }
        CourseFavorite f = new CourseFavorite();
        f.setCourseId(courseId);
        f.setUserId(userId);
        courseFavoriteMapper.insert(f);
        course.setFavoriteCount((course.getFavoriteCount() == null ? 0 : course.getFavoriteCount()) + 1);
        courseMapper.updateById(course);
        return true;
    }

    @Transactional
    public boolean unfavorite(Long courseId, Long userId) {
        courseFavoriteMapper.delete(new LambdaQueryWrapper<CourseFavorite>()
                .eq(CourseFavorite::getCourseId, courseId).eq(CourseFavorite::getUserId, userId));
        Course course = courseMapper.selectById(courseId);
        if (course != null && course.getFavoriteCount() != null && course.getFavoriteCount() > 0) {
            course.setFavoriteCount(course.getFavoriteCount() - 1);
            courseMapper.updateById(course);
        }
        return true;
    }

    @Transactional
    public boolean like(Long courseId, Long userId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        Long cnt = courseLikeMapper.selectCount(new LambdaQueryWrapper<CourseLike>()
                .eq(CourseLike::getCourseId, courseId).eq(CourseLike::getUserId, userId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_OPERATION, "已点赞该课程");
        }
        CourseLike l = new CourseLike();
        l.setCourseId(courseId);
        l.setUserId(userId);
        courseLikeMapper.insert(l);
        course.setLikeCount((course.getLikeCount() == null ? 0 : course.getLikeCount()) + 1);
        courseMapper.updateById(course);
        return true;
    }

    public List<Map<String, Object>> listComments(Long courseId) {
        List<CourseComment> comments = courseCommentMapper.selectList(
                new LambdaQueryWrapper<CourseComment>().eq(CourseComment::getCourseId, courseId)
                        .eq(CourseComment::getStatus, 1)
                        .orderByDesc(CourseComment::getId));
        return comments.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("content", c.getContent());
            m.put("rating", c.getRating());
            m.put("createTime", c.getCreateTime());
            User u = userMapper.selectById(c.getUserId());
            m.put("nickname", u != null ? u.getNickname() : "用户");
            m.put("avatar", u != null ? u.getAvatar() : null);
            return m;
        }).toList();
    }

    public List<Course> listMyFavorites(Long userId) {
        List<CourseFavorite> favorites = courseFavoriteMapper.selectList(
                new LambdaQueryWrapper<CourseFavorite>().eq(CourseFavorite::getUserId, userId)
                        .orderByDesc(CourseFavorite::getId));
        List<Long> courseIds = favorites.stream().map(CourseFavorite::getCourseId).toList();
        if (courseIds.isEmpty()) {
            return List.of();
        }
        return courseMapper.selectBatchIds(courseIds);
    }

    public CourseComment addComment(Long courseId, Long userId, String content, Integer rating) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        CourseComment comment = new CourseComment();
        comment.setCourseId(courseId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setRating(rating);
        comment.setStatus(0); // 待审核
        courseCommentMapper.insert(comment);
        return comment;
    }

    // ==================== 学习进度（REQ-COURSE-008） ====================

    @Transactional
    public Map<String, Object> recordProgress(Long userId, Long courseId, Integer status) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "课程不存在");
        }
        CourseProgress p = courseProgressMapper.selectOne(new LambdaQueryWrapper<CourseProgress>()
                .eq(CourseProgress::getUserId, userId).eq(CourseProgress::getCourseId, courseId));
        if (p == null) {
            p = new CourseProgress();
            p.setUserId(userId);
            p.setCourseId(courseId);
            p.setStatus(status);
            p.setLearnCount(1);
            p.setLastLearnTime(LocalDateTime.now());
            courseProgressMapper.insert(p);
        } else {
            p.setStatus(status);
            p.setLearnCount((p.getLearnCount() == null ? 0 : p.getLearnCount()) + 1);
            p.setLastLearnTime(LocalDateTime.now());
            courseProgressMapper.updateById(p);
        }
        Map<String, Object> m = new HashMap<>();
        m.put("id", p.getId());
        m.put("courseId", p.getCourseId());
        m.put("status", p.getStatus());
        m.put("learnCount", p.getLearnCount());
        return m;
    }

    /** 我的课程进度列表（含课程信息） */
    public List<Map<String, Object>> listMyProgress(Long userId) {
        List<CourseProgress> list = courseProgressMapper.selectList(
                new LambdaQueryWrapper<CourseProgress>().eq(CourseProgress::getUserId, userId)
                        .orderByDesc(CourseProgress::getUpdateTime));
        return list.stream().map(p -> {
            Course c = courseMapper.selectById(p.getCourseId());
            Map<String, Object> m = new HashMap<>();
            m.put("courseId", p.getCourseId());
            m.put("status", p.getStatus());
            m.put("learnCount", p.getLearnCount());
            m.put("lastLearnTime", p.getLastLearnTime());
            if (c != null) {
                m.put("courseName", c.getName());
                m.put("category", c.getCategory());
                m.put("duration", c.getDuration());
            }
            return m;
        }).toList();
    }

    /** 某课程的我的进度 */
    public Map<String, Object> getCourseProgress(Long userId, Long courseId) {
        CourseProgress p = courseProgressMapper.selectOne(new LambdaQueryWrapper<CourseProgress>()
                .eq(CourseProgress::getUserId, userId).eq(CourseProgress::getCourseId, courseId));
        Map<String, Object> m = new HashMap<>();
        if (p == null) {
            m.put("status", 1);
            m.put("learnCount", 0);
        } else {
            m.put("status", p.getStatus());
            m.put("learnCount", p.getLearnCount());
            m.put("lastLearnTime", p.getLastLearnTime());
        }
        return m;
    }

    // ==================== UGC 自定义课程（REQ-COURSE-007） ====================

    @Transactional
    public Course submitUgcCourse(Long userId, String name, String category, String intro,
                                  Integer difficulty, Integer duration, Integer calorie) {
        Course course = new Course();
        course.setName(name);
        course.setCategory(StringUtils.hasText(category) ? category : "其他");
        course.setIntro(intro);
        course.setDifficulty(difficulty == null ? 1 : difficulty);
        course.setDuration(duration == null ? 30 : duration);
        course.setCalorie(calorie == null ? 100 : calorie);
        course.setUserId(userId);
        course.setStatus(1); // 待审核
        course.setViewCount(0);
        course.setFavoriteCount(0);
        course.setLikeCount(0);
        courseMapper.insert(course);
        return course;
    }
}
