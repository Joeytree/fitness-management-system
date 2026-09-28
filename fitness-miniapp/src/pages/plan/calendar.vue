<template>
  <view class="page-pad">
    <!-- 月份切换 -->
    <view class="month-bar">
      <text class="month-btn" @click="prevMonth">‹</text>
      <text class="month-label">{{ year }}年{{ month }}月</text>
      <text class="month-btn" @click="nextMonth">›</text>
    </view>

    <!-- 三栏统计：训练天数 / 训练时长 / 总消耗 -->
    <view class="stat-row">
      <view class="stat-card days">
        <view class="stat-top">
          <text class="stat-ico">📅</text>
          <text class="stat-val">{{ summary.days || 0 }}</text>
          <text class="stat-unit">天</text>
        </view>
        <text class="stat-label">训练天数</text>
      </view>
      <view class="stat-card duration">
        <view class="stat-top">
          <text class="stat-ico">⏱</text>
          <text class="stat-val">{{ summary.duration || 0 }}</text>
          <text class="stat-unit">分钟</text>
        </view>
        <text class="stat-label">训练时长</text>
      </view>
      <view class="stat-card calorie">
        <view class="stat-top">
          <text class="stat-ico">🔥</text>
          <text class="stat-val">{{ fmtNum(summary.calorie) }}</text>
          <text class="stat-unit">千卡</text>
        </view>
        <text class="stat-label">总消耗</text>
      </view>
    </view>

    <!-- 日历卡片 -->
    <view class="calendar-card">
      <!-- 星期表头（周日起始） -->
      <view class="week-row">
        <text v-for="d in weeks" :key="d" class="week-cell">{{ d }}</text>
      </view>
      <!-- 日期网格 -->
      <view class="grid">
        <view v-for="(c, i) in viewCells" :key="c.day !== null ? 'd' + c.day : 'e' + i" class="day-cell">
          <view v-if="c.day" class="day" :class="{ today: c.today, 'has-data': !!c.info }">
            <text class="day-num">{{ c.day }}</text>
            <block v-if="c.info">
              <text class="day-cal">{{ c.info.calorie }}千卡</text>
              <text class="day-dur" v-if="c.info.duration">{{ c.info.duration }}分钟</text>
              <text class="part-tag" v-for="p in c.visibleParts" :key="p">{{ p }}</text>
              <text class="part-tag more" v-if="c.moreCount > 0">+{{ c.moreCount }}</text>
            </block>
          </view>
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
      year: new Date().getFullYear(),
      month: new Date().getMonth() + 1,
      weeks: ['日', '一', '二', '三', '四', '五', '六'],
      cells: [],
      days: [],
      summary: {}
    }
  },
  computed: {
    // 日期 → 当日训练明细
    dataMap() {
      const m = {}
      ;(this.days || []).forEach(d => { m[d.date] = d })
      return m
    },
    // 单元格视图：预计算部位标签的「最多 2 个 + N」
    viewCells() {
      return this.cells.map(day => {
        if (!day) return { day: null }
        const info = this.dataMap[this.dateStr(day)] || null
        const parts = (info && info.parts) || []
        return {
          day,
          info,
          today: this.isToday(day),
          visibleParts: parts.slice(0, 2),
          moreCount: Math.max(0, parts.length - 2)
        }
      })
    }
  },
  onLoad() {
    this.buildCalendar()
    this.load()
  },
  methods: {
    dateStr(day) {
      return `${this.year}-${String(this.month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    },
    fmtNum(n) {
      const s = String(Number(n || 0))
      return s.replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    },
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
        const data = await get('/plan/calendar/detail', { year: this.year, month: this.month })
        this.days = (data && data.days) || []
        this.summary = (data && data.summary) || {}
      } catch (e) {
        this.days = []
        this.summary = {}
      }
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
/* ===== 月份切换 ===== */
.month-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8rpx 24rpx 24rpx;
}
.month-label {
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2937;
}
.month-btn {
  font-size: 48rpx;
  color: #10b981;
  padding: 0 24rpx;
}

/* ===== 三栏统计 ===== */
.stat-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.stat-card {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 8rpx 18rpx;
  border-radius: 20rpx;
}
.stat-card.days { background: #ecfdf5; }
.stat-card.duration { background: #eff6ff; }
.stat-card.calorie { background: #fff7ed; }
.stat-top {
  display: flex;
  align-items: baseline;
  gap: 6rpx;
}
.stat-ico {
  font-size: 24rpx;
}
.stat-val {
  font-size: 38rpx;
  font-weight: 800;
  line-height: 1.1;
  font-variant-numeric: tabular-nums;
}
.stat-card.days .stat-val { color: #10b981; }
.stat-card.duration .stat-val { color: #3b82f6; }
.stat-card.calorie .stat-val { color: #f59e0b; }
.stat-unit {
  font-size: 18rpx;
  color: #6b7280;
}
.stat-label {
  font-size: 20rpx;
  color: #9ca3af;
  margin-top: 8rpx;
}

/* ===== 日历卡片 ===== */
.calendar-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 20rpx 8rpx 24rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.week-row {
  display: flex;
  margin-bottom: 8rpx;
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
  padding: 4rpx 3rpx;
  box-sizing: border-box;
}
.day {
  min-height: 156rpx;
  border-radius: 14rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10rpx 2rpx 8rpx;
  box-sizing: border-box;
}
.day.today {
  background: #f0fdf9;
  box-shadow: inset 0 0 0 2rpx #a7f3d0;
}
.day-num {
  font-size: 26rpx;
  font-weight: 600;
  color: #374151;
  line-height: 1.2;
}
.day.today .day-num {
  color: #10b981;
  font-weight: 800;
}
.day.has-data .day-num {
  color: #1f2937;
  font-weight: 700;
}
.day-cal {
  font-size: 17rpx;
  color: #f59e0b;
  margin-top: 6rpx;
  line-height: 1.2;
}
.day-dur {
  font-size: 16rpx;
  color: #3b82f6;
  background: #eff6ff;
  border-radius: 6rpx;
  padding: 2rpx 6rpx;
  margin-top: 5rpx;
  line-height: 1.3;
}
.part-tag {
  font-size: 16rpx;
  color: #8b5cf6;
  background: #f5f3ff;
  border-radius: 6rpx;
  padding: 2rpx 6rpx;
  margin-top: 5rpx;
  line-height: 1.3;
}
.part-tag.more {
  color: #9ca3af;
  background: #f3f4f6;
}
</style>
