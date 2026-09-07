<template>
  <view class="page-pad">
    <view class="tabs">
      <view
        v-for="g in goals"
        :key="g.value"
        class="tab"
        :class="{ active: goal === g.value }"
        @click="selectGoal(g.value)"
      >{{ g.label }}</view>
    </view>

    <view class="list">
      <view v-for="r in list" :key="r.id" class="recipe-card" @click="goDetail(r)">
        <view class="r-name">{{ r.name }}</view>
        <view class="r-meta">
          <text class="pill">{{ goalText(r.goal) }}</text>
          <text class="muted">{{ r.calorie }}千卡</text>
        </view>
        <view class="r-content muted">{{ r.content }}</view>
      </view>
      <view v-if="!list.length" class="empty muted">暂无食谱</view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { goalText } from '../../utils/index'

export default {
  data() {
    return {
      goal: 0,
      goals: [
        { value: 0, label: '全部' },
        { value: 1, label: '增肌' },
        { value: 2, label: '减脂' },
        { value: 3, label: '塑形' }
      ],
      list: []
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    goalText,
    selectGoal(g) {
      this.goal = g
      this.load()
    },
    async load() {
      try {
        const data = await get('/recipe', { goal: this.goal || undefined, page: 1, pageSize: 20 })
        this.list = data.list || []
      } catch (e) { /* 忽略 */ }
    },
    goDetail(r) {
      uni.navigateTo({ url: '/pages/recipe/detail?id=' + r.id })
    }
  }
}
</script>

<style>
.tabs {
  display: flex;
  gap: 20rpx;
  margin-bottom: 24rpx;
}
.tab {
  padding: 12rpx 36rpx;
  border-radius: 999rpx;
  background: #fff;
  font-size: 26rpx;
  color: #4b5563;
}
.tab.active {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-weight: 600;
}
.recipe-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.r-name {
  font-size: 30rpx;
  font-weight: 600;
}
.r-meta {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.r-content {
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 1.6;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
