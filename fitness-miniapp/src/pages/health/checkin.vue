<template>
  <view class="page-pad">
    <!-- 统计 -->
    <view class="stats-row">
      <view class="stat-card">
        <view class="s-val">{{ info.streak || 0 }}</view>
        <view class="s-label muted">连续天数</view>
      </view>
      <view class="stat-card">
        <view class="s-val">{{ info.totalCount || 0 }}</view>
        <view class="s-label muted">累计次数</view>
      </view>
      <view class="stat-card">
        <view class="s-val">{{ info.totalDuration || 0 }}</view>
        <view class="s-label muted">总时长(分)</view>
      </view>
    </view>

    <!-- 月历 -->
    <view class="calendar">
      <view class="month-bar">
        <text class="month-btn" @click="prevMonth">‹</text>
        <text class="month-label">{{ year }}年{{ month }}月</text>
        <text class="month-btn" @click="nextMonth">›</text>
      </view>
      <view class="week-row">
        <text v-for="d in weeks" :key="d" class="week-cell">{{ d }}</text>
      </view>
      <view class="grid">
        <view v-for="(cell, i) in cells" :key="i" class="day-cell">
          <text v-if="cell" class="day" :class="{ checked: isChecked(cell) }">{{ cell }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'

export default {
  data() {
    return {
      info: {},
      year: new Date().getFullYear(),
      month: new Date().getMonth() + 1,
      weeks: ['日', '一', '二', '三', '四', '五', '六'],
      cells: [],
      checkDates: []
    }
  },
  onShow() {
    this.load()
    this.buildCalendar()
  },
  methods: {
    async load() {
      try {
        this.info = await get('/health/checkin')
        this.checkDates = this.info.checkDates || []
      } catch (e) { /* 忽略 */ }
    },
    buildCalendar() {
      const firstDay = new Date(this.year, this.month - 1, 1).getDay()
      const daysInMonth = new Date(this.year, this.month, 0).getDate()
      const cells = []
      for (let i = 0; i < firstDay; i++) cells.push(null)
      for (let d = 1; d <= daysInMonth; d++) cells.push(d)
      this.cells = cells
    },
    isChecked(day) {
      const dateStr = `${this.year}-${String(this.month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
      return this.checkDates.includes(dateStr)
    },
    prevMonth() {
      this.month--
      if (this.month < 1) { this.month = 12; this.year-- }
      this.buildCalendar()
    },
    nextMonth() {
      this.month++
      if (this.month > 12) { this.month = 1; this.year++ }
      this.buildCalendar()
    }
  }
}
</script>

<style>
.stats-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx 0;
  text-align: center;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.s-val {
  font-size: 44rpx;
  font-weight: 700;
  color: #10b981;
}
.s-label {
  margin-top: 8rpx;
}
.calendar {
  background: #fff;
  border-radius: 28rpx;
  padding: 24rpx 16rpx 32rpx;
}
.month-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 8rpx 24rpx;
}
.month-label {
  font-size: 32rpx;
  font-weight: 700;
}
.month-btn {
  font-size: 44rpx;
  color: #10b981;
  padding: 0 20rpx;
}
.week-row {
  display: flex;
}
.week-cell {
  flex: 1;
  text-align: center;
  font-size: 24rpx;
  color: #9ca3af;
  padding: 16rpx 0;
}
.grid {
  display: flex;
  flex-wrap: wrap;
}
.day-cell {
  width: 14.28%;
  height: 84rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}
.day {
  width: 60rpx;
  height: 60rpx;
  line-height: 60rpx;
  text-align: center;
  border-radius: 50%;
  font-size: 28rpx;
}
.day.checked {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-weight: 600;
}
</style>
