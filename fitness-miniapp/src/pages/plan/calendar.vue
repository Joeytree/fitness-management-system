<template>
  <view class="page-pad">
    <!-- 月份切换 -->
    <view class="month-bar">
      <text class="month-btn" @click="prevMonth">‹</text>
      <text class="month-label">{{ year }}年{{ month }}月</text>
      <text class="month-btn" @click="nextMonth">›</text>
    </view>

    <!-- 日历卡片 -->
    <view class="calendar-card">
      <!-- 星期表头 -->
      <view class="week-row">
        <text v-for="d in weeks" :key="d" class="week-cell">{{ d }}</text>
      </view>
      <!-- 日期网格 -->
      <view class="grid">
        <view v-for="(cell, i) in cells" :key="i" class="day-cell">
          <view v-if="cell" class="day" :class="{ checked: isChecked(cell), today: isToday(cell) }">
            <text>{{ cell }}</text>
            <view v-if="isChecked(cell)" class="dot"></view>
          </view>
        </view>
      </view>
    </view>

    <!-- 底部统计 -->
    <view class="summary">
      本月打卡 <text class="b-val">{{ monthCheckCount }}</text> 天 · 连续 <text class="b-val">{{ info.streak || 0 }}</text> 天
      <text v-if="(info.streak || 0) > 0" class="b-fire">🔥</text>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'

export default {
  data() {
    return {
      year: new Date().getFullYear(),
      month: new Date().getMonth() + 1,
      weeks: ['日', '一', '二', '三', '四', '五', '六'],
      cells: [],
      checkDates: [],
      info: {}
    }
  },
  computed: {
    monthCheckCount() {
      const ym = `${this.year}-${String(this.month).padStart(2, '0')}-`
      return (this.checkDates || []).filter(d => d.startsWith(ym)).length
    }
  },
  onLoad() {
    this.buildCalendar()
    this.load()
  },
  methods: {
    buildCalendar() {
      const firstDay = new Date(this.year, this.month - 1, 1).getDay()
      const daysInMonth = new Date(this.year, this.month, 0).getDate()
      const cells = []
      for (let i = 0; i < firstDay; i++) cells.push(null)
      for (let d = 1; d <= daysInMonth; d++) cells.push(d)
      this.cells = cells
    },
    async load() {
      try {
        this.checkDates = await get('/plan/calendar', { year: this.year, month: this.month })
      } catch (e) { /* 忽略 */ }
      try {
        this.info = await get('/health/checkin')
      } catch (e) { /* 忽略 */ }
    },
    isChecked(day) {
      const dateStr = `${this.year}-${String(this.month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
      return (this.checkDates || []).includes(dateStr)
    },
    isToday(day) {
      const t = new Date()
      return t.getFullYear() === this.year && t.getMonth() + 1 === this.month && t.getDate() === day
    },
    prevMonth() {
      this.month--
      if (this.month < 1) { this.month = 12; this.year-- }
      this.buildCalendar()
      this.load()
    },
    nextMonth() {
      this.month++
      if (this.month > 12) { this.month = 1; this.year++ }
      this.buildCalendar()
      this.load()
    }
  }
}
</script>

<style>
.month-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 24rpx 24rpx;
}
.month-label {
  font-size: 36rpx;
  font-weight: 700;
}
.month-btn {
  font-size: 48rpx;
  color: #10b981;
  padding: 0 24rpx;
}
.calendar-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 24rpx 16rpx 32rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.week-row {
  display: flex;
  margin-bottom: 12rpx;
}
.week-cell {
  flex: 1;
  text-align: center;
  font-size: 24rpx;
  color: #9ca3af;
  padding: 12rpx 0;
}
.grid {
  display: flex;
  flex-wrap: wrap;
}
.day-cell {
  width: 14.28%;
  height: 96rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.day {
  width: 80rpx;
  height: 80rpx;
  border-radius: 20rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  position: relative;
}
.day text {
  font-size: 28rpx;
  color: #1f2937;
}
.day.checked {
  background: linear-gradient(135deg, #34d399, #10b981);
}
.day.checked text {
  color: #fff;
  font-weight: 600;
}
.day.today text {
  font-weight: 700;
  color: #10b981;
}
.day.today.checked text {
  color: #fff;
}
.dot {
  width: 8rpx;
  height: 8rpx;
  border-radius: 50%;
  background: #fff;
  margin-top: 4rpx;
}
.summary {
  text-align: center;
  margin-top: 28rpx;
  font-size: 28rpx;
  color: #4b5563;
}
.b-val {
  color: #10b981;
  font-weight: 700;
  font-size: 32rpx;
  margin: 0 4rpx;
}
.b-fire {
  margin-left: 8rpx;
}
</style>