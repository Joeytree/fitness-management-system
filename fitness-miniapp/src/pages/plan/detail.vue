<template>
  <view class="page-pad" v-if="detail.plan">
    <view class="head-card">
      <view class="name">{{ detail.plan.name }}</view>
      <view class="meta">
        <text class="pill">{{ goalText(detail.plan.goal) }}</text>
        <text class="muted">{{ detail.plan.cycle }}</text>
      </view>
    </view>

    <view class="section-title">课程清单（每天完成一门）</view>
    <view v-for="(wk, wi) in weeks" :key="wi" class="week-block">
      <view v-if="wk.week" class="week-title">{{ planLabel(wk.week) }}</view>
      <view v-for="day in wk.days" :key="day.dayNo" class="day-block">
        <view class="day-title">第 {{ day.dayNo }} 天</view>
        <view v-for="item in day.courses" :key="item.planCourseId" class="course-item" @click="goCourse(item.course)">
          <view class="c-thumb">
            <image v-if="item.course && item.course.cover" class="c-img" :src="fullUrl(item.course.cover)" mode="aspectFill" />
            <text v-else class="c-ico">🏋️</text>
          </view>
          <view class="c-body">
            <view class="c-name">{{ item.course ? item.course.name : '' }}</view>
            <view class="c-meta muted" v-if="item.course">
              {{ item.course.category || '' }} · {{ item.course.duration || 0 }} 分钟 · 难度{{ diffText(item.course.difficulty) }}
            </view>
          </view>
          <text class="c-arrow">›</text>
        </view>
      </view>
    </view>

    <view class="ops">
      <button class="op-btn edit" @click="goEdit">编辑计划</button>
      <button class="op-btn del" @click="onDelete">删除计划</button>
    </view>

    <button class="btn-primary start-btn" @click="startTrain">开始训练（第一天）</button>
  </view>
</template>

<script>
import { get, del } from '../../utils/request'
import { goalText, diffText } from '../../utils/index'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      id: null,
      detail: {}
    }
  },
  onLoad(options) {
    this.id = options.id
  },
  onShow() {
    if (this.id) this.load()
  },
  computed: {
    weeks() {
      const days = this.detail.days || []
      const hasWeek = days.some(d => d.week != null)
      if (!hasWeek) return [{ week: null, days }]
      const map = {}
      for (const d of days) {
        const w = d.week || 1
        ;(map[w] = map[w] || []).push(d)
      }
      return Object.keys(map).sort((a, b) => a - b).map(k => ({ week: Number(k), days: map[k] }))
    }
  },
  methods: {
    goalText,
    diffText,
    fullUrl,
    planLabel(week) {
      return { 1: '第 1 周 · 适应期', 2: '第 2 周 · 递增期', 3: '第 3 周 · 递增期', 4: '第 4 周 · 减载期' }[week] || ('第 ' + week + ' 周')
    },
    async load() {
      try {
        this.detail = await get('/plan/' + this.id)
      } catch (e) { /* 忽略 */ }
    },
    goCourse(c) {
      if (c && c.id) {
        uni.navigateTo({ url: '/pages/course/train?id=' + c.id })
      }
    },
    goEdit() {
      uni.navigateTo({ url: '/pages/plan/create?id=' + this.id })
    },
    onDelete() {
      uni.showModal({
        title: '删除计划',
        content: '确定删除该计划吗？删除后不可恢复。',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await del('/plan/' + this.id)
            uni.showToast({ title: '已删除', icon: 'success' })
            setTimeout(() => uni.navigateBack(), 500)
          } catch (e) { /* 已提示 */ }
        }
      })
    },
    startTrain() {
      const first = this.detail.days && this.detail.days[0]
      const course = first && first.courses && first.courses[0] && first.courses[0].course
      if (course && course.id) {
        uni.navigateTo({ url: '/pages/course/train?id=' + course.id })
      } else {
        uni.showToast({ title: '暂无课程', icon: 'none' })
      }
    }
  }
}
</script>

<style>
.head-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 32rpx;
}
.name {
  font-size: 38rpx;
  font-weight: 700;
}
.meta {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.pill {
  background: #ecfdf5;
  color: #065f46;
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 32rpx 8rpx 20rpx;
}
.week-block {
  margin-bottom: 8rpx;
}
.week-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #065f46;
  background: #ecfdf5;
  padding: 12rpx 20rpx;
  border-radius: 12rpx;
  margin: 20rpx 0 12rpx;
}
.day-block {
  margin-bottom: 8rpx;
}
.day-title {
  font-size: 26rpx;
  font-weight: 600;
  color: #10b981;
  margin: 20rpx 8rpx 12rpx;
}
.course-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 20rpx 24rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.05);
}
.c-thumb {
  width: 120rpx;
  height: 88rpx;
  border-radius: 12rpx;
  overflow: hidden;
  background: linear-gradient(135deg, #bfdbfe, #60a5fa);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.c-img {
  width: 100%;
  height: 100%;
}
.c-ico {
  font-size: 44rpx;
}
.c-body {
  flex: 1;
  min-width: 0;
}
.c-name {
  font-size: 28rpx;
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.c-meta {
  margin-top: 8rpx;
  font-size: 24rpx;
}
.c-arrow {
  color: #9ca3af;
  font-size: 36rpx;
  flex-shrink: 0;
}
.start-btn {
  width: 100%;
  padding: 24rpx 0;
  font-size: 32rpx;
  border: none;
  margin-top: 40rpx;
}
.start-btn::after {
  border: none;
}
.ops {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}
.op-btn {
  flex: 1;
  padding: 20rpx 0;
  border-radius: 44rpx;
  font-size: 28rpx;
  background: #fff;
}
.op-btn.edit {
  color: #10b981;
  border: 1rpx solid #10b981;
}
.op-btn.del {
  color: #ef4444;
  border: 1rpx solid #ef4444;
}
.op-btn::after {
  border: none;
}
</style>
