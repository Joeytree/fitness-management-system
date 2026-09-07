package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ResultCode;
import com.fitness.dto.PlanCreateRequest;
import com.fitness.entity.Action;
import com.fitness.entity.CheckIn;
import com.fitness.entity.PlanAction;
import com.fitness.entity.TrainingPlan;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.CheckInMapper;
import com.fitness.mapper.PlanActionMapper;
import com.fitness.mapper.TrainingPlanMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 训练计划服务
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final TrainingPlanMapper planMapper;
    private final PlanActionMapper planActionMapper;
    private final ActionMapper actionMapper;
    private final CheckInMapper checkInMapper;

    public List<TrainingPlan> listMine(Long userId) {
        return planMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getUserId, userId)
                .eq(TrainingPlan::getIsTemplate, 0)
                .orderByDesc(TrainingPlan::getId));
    }

    public List<TrainingPlan> listTemplates() {
        return planMapper.selectList(new LambdaQueryWrapper<TrainingPlan>()
                .eq(TrainingPlan::getIsTemplate, 1)
                .orderByAsc(TrainingPlan::getId));
    }

    @Transactional
    public TrainingPlan create(Long userId, PlanCreateRequest req) {
        TrainingPlan plan = new TrainingPlan();
        plan.setUserId(userId);
        plan.setName(req.getName());
        plan.setGoal(req.getGoal());
        plan.setLevel(req.getLevel());
        plan.setCycle(req.getCycle());
        plan.setTags(req.getTags());
        plan.setIsTemplate(0);
        planMapper.insert(plan);

        for (PlanCreateRequest.PlanActionItem item : req.getActions()) {
            PlanAction pa = new PlanAction();
            pa.setPlanId(plan.getId());
            pa.setActionId(item.getActionId());
            pa.setDayNo(item.getDayNo() == null ? 1 : item.getDayNo());
            pa.setSets(item.getSets());
            pa.setReps(item.getReps());
            pa.setWeight(item.getWeight());
            pa.setRest(item.getRest());
            pa.setSort(item.getSort());
            planActionMapper.insert(pa);
        }
        return plan;
    }

    @Transactional
    public TrainingPlan adoptTemplate(Long userId, Long templateId) {
        TrainingPlan template = planMapper.selectById(templateId);
        if (template == null || template.getIsTemplate() == null || template.getIsTemplate() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "模板不存在");
        }
        TrainingPlan copy = new TrainingPlan();
        copy.setUserId(userId);
        copy.setName(template.getName());
        copy.setGoal(template.getGoal());
        copy.setLevel(template.getLevel());
        copy.setCycle(template.getCycle());
        copy.setTags(template.getTags());
        copy.setIsTemplate(0);
        planMapper.insert(copy);

        List<PlanAction> templateActions = planActionMapper.selectList(
                new LambdaQueryWrapper<PlanAction>().eq(PlanAction::getPlanId, templateId)
                        .orderByAsc(PlanAction::getSort));
        for (PlanAction ta : templateActions) {
            PlanAction pa = new PlanAction();
            pa.setPlanId(copy.getId());
            pa.setActionId(ta.getActionId());
            pa.setDayNo(ta.getDayNo() == null ? 1 : ta.getDayNo());
            pa.setSets(ta.getSets());
            pa.setReps(ta.getReps());
            pa.setWeight(ta.getWeight());
            pa.setRest(ta.getRest());
            pa.setSort(ta.getSort());
            planActionMapper.insert(pa);
        }
        return copy;
    }

    public Map<String, Object> detail(Long userId, Long planId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "计划不存在");
        }
        if (plan.getIsTemplate() != null && plan.getIsTemplate() == 0
                && !userId.equals(plan.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权访问他人计划");
        }
        List<PlanAction> planActions = planActionMapper.selectList(
                new LambdaQueryWrapper<PlanAction>().eq(PlanAction::getPlanId, planId)
                        .orderByAsc(PlanAction::getDayNo)
                        .orderByAsc(PlanAction::getSort));
        List<Map<String, Object>> actions = planActions.stream().map(pa -> {
            Action action = actionMapper.selectById(pa.getActionId());
            Map<String, Object> m = new HashMap<>();
            m.put("planActionId", pa.getId());
            m.put("dayNo", pa.getDayNo() == null ? 1 : pa.getDayNo());
            m.put("sets", pa.getSets());
            m.put("reps", pa.getReps());
            m.put("weight", pa.getWeight());
            m.put("rest", pa.getRest());
            m.put("sort", pa.getSort());
            m.put("action", action);
            return m;
        }).toList();
        // 按天分组（天序号升序）
        Map<Integer, List<Map<String, Object>>> grouped = new java.util.LinkedHashMap<>();
        for (Map<String, Object> a : actions) {
            int day = ((Number) a.get("dayNo")).intValue();
            grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(a);
        }
        List<Map<String, Object>> days = new ArrayList<>();
        grouped.forEach((dayNo, list) -> {
            Map<String, Object> d = new HashMap<>();
            d.put("dayNo", dayNo);
            d.put("actions", list);
            days.add(d);
        });
        Map<String, Object> data = new HashMap<>();
        data.put("plan", plan);
        data.put("actions", actions);
        data.put("days", days);
        return data;
    }

    @Transactional
    public TrainingPlan update(Long userId, Long planId, PlanCreateRequest req) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该计划");
        }
        plan.setName(req.getName());
        plan.setGoal(req.getGoal());
        plan.setLevel(req.getLevel());
        plan.setCycle(req.getCycle());
        plan.setTags(req.getTags());
        planMapper.updateById(plan);

        planActionMapper.delete(new LambdaQueryWrapper<PlanAction>().eq(PlanAction::getPlanId, planId));
        for (PlanCreateRequest.PlanActionItem item : req.getActions()) {
            PlanAction pa = new PlanAction();
            pa.setPlanId(planId);
            pa.setActionId(item.getActionId());
            pa.setDayNo(item.getDayNo() == null ? 1 : item.getDayNo());
            pa.setSets(item.getSets());
            pa.setReps(item.getReps());
            pa.setWeight(item.getWeight());
            pa.setRest(item.getRest());
            pa.setSort(item.getSort());
            planActionMapper.insert(pa);
        }
        return plan;
    }

    @Transactional
    public void delete(Long userId, Long planId) {
        TrainingPlan plan = planMapper.selectById(planId);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该计划");
        }
        planMapper.deleteById(planId);
        planActionMapper.delete(new LambdaQueryWrapper<PlanAction>().eq(PlanAction::getPlanId, planId));
    }

    public List<LocalDate> calendar(Long userId, Integer year, Integer month) {
        LocalDate start = LocalDate.of(year == null ? LocalDate.now().getYear() : year,
                month == null ? LocalDate.now().getMonthValue() : month, 1);
        LocalDate end = start.plusMonths(1);
        List<CheckIn> checkIns = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId)
                .ge(CheckIn::getCheckDate, start)
                .lt(CheckIn::getCheckDate, end)
                .orderByAsc(CheckIn::getCheckDate));
        return checkIns.stream().map(CheckIn::getCheckDate).toList();
    }
}
