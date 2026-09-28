<template>
  <view class="page-pad">
    <!-- 偏好引导卡（preferParts 缺失时） -->
    <view v-if="loaded && !profile.hasProfile" class="guide-card">
      <view class="guide-title">完善偏好，推荐更精准</view>
      <view class="guide-text">选择你常练的部位（可多选）</view>
      <view class="part-grid">
        <view
          v-for="p in PARTS"
          :key="p"
          class="part-chip"
          :class="{ on: selectedParts.includes(p) }"
          @click="togglePart(p)"
        >{{ p }}</view>
      </view>
      <button class="guide-save" :disabled="!selectedParts.length" @click="saveParts">保存偏好</button>
    </view>

    <!-- 画像卡（已有偏好） -->
    <view v-else-if="loaded" class="profile-card">
      <view class="profile-title">你的推荐画像</view>
      <view class="profile-row">
        <text class="profile-item">目标：{{ goalText(profile.goal) || '未设置' }}</text>
        <text class="profile-item">水平：{{ levelText(profile.level) || '未设置' }}</text>
      </view>
      <view class="profile-row">
        <text class="profile-item">常练：{{ partsText }}</text>
      </view>
      <view class="profile-row">
        <text class="profile-item">近 30 天训练 {{ profile.trainFrequency || 0 }} 次</text>
        <text class="profile-item">平均 {{ profile.avgDuration || 0 }} 分钟</text>
      </view>
    </view>

    <view class="tabs">
      <view class="tab" :class="{ active: type === 'plan' }" @click="switchType('plan')">推荐计划</view>
      <view class="tab" :class="{ active: type === 'recipe' }" @click="switchType('recipe')">推荐食谱</view>
      <view class="tab" :class="{ active: type === 'course' }" @click="switchType('course')">推荐课程</view>
    </view>

    <view class="list">
      <view v-for="item in list" :key="item.id" class="rec-card" @click="goDetail(item)">
        <view class="rec-thumb" :class="thumbClass">
          <image v-if="item.cover" class="thumb-img" :src="fullUrl(item.cover)" mode="aspectFill" lazy-load />
          <text v-else class="thumb-ico">{{ typeIco }}</text>
          <text class="thumb-badge" :class="{ 'badge-alt': type === 'recipe' }">{{ badgeText }}</text>
        </view>
        <view class="rec-body">
          <view class="rec-name">{{ item.name }}</view>
          <view class="rec-meta muted">{{ metaText(item) }}</view>
          <view class="rec-score muted">为你匹配 · 匹配分 {{ item.score }}</view>
          <view class="rec-bottom">
            <view class="rec-tags">
              <text v-for="r in (item.reasons || []).slice(0, 2)" :key="r" class="tag">{{ r }}</text>
            </view>
            <text v-if="type === 'plan'" class="adopt-btn" @click.stop="adopt(item)">一键套用</text>
          </view>
        </view>
      </view>
      <view v-if="loaded && !list.length" class="empty muted">暂无推荐</view>
    </view>
  </view>
</template>

<script>
import { get, post, put } from '../../utils/request'
import { getUserInfo, goalText, levelText, PARTS } from '../../utils/index'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      type: 'plan',
      list: [],
      user: {},
      profile: {},
      loaded: false,
      PARTS,
      selectedParts: []
    }
  },
  computed: {
    partsText() {
      const p = this.profile.mergedParts || []
      return p.length ? p.join(' · ') : '未设置'
    },
    thumbClass() {
      return { plan: 'thumb-plan', recipe: 'thumb-recipe', course: 'thumb-course' }[this.type] || 'thumb-plan'
    },
    typeIco() {
      return { plan: '🎯', recipe: '🥗', course: '🏋️' }[this.type] || '🎯'
    },
    badgeText() {
      return { plan: '官方精品', recipe: '营养精选', course: '热门课程' }[this.type] || '官方精品'
    }
  },
  onShow() {
    this.user = getUserInfo()
    this.loadProfile()
    this.load()
  },
  methods: {
    goalText,
    levelText,
    fullUrl,
    metaText(item) {
      if (this.type === 'plan') return item.cycle || '周期训练计划'
      if (this.type === 'recipe') return (item.calorie || 0) + ' 千卡 · 营养食谱'
      return (item.category || '') + ' · ' + (item.duration || 0) + ' 分钟'
    },
    switchType(t) {
      this.type = t
      this.load()
    },
    async loadProfile() {
      try {
        this.profile = await get('/recommend/profile')
        this.selectedParts = (this.profile.preferParts || '').split(',').filter(Boolean)
      } catch (e) {
        this.profile = { hasProfile: false }
      }
      this.loaded = true
    },
    async load() {
      try {
        this.list = await get('/recommend/' + this.type, { limit: 10 })
      } catch (e) {
        this.list = []
      }
    },
    togglePart(p) {
      const i = this.selectedParts.indexOf(p)
      if (i >= 0) this.selectedParts.splice(i, 1)
      else this.selectedParts.push(p)
    },
    async saveParts() {
      if (!this.selectedParts.length) {
        uni.showToast({ title: '请至少选择一个部位', icon: 'none' })
        return
      }
      try {
        await put('/user/profile', { preferParts: this.selectedParts.join(',') })
        uni.showToast({ title: '偏好已保存', icon: 'success' })
        this.loadProfile()
        this.load()
      } catch (e) { /* 已提示 */ }
    },
    goDetail(item) {
      const typeMap = { plan: 1, recipe: 2, course: 3 }
      post('/recommend/feedback', { type: typeMap[this.type], targetId: item.id, action: 'click' }).catch(() => {})
      if (this.type === 'plan') {
        uni.navigateTo({ url: '/pages/plan/template' })
      } else if (this.type === 'recipe') {
        uni.navigateTo({ url: '/pages/recipe/detail?id=' + item.id })
      } else {
        uni.navigateTo({ url: '/pages/course/detail?id=' + item.id })
      }
    },
    async adopt(item) {
      try {
        await post(`/plan/template/${item.id}/adopt`)
        post('/recommend/feedback', { type: 1, targetId: item.id, action: 'adopt' }).catch(() => {})
        uni.showToast({ title: '已套用，生成你的计划副本', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 600)
      } catch (e) { /* 已提示 */ }
    }
  }
}
</script>

<style>
.page-pad {
  padding: 24rpx;
}
.guide-card {
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  border-radius: 28rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;
}
.guide-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #065f46;
}
.guide-text {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #4b5563;
}
.part-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 24rpx;
}
.part-chip {
  padding: 12rpx 32rpx;
  border-radius: 999rpx;
  background: #fff;
  font-size: 26rpx;
  color: #4b5563;
  border: 2rpx solid transparent;
}
.part-chip.on {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  border-color: #10b981;
}
.guide-save {
  margin-top: 24rpx;
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-size: 28rpx;
  border-radius: 999rpx;
  line-height: 76rpx;
}
.guide-save[disabled] {
  opacity: 0.5;
}
.guide-save::after {
  border: none;
}
.profile-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.profile-title {
  font-size: 28rpx;
  font-weight: 700;
  color: #1f2937;
}
.profile-row {
  display: flex;
  gap: 24rpx;
  margin-top: 16rpx;
}
.profile-item {
  font-size: 26rpx;
  color: #4b5563;
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
.thumb-course {
  background: linear-gradient(135deg, #bfdbfe, #60a5fa);
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
