package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.entity.Action;
import com.fitness.entity.RecommendRecord;
import com.fitness.entity.TrainingRecord;
import com.fitness.entity.TrainingRecordSet;
import com.fitness.entity.User;
import com.fitness.mapper.ActionMapper;
import com.fitness.mapper.RecommendRecordMapper;
import com.fitness.mapper.TrainingRecordMapper;
import com.fitness.mapper.TrainingRecordSetMapper;
import com.fitness.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 推荐画像聚合服务：多源融合（静态标签 + 训练行为 + 历史反馈），纯计算不落库。
 */
@Service
@RequiredArgsConstructor
public class RecommendProfileService {

    private final UserMapper userMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final TrainingRecordSetMapper trainingRecordSetMapper;
    private final ActionMapper actionMapper;
    private final RecommendRecordMapper recommendRecordMapper;

    /** 画像概览（供引导卡/画像卡展示） */
    public Map<String, Object> profile(Long userId) {
        User user = userMapper.selectById(userId);
        Map<String, Object> data = new HashMap<>();
        if (user == null) {
            data.put("hasProfile", false);
            return data;
        }
        List<String> inferred = inferParts(userId);
        data.put("goal", user.getGoal());
        data.put("level", user.getLevel());
        data.put("preferParts", user.getPreferParts());
        data.put("inferredParts", inferred);
        data.put("mergedParts", mergedParts(user));
        data.put("trainFrequency", trainFrequency(userId));
        data.put("avgDuration", avgDuration(userId));
        data.put("hasProfile", StringUtils.hasText(user.getPreferParts()));
        return data;
    }

    /** 打分用的偏好部位（静态 ∪ 训练推断），无静态画像时也可用推断兜底 */
    public List<String> mergedParts(User user) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (user != null && StringUtils.hasText(user.getPreferParts())) {
            for (String p : user.getPreferParts().split(",")) {
                if (StringUtils.hasText(p)) {
                    set.add(p.trim());
                }
            }
        }
        if (user != null) {
            set.addAll(inferParts(user.getId()));
        }
        return new ArrayList<>(set);
    }

    /** 近 30 天训练推断偏好部位（set.actionId → action.part 频次，取 Top 2） */
    public List<String> inferParts(Long userId) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<TrainingRecord> records = trainingRecordMapper.selectList(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getUserId, userId)
                        .ge(TrainingRecord::getStartTime, since));
        if (records.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> recordIds = records.stream().map(TrainingRecord::getId).toList();
        List<TrainingRecordSet> sets = trainingRecordSetMapper.selectList(
                new LambdaQueryWrapper<TrainingRecordSet>().in(TrainingRecordSet::getRecordId, recordIds));
        Map<Long, Integer> actionFreq = new HashMap<>();
        for (TrainingRecordSet s : sets) {
            if (s.getActionId() != null) {
                actionFreq.merge(s.getActionId(), 1, Integer::sum);
            }
        }
        if (actionFreq.isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Integer> partFreq = new HashMap<>();
        for (Action a : actionMapper.selectBatchIds(actionFreq.keySet())) {
            if (a.getPart() != null && !a.getPart().isBlank()) {
                partFreq.merge(a.getPart(), actionFreq.get(a.getId()), Integer::sum);
            }
        }
        return partFreq.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue() - e1.getValue())
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** 近 30 天训练次数 */
    public int trainFrequency(Long userId) {
        Long count = trainingRecordMapper.selectCount(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getUserId, userId)
                        .ge(TrainingRecord::getStartTime, LocalDateTime.now().minusDays(30)));
        return count == null ? 0 : count.intValue();
    }

    /** 近 30 天平均训练时长（分钟，忽略 0 时长空会话） */
    public int avgDuration(Long userId) {
        List<TrainingRecord> records = trainingRecordMapper.selectList(
                new LambdaQueryWrapper<TrainingRecord>()
                        .eq(TrainingRecord::getUserId, userId)
                        .ge(TrainingRecord::getStartTime, LocalDateTime.now().minusDays(30)));
        int sum = 0;
        int cnt = 0;
        for (TrainingRecord r : records) {
            if (r.getDuration() != null && r.getDuration() > 0) {
                sum += r.getDuration();
                cnt++;
            }
        }
        return cnt == 0 ? 0 : sum / cnt;
    }

    /** 已采纳对象集合（type: 1=计划 2=食谱 3=课程） */
    public Set<Long> adoptedIds(Long userId, int type) {
        List<RecommendRecord> list = recommendRecordMapper.selectList(
                new LambdaQueryWrapper<RecommendRecord>()
                        .eq(RecommendRecord::getUserId, userId)
                        .eq(RecommendRecord::getType, type)
                        .eq(RecommendRecord::getAdopted, 1));
        return list.stream().map(RecommendRecord::getTargetId).collect(Collectors.toSet());
    }
}
