package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ResultCode;
import com.fitness.dto.PlanCreateRequest;
import com.fitness.entity.Action;
import com.fitness.entity.CheckIn;
import com.fitness.entity.Course;
import com.fitness.entity.PlanCourse;
import com.fitness.entity.TrainingPlan;
import com.fitness.entity.TrainingRecord;
import com.fitness.entity.TrainingRecordSet;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.CheckInMapper;
import com.fitness.mapper.CourseMapper;
import com.fitness.mapper.PlanCourseMapper;
import com.fitness.mapper.TrainingPlanMapper;
import com.fitness.mapper.TrainingRecordMapper;
import com.fitness.mapper.TrainingRecordSetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 训练计划服务
 */
@Service
@RequiredArgsConstructor
public class PlanService {

    private final TrainingPlanMapper planMapper;
    private final PlanCourseMapper planCourseMapper;
    private final CourseMapper courseMapper;
    private final ActionMapper actionMapper;
    private final CheckInMapper checkInMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final TrainingRecordSetMapper trainingRecordSetMapper;

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

        for (PlanCreateRequest.CourseItem item : req.getCourses()) {
            PlanCourse pc = new PlanCourse();
            pc.setPlanId(plan.getId());
            pc.setCourseId(item.getCourseId());
            pc.setDayNo(item.getDayNo() == null ? 1 : item.getDayNo());
            pc.setSort(item.getSort() == null ? 0 : item.getSort());
            planCourseMapper.insert(pc);
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
        copy.setCover(template.getCover());
        copy.setIsTemplate(0);
        planMapper.insert(copy);

        List<PlanCourse> templateCourses = planCourseMapper.selectList(
                new LambdaQueryWrapper<PlanCourse>().eq(PlanCourse::getPlanId, templateId)
                        .orderByAsc(PlanCourse::getDayNo)
                        .orderByAsc(PlanCourse::getSort));
        for (PlanCourse tc : templateCourses) {
            PlanCourse pc = new PlanCourse();
            pc.setPlanId(copy.getId());
            pc.setCourseId(tc.getCourseId());
            pc.setDayNo(tc.getDayNo() == null ? 1 : tc.getDayNo());
            pc.setSort(tc.getSort());
            planCourseMapper.insert(pc);
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
        List<PlanCourse> planCourses = planCourseMapper.selectList(
                new LambdaQueryWrapper<PlanCourse>().eq(PlanCourse::getPlanId, planId)
                        .orderByAsc(PlanCourse::getDayNo)
                        .orderByAsc(PlanCourse::getSort));
        List<Map<String, Object>> courses = new ArrayList<>();
        for (PlanCourse pc : planCourses) {
            Course course = courseMapper.selectById(pc.getCourseId());
            Map<String, Object> m = new HashMap<>();
            m.put("planCourseId", pc.getId());
            m.put("dayNo", pc.getDayNo() == null ? 1 : pc.getDayNo());
            m.put("week", pc.getWeek());
            m.put("course", course);
            courses.add(m);
        }
        // 按天分组（天序号升序）
        Map<Integer, List<Map<String, Object>>> grouped = new java.util.LinkedHashMap<>();
        for (Map<String, Object> c : courses) {
            int day = ((Number) c.get("dayNo")).intValue();
            grouped.computeIfAbsent(day, k -> new ArrayList<>()).add(c);
        }
        List<Map<String, Object>> days = new ArrayList<>();
        grouped.forEach((dayNo, list) -> {
            Map<String, Object> d = new HashMap<>();
            d.put("dayNo", dayNo);
            d.put("week", list.get(0).get("week"));
            d.put("courses", list);
            days.add(d);
        });
        Map<String, Object> data = new HashMap<>();
        data.put("plan", plan);
        data.put("courses", courses);
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

        planCourseMapper.delete(new LambdaQueryWrapper<PlanCourse>().eq(PlanCourse::getPlanId, planId));
        for (PlanCreateRequest.CourseItem item : req.getCourses()) {
            PlanCourse pc = new PlanCourse();
            pc.setPlanId(planId);
            pc.setCourseId(item.getCourseId());
            pc.setDayNo(item.getDayNo() == null ? 1 : item.getDayNo());
            pc.setSort(item.getSort() == null ? 0 : item.getSort());
            planCourseMapper.insert(pc);
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
        planCourseMapper.delete(new LambdaQueryWrapper<PlanCourse>().eq(PlanCourse::getPlanId, planId));
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

    /**
     * 训练日历明细：当月每日 消耗 / 时长 / 训练部位 + 月度汇总。
     * 数据源用 training_record（比 check_in 准：一天多次训练不会漏算），
     * 只 start 未 complete 的空会话（duration=0 且 calorie=0）会被跳过。
     */
    public Map<String, Object> calendarDetail(Long userId, Integer year, Integer month) {
        LocalDate monthStart = LocalDate.of(year == null ? LocalDate.now().getYear() : year,
                month == null ? LocalDate.now().getMonthValue() : month, 1);
        LocalDate monthEnd = monthStart.plusMonths(1);

        List<TrainingRecord> records = trainingRecordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                .eq(TrainingRecord::getUserId, userId)
                .ge(TrainingRecord::getStartTime, monthStart.atStartOfDay())
                .lt(TrainingRecord::getStartTime, monthEnd.atStartOfDay())
                .orderByAsc(TrainingRecord::getStartTime));

        // 按日期分组
        Map<LocalDate, List<TrainingRecord>> byDate = new LinkedHashMap<>();
        for (TrainingRecord r : records) {
            if (r.getStartTime() == null) {
                continue;
            }
            byDate.computeIfAbsent(r.getStartTime().toLocalDate(), k -> new ArrayList<>()).add(r);
        }

        List<Map<String, Object>> days = new ArrayList<>();
        int totalDuration = 0;
        int totalCalorie = 0;
        for (Map.Entry<LocalDate, List<TrainingRecord>> entry : byDate.entrySet()) {
            int duration = 0;
            int calorie = 0;
            List<Long> recordIds = new ArrayList<>();
            for (TrainingRecord r : entry.getValue()) {
                if (r.getDuration() != null) {
                    duration += r.getDuration();
                }
                if (r.getCalorie() != null) {
                    calorie += r.getCalorie();
                }
                recordIds.add(r.getId());
            }
            // 跳过只 start 未 complete 的空会话
            if (duration <= 0 && calorie <= 0) {
                continue;
            }

            Map<String, Object> day = new HashMap<>();
            day.put("date", entry.getKey().toString());
            day.put("duration", duration);
            day.put("calorie", calorie);
            day.put("parts", queryParts(recordIds));

            days.add(day);
            totalDuration += duration;
            totalCalorie += calorie;
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("days", days.size());
        summary.put("duration", totalDuration);
        summary.put("calorie", totalCalorie);

        Map<String, Object> data = new HashMap<>();
        data.put("days", days);
        data.put("summary", summary);
        return data;
    }

    /**
     * 由逐组明细反查训练部位（action.part，去重）
     */
    private List<String> queryParts(List<Long> recordIds) {
        Set<String> parts = new LinkedHashSet<>();
        if (recordIds == null || recordIds.isEmpty()) {
            return new ArrayList<>(parts);
        }
        List<TrainingRecordSet> sets = trainingRecordSetMapper.selectList(
                new LambdaQueryWrapper<TrainingRecordSet>().in(TrainingRecordSet::getRecordId, recordIds));
        Set<Long> actionIds = new LinkedHashSet<>();
        for (TrainingRecordSet s : sets) {
            if (s.getActionId() != null) {
                actionIds.add(s.getActionId());
            }
        }
        if (actionIds.isEmpty()) {
            return new ArrayList<>(parts);
        }
        for (Action a : actionMapper.selectBatchIds(actionIds)) {
            if (a.getPart() != null && !a.getPart().isBlank()) {
                parts.add(a.getPart());
            }
        }
        return new ArrayList<>(parts);
    }
}
