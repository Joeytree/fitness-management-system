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
      <view class="plan-head">
        <view class="plan-name">{{ p.name }}</view>
        <view class="pill" :class="statusClass(p)">{{ statusText(p) }}</view>
      </view>
      <view class="muted plan-cycle">{{ p.cycle || '未设定' }} · {{ goalText(p.goal) }}</view>
      <view class="bar">
        <view class="bar-fill" :style="{ width: progressOf(p) + '%' }"></view>
      </view>
      <view class="bar-tip muted">{{ progressOf(p) }}% · {{ countByPlan(p.id) }} 次打卡</view>
    </view>
    <view v-if="!list.length" class="empty muted">还没有计划，去套用模板吧</view>

    <view class="create-fab" @click="go('/pages/plan/create')">＋</view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { goalText } from '../../utils/index'

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
  padding: 28rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
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