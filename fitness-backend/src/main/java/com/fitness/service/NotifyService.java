package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.entity.Action;
import com.fitness.entity.Course;
import com.fitness.entity.CourseComment;
import com.fitness.entity.Moment;
import com.fitness.entity.MomentComment;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.CourseCommentMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.MomentCommentMapper;
import com.fitness.mapper.MomentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 审核通知服务：向用户推送 UGC 内容审核结果（REQ-SOCIAL-007）
 */
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final MomentMapper momentMapper;
    private final MomentCommentMapper momentCommentMapper;
    private final CourseCommentMapper courseCommentMapper;
    private final ActionMapper actionMapper;
    private final CourseMapper courseMapper;

    public List<Map<String, Object>> listNotifications(Long userId) {
        List<Map<String, Object>> list = new ArrayList<>();

        // 审核通过：动态
        momentMapper.selectList(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getUserId, userId).eq(Moment::getStatus, 1)
                .orderByDesc(Moment::getId))
                .forEach(m -> list.add(notification("moment", "你的动态已通过审核", m.getContent(),
                        null, m.getCreateTime())));

        // 审核通过：动态评论
        momentCommentMapper.selectList(new LambdaQueryWrapper<MomentComment>()
                .eq(MomentComment::getUserId, userId).eq(MomentComment::getStatus, 1)
                .orderByDesc(MomentComment::getId))
                .forEach(c -> list.add(notification("moment_comment", "你的动态评论已通过审核", c.getContent(),
                        null, c.getCreateTime())));

        // 审核通过：课程评论
        courseCommentMapper.selectList(new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getUserId, userId).eq(CourseComment::getStatus, 1)
                .orderByDesc(CourseComment::getId))
                .forEach(c -> list.add(notification("comment", "你的课程评论已通过审核", c.getContent(),
                        null, c.getCreateTime())));

        // 审核通过：UGC 动作（仅本人提交）
        actionMapper.selectList(new LambdaQueryWrapper<Action>()
                .eq(Action::getUserId, userId).eq(Action::getStatus, 2)
                .orderByDesc(Action::getId))
                .forEach(a -> list.add(notification("action", "你提交的动作已通过审核", a.getName(),
                        null, a.getCreateTime())));

        // 审核通过：UGC 课程（仅本人提交）
        courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getUserId, userId).eq(Course::getStatus, 2)
                .orderByDesc(Course::getId))
                .forEach(c -> list.add(notification("course", "你提交的课程已通过审核", c.getName(),
                        null, c.getCreateTime())));

        // 被驳回的动态
        momentMapper.selectList(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getUserId, userId).eq(Moment::getStatus, 2)
                .orderByDesc(Moment::getId))
                .forEach(m -> list.add(notification("moment", "你的动态被驳回", m.getContent(),
                        m.getRejectReason(), m.getCreateTime())));

        // 被驳回的动态评论
        momentCommentMapper.selectList(new LambdaQueryWrapper<MomentComment>()
                .eq(MomentComment::getUserId, userId).eq(MomentComment::getStatus, 2)
                .orderByDesc(MomentComment::getId))
                .forEach(c -> list.add(notification("moment_comment", "你的动态评论被驳回", c.getContent(),
                        c.getRejectReason(), c.getCreateTime())));

        // 被驳回的课程评论
        courseCommentMapper.selectList(new LambdaQueryWrapper<CourseComment>()
                .eq(CourseComment::getUserId, userId).eq(CourseComment::getStatus, 2)
                .orderByDesc(CourseComment::getId))
                .forEach(c -> list.add(notification("comment", "你的课程评论被驳回", c.getContent(),
                        c.getRejectReason(), c.getCreateTime())));

        // 被驳回的 UGC 动作
        actionMapper.selectList(new LambdaQueryWrapper<Action>()
                .eq(Action::getUserId, userId).eq(Action::getStatus, 3)
                .orderByDesc(Action::getId))
                .forEach(a -> list.add(notification("action", "你提交的动作被驳回", a.getName(),
                        a.getRejectReason(), a.getCreateTime())));

        // 被驳回的 UGC 课程
        courseMapper.selectList(new LambdaQueryWrapper<Course>()
                .eq(Course::getUserId, userId).eq(Course::getStatus, 3)
                .orderByDesc(Course::getId))
                .forEach(c -> list.add(notification("course", "你提交的课程被驳回", c.getName(),
                        null, c.getCreateTime())));

        return list;
    }

    private Map<String, Object> notification(String type, String title, String content, String reason, Object time) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("title", title);
        m.put("content", content);
        m.put("reason", reason);
        m.put("time", time);
        return m;
    }
}
