package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ResultCode;
import com.fitness.dto.BodyDataRequest;
import com.fitness.entity.BodyData;
import com.fitness.entity.CheckIn;
import com.fitness.entity.User;
import com.fitness.mapper.BodyDataMapper;
import com.fitness.mapper.CheckInMapper;
import com.fitness.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康管理服务
 */
@Service
@RequiredArgsConstructor
public class HealthService {

    private final BodyDataMapper bodyDataMapper;
    private final UserMapper userMapper;
    private final CheckInMapper checkInMapper;

    @Transactional
    public BodyData record(Long userId, BodyDataRequest req) {
        User user = userMapper.selectById(userId);
        BodyData data = new BodyData();
        data.setUserId(userId);
        data.setWeight(req.getWeight());
        data.setBodyFat(req.getBodyFat());
        data.setChest(req.getChest());
        data.setWaist(req.getWaist());
        data.setHip(req.getHip());
        data.setArm(req.getArm());
        data.setMuscle(req.getMuscle());
        data.setWater(req.getWater());
        data.setRecordDate(req.getRecordDate() != null ? req.getRecordDate() : LocalDate.now());
        // 自动计算 BMI
        if (user != null && user.getHeight() != null && user.getHeight().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heightM = user.getHeight().divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
            BigDecimal bmi = req.getWeight().divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
            data.setBmi(bmi);
        }
        bodyDataMapper.insert(data);
        return data;
    }

    public List<BodyData> list(Long userId, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<BodyData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BodyData::getUserId, userId);
        if (start != null) {
            wrapper.ge(BodyData::getRecordDate, start);
        }
        if (end != null) {
            wrapper.le(BodyData::getRecordDate, end);
        }
        wrapper.orderByAsc(BodyData::getRecordDate);
        return bodyDataMapper.selectList(wrapper);
    }

    public List<Map<String, Object>> trend(Long userId, String metric, String range) {
        LocalDate start;
        LocalDate today = LocalDate.now();
        switch (range == null ? "30d" : range) {
            case "7d" -> start = today.minusDays(7);
            case "30d" -> start = today.minusDays(30);
            default -> start = LocalDate.of(2000, 1, 1);
        }
        List<BodyData> list = list(userId, start, today);
        return list.stream().map(b -> {
            Map<String, Object> m = new HashMap<>();
            m.put("date", b.getRecordDate());
            m.put("value", "bodyFat".equals(metric) ? b.getBodyFat() : b.getWeight());
            return m;
        }).toList();
    }

    public Map<String, Object> assessment(Long userId) {
        User user = userMapper.selectById(userId);
        BodyData latest = bodyDataMapper.selectOne(new LambdaQueryWrapper<BodyData>()
                .eq(BodyData::getUserId, userId).orderByDesc(BodyData::getRecordDate).last("LIMIT 1"));
        Map<String, Object> data = new HashMap<>();
        data.put("latest", latest);
        if (latest != null && latest.getBmi() != null) {
            double bmi = latest.getBmi().doubleValue();
            String level;
            String advice;
            if (bmi < 18.5) {
                level = "偏瘦";
                advice = "建议适当增加热量摄入并配合力量训练，促进肌肉增长。";
            } else if (bmi < 24) {
                level = "正常";
                advice = "体重处于正常范围，保持规律训练与均衡饮食即可。";
            } else if (bmi < 28) {
                level = "超重";
                advice = "建议控制热量摄入，增加有氧运动，循序渐进减脂。";
            } else {
                level = "肥胖";
                advice = "建议制定系统减脂计划，必要时咨询专业人士。";
            }
            data.put("level", level);
            data.put("advice", advice);
            data.put("bmi", latest.getBmi());
        }
        return data;
    }

    @Transactional
    public User setGoal(Long userId, BigDecimal targetWeight, BigDecimal targetBodyFat) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        user.setTargetWeight(targetWeight);
        user.setTargetBodyFat(targetBodyFat);
        userMapper.updateById(user);
        return user;
    }

    public Map<String, Object> goalProgress(Long userId) {
        User user = userMapper.selectById(userId);
        BodyData latest = bodyDataMapper.selectOne(new LambdaQueryWrapper<BodyData>()
                .eq(BodyData::getUserId, userId).orderByDesc(BodyData::getRecordDate).last("LIMIT 1"));
        Map<String, Object> data = new HashMap<>();
        data.put("targetWeight", user.getTargetWeight());
        data.put("targetBodyFat", user.getTargetBodyFat());
        data.put("currentWeight", latest != null ? latest.getWeight() : null);
        data.put("currentBodyFat", latest != null ? latest.getBodyFat() : null);
        if (user.getTargetWeight() != null && latest != null && latest.getWeight() != null) {
            BigDecimal diff = latest.getWeight().subtract(user.getTargetWeight());
            data.put("weightDiff", diff);
        }
        return data;
    }

    @Transactional
    public CheckIn checkIn(Long userId, Long planId, Integer duration, Integer calorie) {
        LocalDate today = LocalDate.now();
        Long cnt = checkInMapper.selectCount(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId).eq(CheckIn::getCheckDate, today));
        if (cnt != null && cnt > 0) {
            throw new BusinessException(ResultCode.DUPLICATE_OPERATION, "今日已打卡");
        }
        CheckIn checkIn = new CheckIn();
        checkIn.setUserId(userId);
        checkIn.setPlanId(planId);
        checkIn.setCheckDate(today);
        checkIn.setDuration(duration);
        checkIn.setCalorie(calorie);
        checkInMapper.insert(checkIn);
        return checkIn;
    }

    public Map<String, Object> checkInInfo(Long userId) {
        List<CheckIn> list = checkInMapper.selectList(new LambdaQueryWrapper<CheckIn>()
                .eq(CheckIn::getUserId, userId).orderByDesc(CheckIn::getCheckDate));
        int streak = 0;
        LocalDate cursor = LocalDate.now();
        // 连续打卡天数：从今天（或昨天）往前数
        if (list.isEmpty() || !list.get(0).getCheckDate().equals(cursor)) {
            cursor = cursor.minusDays(1);
        }
        for (CheckIn c : list) {
            if (c.getCheckDate().equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else if (c.getCheckDate().isBefore(cursor)) {
                break;
            }
        }
        int totalDuration = list.stream().mapToInt(c -> c.getDuration() == null ? 0 : c.getDuration()).sum();
        int totalCalorie = list.stream().mapToInt(c -> c.getCalorie() == null ? 0 : c.getCalorie()).sum();
        Map<String, Object> data = new HashMap<>();
        data.put("streak", streak);
        data.put("totalCount", list.size());
        data.put("totalDuration", totalDuration);
        data.put("totalCalorie", totalCalorie);
        data.put("checkDates", list.stream().map(CheckIn::getCheckDate).toList());
        return data;
    }
}
