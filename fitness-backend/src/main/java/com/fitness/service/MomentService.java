package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.ResultCode;
import com.fitness.entity.Follow;
import com.fitness.entity.Moment;
import com.fitness.entity.MomentComment;
import com.fitness.entity.MomentLike;
import com.fitness.entity.User;
import com.fitness.mapper.FollowMapper;
import com.fitness.mapper.MomentCommentMapper;
import com.fitness.mapper.MomentLikeMapper;
import com.fitness.mapper.MomentMapper;
import com.fitness.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态服务
 */
@Service
@RequiredArgsConstructor
public class MomentService {

    private final MomentMapper momentMapper;
    private final MomentLikeMapper momentLikeMapper;
    private final MomentCommentMapper momentCommentMapper;
    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    @Transactional
    public Moment publish(Long userId, String content, String images) {
        Moment moment = new Moment();
        moment.setUserId(userId);
        moment.setContent(content);
        moment.setImages(images);
        moment.setStatus(0); // 待审核
        momentMapper.insert(moment);
        return moment;
    }

    public PageResult<Map<String, Object>> feed(Long currentUserId, String tab, long page, long pageSize) {
        LambdaQueryWrapper<Moment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Moment::getStatus, 1);
        if ("follow".equals(tab)) {
            List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                    .eq(Follow::getUserId, currentUserId));
            List<Long> userIds = follows.stream().map(Follow::getFollowUserId).toList();
            userIds = userIds.isEmpty() ? List.of(-1L) : userIds;
            wrapper.in(Moment::getUserId, userIds);
        }
        wrapper.orderByDesc(Moment::getId);
        Page<Moment> result = momentMapper.selectPage(new Page<>(page, pageSize), wrapper);
        List<Map<String, Object>> list = result.getRecords().stream()
                .map(m -> toMap(m, currentUserId)).toList();
        return PageResult.of(list, result.getTotal(), page, pageSize);
    }

    public Map<String, Object> detail(Long currentUserId, Long id) {
        Moment moment = momentMapper.selectById(id);
        if (moment == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
        }
        if (moment.getStatus() == null || moment.getStatus() != 1) {
            if (!moment.getUserId().equals(currentUserId)) {
                throw new BusinessException(ResultCode.CONTENT_PENDING, "内容待审核，暂不可见");
            }
        }
        Map<String, Object> m = toMap(moment, currentUserId);
        m.put("comments", listComments(id, currentUserId));
        return m;
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Moment moment = momentMapper.selectById(id);
        if (moment == null || !moment.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权删除该动态");
        }
        momentMapper.deleteById(id);
        momentLikeMapper.delete(new LambdaQueryWrapper<MomentLike>().eq(MomentLike::getMomentId, id));
        momentCommentMapper.delete(new LambdaQueryWrapper<MomentComment>().eq(MomentComment::getMomentId, id));
    }

    @Transactional
    public void like(Long userId, Long momentId) {
        if (momentMapper.selectById(momentId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
        }
        Long cnt = momentLikeMapper.selectCount(new LambdaQueryWrapper<MomentLike>()
                .eq(MomentLike::getMomentId, momentId).eq(MomentLike::getUserId, userId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_OPERATION, "已点赞");
        }
        MomentLike like = new MomentLike();
        like.setMomentId(momentId);
        like.setUserId(userId);
        momentLikeMapper.insert(like);
    }

    @Transactional
    public void unlike(Long userId, Long momentId) {
        momentLikeMapper.delete(new LambdaQueryWrapper<MomentLike>()
                .eq(MomentLike::getMomentId, momentId).eq(MomentLike::getUserId, userId));
    }

    public List<Map<String, Object>> listComments(Long momentId, Long currentUserId) {
        List<MomentComment> comments = momentCommentMapper.selectList(
                new LambdaQueryWrapper<MomentComment>().eq(MomentComment::getMomentId, momentId)
                        .eq(MomentComment::getStatus, 1).orderByAsc(MomentComment::getId));
        return comments.stream().map(c -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getId());
            m.put("content", c.getContent());
            m.put("replyTo", c.getReplyTo());
            m.put("createTime", c.getCreateTime());
            User u = userMapper.selectById(c.getUserId());
            m.put("nickname", u != null ? u.getNickname() : "用户");
            m.put("avatar", u != null ? u.getAvatar() : null);
            // 回复目标的昵称
            if (c.getReplyTo() != null) {
                MomentComment parent = momentCommentMapper.selectById(c.getReplyTo());
                if (parent != null) {
                    User pu = userMapper.selectById(parent.getUserId());
                    m.put("replyNickname", pu != null ? pu.getNickname() : "用户");
                }
            }
            return m;
        }).toList();
    }

    public MomentComment addComment(Long userId, Long momentId, String content, Long replyTo) {
        if (momentMapper.selectById(momentId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动态不存在");
        }
        MomentComment comment = new MomentComment();
        comment.setMomentId(momentId);
        comment.setUserId(userId);
        comment.setContent(content);
        comment.setReplyTo(replyTo);
        comment.setStatus(0); // 待审核
        momentCommentMapper.insert(comment);
        return comment;
    }

    private Map<String, Object> toMap(Moment m, Long currentUserId) {
        User u = userMapper.selectById(m.getUserId());
        long likeCount = momentLikeMapper.selectCount(new LambdaQueryWrapper<MomentLike>()
                .eq(MomentLike::getMomentId, m.getId()));
        long commentCount = momentCommentMapper.selectCount(new LambdaQueryWrapper<MomentComment>()
                .eq(MomentComment::getMomentId, m.getId()).eq(MomentComment::getStatus, 1));
        boolean liked = currentUserId != null && momentLikeMapper.selectCount(
                new LambdaQueryWrapper<MomentLike>().eq(MomentLike::getMomentId, m.getId())
                        .eq(MomentLike::getUserId, currentUserId)) > 0;
        Map<String, Object> map = new HashMap<>();
        map.put("id", m.getId());
        map.put("content", m.getContent());
        map.put("images", m.getImages());
        map.put("createTime", m.getCreateTime());
        map.put("likeCount", likeCount);
        map.put("commentCount", commentCount);
        map.put("liked", liked);
        if (u != null) {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", u.getId());
            userMap.put("nickname", u.getNickname());
            userMap.put("avatar", u.getAvatar());
            map.put("user", userMap);
            map.put("nickname", u.getNickname());
            map.put("userId", u.getId());
        } else {
            map.put("user", null);
        }
        return map;
    }
}
