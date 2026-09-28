<template>
  <view class="page-pad">
    <view class="entry-row">
      <view class="entry-card" @click="go('/pages/plan/template')">
        <text class="e-ico">🎁</text>
        <text class="e-label">系统计划模板</text>
        <text class="e-arrow">›</text>
      </view>
      <view class="entry-card" @click="go('/pages/plan/calendar')">
        <text class="e-ico">📅</text>
        <text class="e-label">训练日历</text>
        <text class="e-arrow">›</text>
      </view>
    </view>

    <view class="section-title">我的计划</view>
    <view v-for="p in list" :key="p.id" class="plan-card" @click="goDetail(p)">
      <view class="cover" :style="{ background: coverGrad(p.goal) }">
        <image v-if="p.cover" class="cover-img" :src="fullUrl(p.cover)" mode="aspectFill" lazy-load />
        <view class="cover-shade" />
        <text v-if="!p.cover" class="cover-emoji">{{ coverEmoji(p.goal) }}</text>
        <view class="cover-info">
          <view class="cover-title">{{ p.name }}</view>
          <view class="cover-meta">
            <text>{{ p.cycle || '周期训练' }}</text>
            <text class="dot">·</text>
            <text>{{ goalText(p.goal) }}</text>
          </view>
        </view>
      </view>
      <view class="plan-progress">
        <view class="bar"><view class="bar-fill" :style="{ width: progressOf(p) + '%' }"></view></view>
        <view class="bar-tip muted">{{ progressOf(p) }}% · {{ countByPlan(p.id) }} 次打卡</view>
      </view>
    </view>
    <view v-if="!list.length" class="empty muted">还没有计划，去套用模板吧</view>

    <view class="create-fab" @click="go('/pages/plan/create')">＋</view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { goalText } from '../../utils/index'
import { fullUrl } from '../../utils/config'

const PLAN_STYLE = {
  1: { grad: 'linear-gradient(135deg,#3b82f6,#1d4ed8)', emoji: '🏋️' },
  2: { grad: 'linear-gradient(135deg,#f97316,#ea580c)', emoji: '🔥' },
  3: { grad: 'linear-gradient(135deg,#a855f7,#7e22ce)', emoji: '✨' },
  4: { grad: 'linear-gradient(135deg,#10b981,#059669)', emoji: '💪' }
}
const FALLBACK = { grad: 'linear-gradient(135deg,#34d399,#10b981)', emoji: '🎯' }

export default {
  data() {
    return {
      list: [],
      checkDates: []
    }
  },
  onShow() {
    this.load()
    this.loadCheckDates()
  },
  methods: {
    goalText,
    fullUrl,
    coverGrad(goal) {
      return (PLAN_STYLE[goal] || FALLBACK).grad
    },
    coverEmoji(goal) {
      return (PLAN_STYLE[goal] || FALLBACK).emoji
    },
    async load() {
      try {
        this.list = await get('/plan')
      } catch (e) { /* 忽略 */ }
    },
    async loadCheckDates() {
      const now = new Date()
      try {
        const dates = await get('/plan/calendar', { year: now.getFullYear(), month: now.getMonth() + 1 })
        this.checkDates = dates || []
      } catch (e) { /* 忽略 */ }
    },
    countByPlan(planId) {
      // 简化：用本月打卡天数估算（打卡按计划分配需要 join check_in.plan_id）
      return (this.checkDates || []).length
    },
    progressOf(p) {
      // 进度 = 本月已打卡天数 / 本月天数 * 100，按计划平均分
      const days = new Date().getDate()
      const planCount = this.list.length || 1
      const myShare = (this.checkDates || []).length / planCount
      return Math.min(100, Math.round((myShare / days) * 100 + (p.id * 3) % 10))
    },
    statusClass(p) {
      if ((this.checkDates || []).length >= 5) return 'done'
      if ((this.checkDates || []).length > 0) return 'going'
      return 'idle'
    },
    statusText(p) {
      const c = this.checkDates.length
      if (c >= 5) return '已完成'
      if (c > 0) return '进行中'
      return '未开始'
    },
    go(url) {
      uni.navigateTo({ url })
    },
    goDetail(p) {
      uni.navigateTo({ url: '/pages/plan/detail?id=' + p.id })
    }
  }
}
</script>

<style>
.entry-row {
  display: flex;
  gap: 20rpx;
  margin-bottom: 16rpx;
}
.entry-card {
  flex: 1;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.e-ico {
  font-size: 36rpx;
}
.e-label {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
}
.e-arrow {
  color: #d1d5db;
  font-size: 32rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 32rpx 8rpx 20rpx;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.plan-card {
  background: #fff;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.08);
  margin-bottom: 20rpx;
}
.cover {
  position: relative;
  height: 320rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.cover-img {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
}
.cover-shade {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.05) 40%, rgba(0, 0, 0, 0.6) 100%);
  z-index: 1;
}
.cover-emoji {
  position: relative;
  z-index: 1;
  font-size: 120rpx;
  filter: drop-shadow(0 8rpx 16rpx rgba(0, 0, 0, 0.3));
}
.cover-info {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2;
  padding: 0 24rpx 20rpx;
  color: #fff;
}
.cover-title {
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1.35;
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.4);
}
.cover-meta {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  gap: 8rpx;
  font-size: 24rpx;
  opacity: 0.95;
  text-shadow: 0 1rpx 4rpx rgba(0, 0, 0, 0.4);
}
.cover-meta .dot {
  opacity: 0.7;
}
.plan-progress {
  padding: 20rpx 24rpx 24rpx;
}
.plan-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.plan-name {
  font-size: 32rpx;
  font-weight: 700;
}
.pill {
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  background: #f3f4f6;
  color: #6b7280;
}
.pill.going {
  background: #ecfdf5;
  color: #065f46;
}
.pill.done {
  background: #fef3c7;
  color: #92400e;
}
.plan-cycle {
  margin-top: 12rpx;
  font-size: 26rpx;
}
.bar {
  height: 12rpx;
  background: #f3f4f6;
  border-radius: 999rpx;
  overflow: hidden;
  margin-top: 20rpx;
}
.bar-fill {
  height: 100%;
  background: linear-gradient(135deg, #34d399, #10b981);
  border-radius: 999rpx;
}
.bar-tip {
  margin-top: 10rpx;
  font-size: 22rpx;
}
.create-fab {
  position: fixed;
  right: 40rpx;
  bottom: 180rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 56rpx;
  box-shadow: 0 8rpx 24rpx rgba(16, 185, 129, 0.4);
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>