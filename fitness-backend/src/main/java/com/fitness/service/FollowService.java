package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
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
 * 关注服务
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;
    private final MomentMapper momentMapper;
    private final MomentLikeMapper momentLikeMapper;
    private final MomentCommentMapper momentCommentMapper;

    @Transactional
    public void follow(Long userId, Long followUserId) {
        if (userId.equals(followUserId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能关注自己");
        }
        if (userMapper.selectById(followUserId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        Long cnt = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, followUserId));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_OPERATION, "已关注该用户");
        }
        Follow follow = new Follow();
        follow.setUserId(userId);
        follow.setFollowUserId(followUserId);
        followMapper.insert(follow);
    }

    @Transactional
    public void unfollow(Long userId, Long followUserId) {
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId).eq(Follow::getFollowUserId, followUserId));
    }

    public List<Map<String, Object>> listFollowing(Long userId, Long currentUserId) {
        List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId).orderByDesc(Follow::getId));
        return follows.stream().map(f -> toUserMap(f.getFollowUserId(), currentUserId)).toList();
    }

    public List<Map<String, Object>> listFollowers(Long userId, Long currentUserId) {
        List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowUserId, userId).orderByDesc(Follow::getId));
        return follows.stream().map(f -> toUserMap(f.getUserId(), currentUserId)).toList();
    }

    private Map<String, Object> toUserMap(Long targetId, Long currentUserId) {
        User u = userMapper.selectById(targetId);
        Map<String, Object> m = new HashMap<>();
        if (u == null) {
            return m;
        }
        m.put("id", u.getId());
        m.put("nickname", u.getNickname());
        m.put("avatar", u.getAvatar());
        m.put("intro", "");
        // 当前用户是否已关注
        boolean followed = currentUserId != null && followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, currentUserId)
                        .eq(Follow::getFollowUserId, targetId)) > 0;
        m.put("followed", followed);
        return m;
    }

    public Map<String, Object> userProfile(Long userId, Long currentUserId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        long following = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, userId));
        long followers = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowUserId, userId));
        boolean followed = currentUserId != null && followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, currentUserId)
                        .eq(Follow::getFollowUserId, userId)) > 0;
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("nickname", u.getNickname());
        m.put("avatar", u.getAvatar());
        m.put("goal", u.getGoal());
        m.put("level", u.getLevel());
        m.put("followingCount", following);
        m.put("followerCount", followers);
        m.put("followed", followed);
        // 该用户的已通过动态（个人主页动态列表）
        List<Moment> moments = momentMapper.selectList(new LambdaQueryWrapper<Moment>()
                .eq(Moment::getUserId, userId).eq(Moment::getStatus, 1)
                .orderByDesc(Moment::getId).last("LIMIT 20"));
        m.put("moments", moments.stream().map(mo -> {
            Map<String, Object> mm = new HashMap<>();
            mm.put("id", mo.getId());
            mm.put("content", mo.getContent());
            mm.put("images", mo.getImages());
            mm.put("createTime", mo.getCreateTime());
            long likeCount = momentLikeMapper.selectCount(new LambdaQueryWrapper<MomentLike>()
                    .eq(MomentLike::getMomentId, mo.getId()));
            long commentCount = momentCommentMapper.selectCount(new LambdaQueryWrapper<MomentComment>()
                    .eq(MomentComment::getMomentId, mo.getId()).eq(MomentComment::getStatus, 1));
            mm.put("likeCount", likeCount);
            mm.put("commentCount", commentCount);
            return mm;
        }).toList());
        return m;
    }
}
