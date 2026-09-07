package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fitness.common.BusinessException;
import com.fitness.common.PageResult;
import com.fitness.common.ResultCode;
import com.fitness.entity.Action;
import com.fitness.mapper.ActionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 动作服务
 */
@Service
@RequiredArgsConstructor
public class ActionService {

    private final ActionMapper actionMapper;

    public PageResult<Action> list(String part, Integer difficulty, String equipment, String keyword, long page, long pageSize) {
        LambdaQueryWrapper<Action> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Action::getStatus, 2);
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

    public Action detail(Long id) {
        Action action = actionMapper.selectById(id);
        if (action == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "动作不存在");
        }
        return action;
    }

    /** UGC 自定义动作提交（REQ-COURSE-007），默认待审核 */
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
        action.setStatus(1); // 待审核
        actionMapper.insert(action);
        return action;
    }
}
