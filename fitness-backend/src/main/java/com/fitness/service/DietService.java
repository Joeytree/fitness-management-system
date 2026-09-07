package com.fitness.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fitness.common.BusinessException;
import com.fitness.common.ResultCode;
import com.fitness.dto.DietRecordRequest;
import com.fitness.entity.DietRecord;
import com.fitness.entity.Food;
import com.fitness.entity.User;
import com.fitness.mapper.DietRecordMapper;
import com.fitness.mapper.FoodMapper;
import com.fitness.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 饮食记录服务
 */
@Service
@RequiredArgsConstructor
public class DietService {

    private final DietRecordMapper dietRecordMapper;
    private final FoodMapper foodMapper;
    private final UserMapper userMapper;

    public DietRecord add(Long userId, DietRecordRequest req) {
        Food food = foodMapper.selectById(req.getFoodId());
        if (food == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "食物不存在");
        }
        LocalDate date = req.getRecordDate() != null ? req.getRecordDate() : LocalDate.now();
        DietRecord record = new DietRecord();
        record.setUserId(userId);
        record.setMealType(req.getMealType());
        record.setFoodId(req.getFoodId());
        record.setAmount(req.getAmount());
        record.setRecordDate(date);
        dietRecordMapper.insert(record);
        return record;
    }

    public List<Map<String, Object>> listByDate(Long userId, LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        List<DietRecord> records = dietRecordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>().eq(DietRecord::getUserId, userId)
                        .eq(DietRecord::getRecordDate, d)
                        .orderByDesc(DietRecord::getId));
        return records.stream().map(r -> {
            Food food = foodMapper.selectById(r.getFoodId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("mealType", r.getMealType());
            m.put("foodId", r.getFoodId());
            m.put("foodName", food != null ? food.getName() : "未知食物");
            m.put("amount", r.getAmount());
            m.put("recordDate", r.getRecordDate());
            if (food != null) {
                BigDecimal factor = r.getAmount().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
                m.put("calorie", food.getCalorie().multiply(factor).setScale(1, RoundingMode.HALF_UP));
                m.put("protein", food.getProtein().multiply(factor).setScale(1, RoundingMode.HALF_UP));
                m.put("carb", food.getCarb().multiply(factor).setScale(1, RoundingMode.HALF_UP));
                m.put("fat", food.getFat().multiply(factor).setScale(1, RoundingMode.HALF_UP));
            }
            return m;
        }).toList();
    }

    public DietRecord update(Long userId, Long recordId, DietRecordRequest req) {
        DietRecord record = dietRecordMapper.selectById(recordId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该记录");
        }
        record.setMealType(req.getMealType());
        record.setFoodId(req.getFoodId());
        record.setAmount(req.getAmount());
        if (req.getRecordDate() != null) {
            record.setRecordDate(req.getRecordDate());
        }
        dietRecordMapper.updateById(record);
        return record;
    }

    public void delete(Long userId, Long recordId) {
        DietRecord record = dietRecordMapper.selectById(recordId);
        if (record == null || !userId.equals(record.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该记录");
        }
        dietRecordMapper.deleteById(recordId);
    }

    /**
     * 当日营养汇总与目标对比
     */
    public Map<String, Object> summary(Long userId, LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        List<DietRecord> records = dietRecordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>().eq(DietRecord::getUserId, userId)
                        .eq(DietRecord::getRecordDate, d));

        BigDecimal totalCalorie = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carb = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        for (DietRecord r : records) {
            Food food = foodMapper.selectById(r.getFoodId());
            if (food == null) {
                continue;
            }
            BigDecimal factor = r.getAmount().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            totalCalorie = totalCalorie.add(food.getCalorie().multiply(factor));
            protein = protein.add(food.getProtein().multiply(factor));
            carb = carb.add(food.getCarb().multiply(factor));
            fat = fat.add(food.getFat().multiply(factor));
        }

        int targetCalorie = estimateTargetCalorie(userId);
        boolean over = totalCalorie.compareTo(BigDecimal.valueOf(targetCalorie)) > 0;

        Map<String, Object> data = new HashMap<>();
        data.put("totalCalorie", totalCalorie.setScale(0, RoundingMode.HALF_UP));
        data.put("protein", protein.setScale(1, RoundingMode.HALF_UP));
        data.put("carb", carb.setScale(1, RoundingMode.HALF_UP));
        data.put("fat", fat.setScale(1, RoundingMode.HALF_UP));
        data.put("targetCalorie", targetCalorie);
        data.put("over", over);
        data.put("mealType", groupByMeal(records));
        return data;
    }

    private Map<String, BigDecimal> groupByMeal(List<DietRecord> records) {
        // 简单返回各餐次热量（供前端展示）
        Map<String, BigDecimal> map = new HashMap<>();
        for (DietRecord r : records) {
            Food food = foodMapper.selectById(r.getFoodId());
            if (food == null) {
                continue;
            }
            BigDecimal factor = r.getAmount().divide(new BigDecimal(100), 4, RoundingMode.HALF_UP);
            BigDecimal cal = food.getCalorie().multiply(factor);
            String key = "meal" + r.getMealType();
            map.merge(key, cal, BigDecimal::add);
        }
        return map;
    }

    /**
     * 估算每日建议热量（基于目标与身体数据）
     */
    private int estimateTargetCalorie(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return 2000;
        }
        double weight = 65;
        double height = 170;
        int age = 25;
        // 取最近身体数据（简化：用身高，体重默认）
        if (user.getHeight() != null) {
            height = user.getHeight().doubleValue();
        }
        double bmr;
        if (user.getGender() != null && user.getGender() == 2) {
            bmr = 655 + 9.6 * weight + 1.8 * height - 4.7 * age;
        } else {
            bmr = 66 + 13.7 * weight + 5 * height - 6.8 * age;
        }
        double tdee = bmr * 1.375;
        if (user.getGoal() != null) {
            if (user.getGoal() == 1) {
                tdee += 300; // 增肌
            } else if (user.getGoal() == 2) {
                tdee -= 300; // 减脂
            }
        }
        return (int) Math.round(tdee);
    }
}
