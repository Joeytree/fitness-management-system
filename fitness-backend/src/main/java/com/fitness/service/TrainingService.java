package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ResultCode;
import com.fitness.dto.TrainingCompleteRequest;
import com.fitness.entity.Action;
import com.fitness.entity.CheckIn;
import com.fitness.entity.TrainingRecord;
import com.fitness.entity.TrainingRecordSet;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.CheckInMapper;
import com.fitness.mapper.TrainingRecordMapper;
import com.fitness.mapper.TrainingRecordSetMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 训练执行服务：训练会话 + 逐组明细 + 自动打卡
 *
 * 对应问题 ISS-002：打卡由真实训练产生（training_record 单次训练 +
 * training_record_set 逐组记录），完成后自动生成打卡，保留手动补打卡。
 */
@Service
@RequiredArgsConstructor
public class TrainingService {

    private final TrainingRecordMapper recordMapper;
    private final TrainingRecordSetMapper setMapper;
    private final CheckInMapper checkInMapper;
    private final ActionMapper actionMapper;

    /**
     * 开始训练：创建一条训练会话记录，返回 recordId 供后续完成时回填。
     */
    @Transactional
    public TrainingRecord start(Long userId, Long planId) {
        TrainingRecord r = new TrainingRecord();
        r.setUserId(userId);
        r.setPlanId(planId);
        r.setStartTime(LocalDateTime.now());
        r.setDuration(0);
        r.setCalorie(0);
        r.setCompletedCount(0);
        recordMapper.insert(r);
        return r;
    }

    /**
     * 完成训练：落逐组明细 → 汇总时长/消耗/完成动作数 → 自动打卡。
     * 若未先 start（recordId 为空）则自动创建一条会话。
     */
    @Transactional
    public Map<String, Object> complete(Long userId, TrainingCompleteRequest req) {
        TrainingRecord r;
        if (req.getRecordId() != null) {
            r = recordMapper.selectById(req.getRecordId());
            if (r == null || !userId.equals(r.getUserId())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "训练记录不存在或无权操作");
            }
        } else {
            r = new TrainingRecord();
            r.setUserId(userId);
            r.setStartTime(LocalDateTime.now().minusMinutes(1));
            recordMapper.insert(r);
        }
        r.setEndTime(LocalDateTime.now());

        // 逐组明细
        int completedSets = 0;
        Set<Long> completedActions = new HashSet<>();
        if (req.getSets() != null) {
            for (TrainingCompleteRequest.SetItem item : req.getSets()) {
                TrainingRecordSet s = new TrainingRecordSet();
                s.setRecordId(r.getId());
                s.setActionId(item.getActionId());
                s.setSetNo(item.getSetNo() == null ? 1 : item.getSetNo());
                s.setWeight(item.getWeight());
                s.setReps(item.getReps());
                s.setDone(item.getDone() == null ? 1 : item.getDone());
                setMapper.insert(s);
                if (s.getDone() != null && s.getDone() == 1) {
                    completedSets++;
                    if (item.getActionId() != null) {
                        completedActions.add(item.getActionId());
                    }
                }
            }
        }

        // 时长（分钟）：优先用请求值，否则按起止时间推算
        int duration = req.getDuration() != null && req.getDuration() > 0
                ? req.getDuration()
                : (int) Duration.between(r.getStartTime(), r.getEndTime()).toMinutes();
        if (duration < 1) {
            duration = 1;
        }
        r.setDuration(duration);
        r.setCompletedCount(completedActions.size());

        // 估算消耗（千卡）：中等强度力量训练约 6 kcal/分钟 + 每完成一组约 2 kcal
        int calorie = (int) Math.round(duration * 6.0 + completedSets * 2.0);
        r.setCalorie(calorie);
        recordMapper.updateById(r);

        // 自动打卡：当天已打卡则跳过，不阻断训练完成
        CheckIn checkIn = autoCheckIn(userId, r.getPlanId(), duration, calorie);

        Map<String, Object> data = new HashMap<>();
        data.put("record", r);
        data.put("checkIn", checkIn);
        return data;
    }

    /**
     * 训练历史（含逐组明细）
     */
    public List<Map<String, Object>> list(Long userId) {
        List<TrainingRecord> records = recordMapper.selectList(new LambdaQueryWrapper<TrainingRecord>()
                .eq(TrainingRecord::getUserId, userId)
                .orderByDesc(TrainingRecord::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (TrainingRecord r : records) {
            Map<String, Object> m = new HashMap<>();
            m.put("record", r);
            m.put("sets", setMapper.selectList(new LambdaQueryWrapper<TrainingRecordSet>()
                    .eq(TrainingRecordSet::getRecordId, r.getId())
                    .orderByAsc(TrainingRecordSet::getSetNo)));
            result.add(m);
        }
        return result;
    }

    /**
     * 单条训练详情（含逐组明细与动作名）
     */
    public Map<String, Object> detail(Long userId, Long recordId) {
        TrainingRecord r = recordMapper.selectById(recordId);
        if (r == null || !userId.equals(r.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "训练记录不存在或无权访问");
        }
        List<TrainingRecordSet> sets = setMapper.selectList(new LambdaQueryWrapper<TrainingRecordSet>()
                .eq(TrainingRecordSet::getRecordId, recordId)
                .orderByAsc(TrainingRecordSet::getSetNo));

        // 批量取动作名
        Set<Long> actionIds = new HashSet<>();
        sets.forEach(s -> {
            if (s.getActionId() != null) {
                actionIds.add(s.getActionId());
            }
        });
        Map<Long, String> nameMap = new HashMap<>();
        if (!actionIds.isEmpty()) {
            for (Action a : actionMapper.selectBatchIds(actionIds)) {
                nameMap.put(a.getId(), a.getName());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("record", r);
        data.put("sets", sets.stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", s.getId());
            m.put("actionId", s.getActionId());
            m.put("actionName", s.getActionId() != null ? nameMap.get(s.getActionId()) : null);
            m.put("setNo", s.getSetNo());
            m.put("weight", s.getWeight());
            m.put("reps", s.getReps());
            m.put("done", s.getDone());
            return m;
        }).toList());
        return data;
    }

    private CheckIn autoCheckIn(Long userId, Long planId, int duration, int calorie) {
        LocalDate today = LocalDate.now();
        Long cnt = checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId).eq(CheckIn::getCheckDate, today));
        if (cnt != null && cnt > 0) {
            return null; // 今日已打卡，跳过
        }
        CheckIn c = new CheckIn();
        c.setUserId(userId);
        c.setPlanId(planId);
        c.setCheckDate(today);
        c.setDuration(duration);
        c.setCalorie(calorie);
        checkInMapper.insert(c);
        return c;
    }
}
