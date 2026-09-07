<template>
  <view class="page-pad">
    <view v-for="p in list" :key="p.id" class="tpl-card">
      <view class="tpl-name">{{ p.name }}</view>
      <view class="tpl-meta">
        <text class="pill">{{ goalText(p.goal) }}</text>
        <text class="pill">{{ levelText(p.level) }}</text>
        <text class="muted">{{ p.cycle }}</text>
      </view>
      <button class="adopt-btn" @click="adopt(p)">一键套用</button>
    </view>
  </view>
</template>

<script>
import { get, post } from '../../utils/request'
import { goalText, levelText } from '../../utils/index'

export default {
  data() {
    return {
      list: []
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    goalText,
    levelText,
    async load() {
      try {
        this.list = await get('/plan/templates')
      } catch (e) { /* 忽略 */ }
    },
    async adopt(p) {
      try {
        await post(`/plan/template/${p.id}/adopt`)
        uni.showToast({ title: '已套用，生成你的计划副本', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 600)
      } catch (e) { /* 已提示 */ }
    }
  }
}
</script>

<style>
.tpl-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.tpl-name {
  font-size: 32rpx;
  font-weight: 700;
}
.tpl-meta {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex-wrap: wrap;
}
.adopt-btn {
  margin-top: 24rpx;
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-size: 26rpx;
  border-radius: 999rpx;
  padding: 0 32rpx;
  line-height: 64rpx;
  display: inline-block;
}
.adopt-btn::after {
  border: none;
}
</style>
