<template>
  <view class="page-pad">
    <!-- 为什么推荐 -->
    <view class="why-card">
      <view class="why-title">为什么推荐给你？</view>
      <view class="why-text">基于你的目标（{{ userGoal }}） + 水平（{{ userLevel }}）+ 标签命中生成标签，与内容标签加权打分（目标+3、水平+2、部位+1）。</view>
    </view>

    <view class="tabs">
      <view class="tab" :class="{ active: type === 'plan' }" @click="switchType('plan')">推荐计划</view>
      <view class="tab" :class="{ active: type === 'recipe' }" @click="switchType('recipe')">推荐食谱</view>
    </view>

    <view class="list">
      <view v-for="item in list" :key="item.id" class="rec-card" @click="goDetail(item)">
        <view class="rec-thumb" :class="type === 'plan' ? 'thumb-plan' : 'thumb-recipe'">
          <image v-if="item.cover" class="thumb-img" :src="fullUrl(item.cover)" mode="aspectFill" lazy-load />
          <text v-else class="thumb-ico">{{ type === 'plan' ? '🎯' : '🥗' }}</text>
          <text class="thumb-badge" :class="{ 'badge-alt': type !== 'plan' }">{{ type === 'plan' ? '官方精品' : '营养精选' }}</text>
        </view>
        <view class="rec-body">
          <view class="rec-name">{{ item.name }}</view>
          <view class="rec-meta muted">{{ type === 'plan' ? (item.cycle || '周期训练计划') : (item.calorie + ' 千卡 · 营养食谱') }}</view>
          <view class="rec-score muted">为你匹配 · 匹配分 {{ item.score }}</view>
          <view class="rec-bottom">
            <view class="rec-tags">
              <text v-for="r in (item.reasons || []).slice(0, 2)" :key="r" class="tag">{{ r }}</text>
            </view>
            <text v-if="type === 'plan'" class="adopt-btn" @click.stop="adopt(item)">一键套用</text>
          </view>
        </view>
      </view>
      <view v-if="!list.length" class="empty muted">暂无推荐</view>
    </view>
  </view>
</template>

<script>
import { get, post } from '../../utils/request'
import { getUserInfo, goalText, levelText } from '../../utils/index'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      type: 'plan',
      list: [],
      user: {}
    }
  },
  computed: {
    userGoal() {
      return goalText(this.user.goal) || '未设置'
    },
    userLevel() {
      return levelText(this.user.level) || '未设置'
    }
  },
  onShow() {
    this.user = getUserInfo()
    this.load()
  },
  methods: {
    goalText,
    levelText,
    fullUrl,
    switchType(t) {
      this.type = t
      this.load()
    },
    async load() {
      try {
        this.list = await get('/recommend/' + this.type, { limit: 10 })
      } catch (e) { /* 忽略 */ }
    },
    goDetail(item) {
      post('/recommend/feedback', { type: this.type === 'plan' ? 1 : 2, targetId: item.id, action: 'click' }).catch(() => {})
      if (this.type === 'plan') {
        uni.navigateTo({ url: '/pages/plan/template' })
      } else {
        uni.navigateTo({ url: '/pages/recipe/detail?id=' + item.id })
      }
    },
    async adopt(item) {
      try {
        await post(`/plan/template/${item.id}/adopt`)
        uni.showToast({ title: '已套用，生成你的计划副本', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 600)
      } catch (e) { /* 已提示 */ }
    }
  }
}
</script>

<style>
.why-card {
  background: #ecfdf5;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.why-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #065f46;
}
.why-text {
  margin-top: 12rpx;
  font-size: 24rpx;
  color: #4b5563;
  line-height: 1.7;
}
.tabs {
  display: flex;
  gap: 40rpx;
  margin-bottom: 24rpx;
}
.tab {
  font-size: 30rpx;
  color: #6b7280;
  padding-bottom: 12rpx;
}
.tab.active {
  color: #065f46;
  font-weight: 700;
  border-bottom: 6rpx solid #10b981;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.rec-card {
  display: flex;
  align-items: stretch;
  gap: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx;
}
.rec-thumb {
  position: relative;
  width: 256rpx;
  height: 192rpx;
  border-radius: 16rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.thumb-img {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
}
.thumb-badge {
  position: absolute;
  left: 0;
  top: 0;
  z-index: 1;
  background: rgba(17, 24, 39, 0.55);
  color: #fff;
  font-size: 20rpx;
  padding: 4rpx 16rpx;
  border-radius: 16rpx 0 12rpx 0;
}
.thumb-badge.badge-alt {
  background: #f97316;
}
.thumb-plan {
  background: linear-gradient(135deg, #a7f3d0, #34d399);
}
.thumb-recipe {
  background: linear-gradient(135deg, #fde68a, #fbbf24);
}
.thumb-ico {
  font-size: 88rpx;
}
.rec-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 4rpx 0;
}
.rec-name {
  font-size: 32rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.rec-meta {
  margin-top: 10rpx;
  font-size: 26rpx;
}
.rec-score {
  margin-top: 8rpx;
  font-size: 24rpx;
}
.rec-bottom {
  margin-top: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12rpx;
}
.rec-tags {
  display: flex;
  gap: 8rpx;
  flex-wrap: wrap;
  min-width: 0;
  overflow: hidden;
}
.tag {
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  background: #f0fdf4;
  color: #047857;
  font-size: 22rpx;
  white-space: nowrap;
}
.adopt-btn {
  flex-shrink: 0;
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-size: 26rpx;
  padding: 10rpx 28rpx;
  border-radius: 999rpx;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>