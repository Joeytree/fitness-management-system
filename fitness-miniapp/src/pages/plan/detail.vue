<template>
  <view class="page-pad" v-if="detail.plan">
    <view class="head-card">
      <view class="name">{{ detail.plan.name }}</view>
      <view class="meta">
        <text class="pill">{{ goalText(detail.plan.goal) }}</text>
        <text class="muted">{{ detail.plan.cycle }}</text>
      </view>
    </view>

    <view class="section-title">动作清单</view>
    <view v-for="day in detail.days" :key="day.dayNo" class="day-block">
      <view class="day-title">第 {{ day.dayNo }} 天</view>
      <view v-for="item in day.actions" :key="item.planActionId" class="action-item">
        <view class="a-name">{{ item.action.name }}</view>
        <view class="a-params">
          {{ item.sets }} 组 × {{ item.reps }} 次
          <text v-if="item.weight"> · {{ item.weight }}kg</text>
          <text> · 休息 {{ item.rest || 0 }}s</text>
        </view>
      </view>
    </view>

    <button class="btn-primary start-btn" @click="startTrain">开始训练</button>
    <view class="skip-checkin" @click="quickCheckin">仅打卡（不记录训练）</view>
  </view>
</template>

<script>
import { get, post } from '../../utils/request'
import { goalText } from '../../utils/index'

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
  methods: {
    goalText,
    async load() {
      try {
        this.detail = await get('/plan/' + this.id)
      } catch (e) { /* 忽略 */ }
    },
    startTrain() {
      uni.navigateTo({ url: '/pages/training/execute?id=' + this.id })
    },
    async quickCheckin() {
      try {
        await post('/health/checkin', {
          planId: Number(this.id),
          duration: 30,
          calorie: 200
        })
        uni.showToast({ title: '打卡成功！', icon: 'success' })
      } catch (e) { /* 已提示（当日已打卡等） */ }
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
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 32rpx 8rpx 20rpx;
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
.action-item {
  background: #fff;
  border-radius: 24rpx;
  padding: 26rpx 28rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.05);
}
.a-name {
  font-size: 28rpx;
  font-weight: 600;
}
.a-params {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #6b7280;
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
.skip-checkin {
  text-align: center;
  font-size: 26rpx;
  color: #9ca3af;
  margin-top: 24rpx;
}
</style>
