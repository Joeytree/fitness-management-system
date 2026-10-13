package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.ResultCode;
import com.fitness.entity.Action;
import com.fitness.entity.CourseAction;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.CourseActionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 动作服务
 */
@Service
@RequiredArgsConstructor
public class ActionService {

    /** 仅自用（私有，永不公开） */
    public static final int STATUS_PRIVATE = 4;
    /** 待审核 */
    public static final int STATUS_PENDING = 1;
    /** 已发布 */
    public static final int STATUS_PUBLISHED = 2;
    /** 已驳回 */
    public static final int STATUS_REJECTED = 3;

    private final ActionMapper actionMapper;
    private final CourseActionMapper courseActionMapper;

    public PageResult<Action> list(String part, Integer difficulty, String equipment, String keyword,
                                   boolean includeMine, Long currentUserId, long page, long pageSize) {
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        if (includeMine && currentUserId != null) {
            // 组装课程用：系统动作(status=2) + 本人自定义动作(全状态 1/2/3/4)
            wrapper.and(w -> w.eq(Action::getStatus, STATUS_PUBLISHED)
                    .or(o -> o.eq(Action::getUserId, currentUserId)
                            .in(Action::getStatus, STATUS_PENDING, STATUS_PUBLISHED, STATUS_REJECTED, STATUS_PRIVATE)));
        } else {
            wrapper.eq(Action::getStatus, STATUS_PUBLISHED);
        }
        if (StringUtils.hasText(part)) {
            wrapper.eq(Action::getPart, part);
        }
        if (difficulty != null) {
            wrapper.eq(Action::getDifficulty, difficulty);
        }
        if (StringUtils.hasText(equipment)) {
            wrapper.eq(Action::getEquipment, equipment);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Action::getName, keyword);
        }
        wrapper.orderByAsc(Action::getId);
        Page<Action> result = actionMapper.selectPage(new Page<>(page, pageSize), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, pageSize);
    }

    public Action detail(Long id, Long currentUserId) {
        Action action = actionMapper.selectById(id);
        if (action == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动作不存在");
        }
        if (action.getStatus() != null && action.getStatus() == STATUS_PUBLISHED) {
            return action;
        }
        // 非公开内容仅提交者本人可见（不泄露存在性）
        if (currentUserId != null && currentUserId.equals(action.getUserId())) {
            return action;
        }
        throw new BusinessException(ResultCode.NOT_FOUND, "动作不存在或已下架");
    }

    /** UGC 自定义动作提交：visibility=private→4 仅自用 / public→1 申请公开 */
    public Action submitUgc(Long userId, Action req) {
        if (!StringUtils.hasText(req.getName()) || !StringUtils.hasText(req.getSteps())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "动作名称与文字步骤为必填");
        }
        Action action = new Action();
        action.setName(req.getName());
        action.setPart(StringUtils.hasText(req.getPart()) ? req.getPart() : "全身");
        action.setDifficulty(req.getDifficulty() == null ? 1 : req.getDifficulty());
        action.setEquipment(req.getEquipment());
        action.setSteps(req.getSteps());
        action.setTips(req.getTips());
        action.setErrors(req.getErrors());
        action.setBreath(req.getBreath());
        action.setMediaType(req.getMediaType());
        action.setMediaUrl(req.getMediaUrl());
        action.setUserId(userId);
        action.setStatus(visibilityToStatus(req.getVisibility()));
        actionMapper.insert(action);
        return action;
    }

    /** 我的自定义动作（全状态 1/2/3/4） */
    public List<Action> listMine(Long userId) {
        return actionMapper.selectList(new LambdaQueryWrapper<Action>()
                .eq(Action::getUserId, userId)
                .in(Action::getStatus, STATUS_PENDING, STATUS_PUBLISHED, STATUS_REJECTED, STATUS_PRIVATE)
                .orderByDesc(Action::getId));
    }

    /** 编辑自己的自定义动作（驳回后重提：重提公开回 1、改仅自用回 4，清空驳回原因） */
    public Action updateUgc(Long userId, Long id, Action req) {
        Action exist = actionMapper.selectById(id);
        if (exist == null || !userId.equals(exist.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "动作不存在或无权操作");
        }
        if (!StringUtils.hasText(req.getName()) || !StringUtils.hasText(req.getSteps())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "动作名称与文字步骤为必填");
        }
        exist.setName(req.getName());
        exist.setPart(StringUtils.hasText(req.getPart()) ? req.getPart() : "全身");
        exist.setDifficulty(req.getDifficulty() == null ? 1 : req.getDifficulty());
        exist.setEquipment(req.getEquipment());
        exist.setSteps(req.getSteps());
        exist.setTips(req.getTips());
        exist.setErrors(req.getErrors());
        exist.setBreath(req.getBreath());
        exist.setMediaType(req.getMediaType());
        exist.setMediaUrl(req.getMediaUrl());
        exist.setStatus(visibilityToStatus(req.getVisibility()));
        exist.setRejectReason(null);
        actionMapper.updateById(exist);
        return exist;
    }

    /** 删除自己的自定义动作（被课程引用时阻止删除）；引用校验与删除须在同一事务，避免 TOCTOU */
    @Transactional
    public void deleteUgc(Long userId, Long id) {
        Action exist = actionMapper.selectById(id);
        if (exist == null || !userId.equals(exist.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "动作不存在或无权操作");
        }
        Long refCnt = courseActionMapper.selectCount(new LambdaQueryWrapper<CourseAction>()
                .eq(CourseAction::getActionId, id));
        if (refCnt != null && refCnt > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST,
                    "该动作已被 " + refCnt + " 门课程引用，请先从课程编排中移除后再删除");
        }
        actionMapper.deleteById(id);
    }

    private int visibilityToStatus(String visibility) {
        return "private".equals(visibility) ? STATUS_PRIVATE : STATUS_PENDING;
    }
}
