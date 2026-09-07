<template>
  <view class="home">
    <!-- 搜索框 -->
    <view class="search-bar" @click="go('/pages/search/index')">
      <text class="search-ico">🔍</text>
      <text class="search-text muted">搜索食物 / 课程 / 知识库</text>
    </view>

    <!-- 公告 -->
    <view v-if="notices.length" class="notice" @click="go('/pages/mine/notifications')">
      <text class="notice-icon">📢</text>
      <swiper class="notice-swiper" vertical autoplay circular :interval="3000">
        <swiper-item v-for="n in notices" :key="n.id">
          <text class="notice-text">{{ n.title }}</text>
        </swiper-item>
      </swiper>
    </view>

    <!-- 欢迎 -->
    <view class="greet-row">
      <view class="greet">
        <view class="greet-name">{{ user.nickname || '健身者' }}，今天也要加油 💪</view>
        <view class="greet-tags">
          <text v-if="goalText(user.goal)" class="greet-pill">{{ goalText(user.goal) }}</text>
          <text v-if="levelText(user.level)" class="greet-pill">{{ levelText(user.level) }}</text>
        </view>
      </view>
      <view class="avatar" :style="{ background: avatarColor(user.id) }">{{ avatarText(user.nickname) }}</view>
    </view>

    <!-- 打卡大卡片 -->
    <view class="hero-card">
      <view class="hero-row">
        <view class="hero-item">
          <view class="hero-label">今日摄入</view>
          <view class="hero-val">{{ summary.totalCalorie || 0 }}<text class="hero-unit">/{{ summary.targetCalorie || 0 }} kcal</text></view>
        </view>
        <view class="hero-item center">
          <view class="hero-label">今日打卡</view>
          <view class="hero-val">{{ checked ? '✓' : '○' }}</view>
        </view>
        <view class="hero-item right">
          <view class="hero-label">连续打卡</view>
          <view class="hero-val">{{ checkin.streak || 0 }}<text class="hero-unit">天</text></view>
        </view>
      </view>
      <button class="check-btn" :class="{ done: checked }" @click="doCheckIn">
        {{ checked ? '已完成今日打卡 ✓' : '立即打卡' }}
      </button>
    </view>

    <!-- 快捷入口 -->
    <view class="quick-grid">
      <view class="quick-item" @click="go('/pages/health/checkin')">
        <view class="quick-ico" style="background:#d1fae5">🔥</view>
        <text>打卡</text>
      </view>
      <view class="quick-item" @click="go('/pages/diet/index')">
        <view class="quick-ico" style="background:#dbeafe">🥗</view>
        <text>饮食</text>
      </view>
      <view class="quick-item" @click="go('/pages/health/index')">
        <view class="quick-ico" style="background:#fef3c7">📊</view>
        <text>健康</text>
      </view>
      <view class="quick-item" @click="go('/pages/plan/list')">
        <view class="quick-ico" style="background:#ede9fe">🎯</view>
        <text>计划</text>
      </view>
    </view>

    <!-- 推荐计划 -->
    <view class="section-title" @click="go('/pages/recommend/index')">为你推荐 <text class="more">更多 ›</text></view>
    <view class="rec-list">
      <view v-for="p in plans" :key="p.id" class="rec-card" @click="clickPlan(p)">
        <view class="card-thumb thumb-plan">
          <image v-if="p.cover" class="thumb-img" :src="fullUrl(p.cover)" mode="aspectFill" lazy-load />
          <text v-else class="thumb-ico">🎯</text>
          <text class="thumb-badge">官方精品</text>
        </view>
        <view class="card-body">
          <view class="plan-name">{{ p.name }}</view>
          <view class="card-sub muted">{{ p.cycle || '周期训练计划' }}</view>
          <view class="rec-tags">
            <text v-for="r in (p.reasons || []).slice(0, 2)" :key="r" class="pill">{{ r }}</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 推荐食谱 -->
    <view class="section-title" @click="go('/pages/recipe/list')">推荐食谱 <text class="more">更多 ›</text></view>
    <view class="rec-list">
      <view v-for="r in recipes" :key="r.id" class="rec-card" @click="clickRecipe(r)">
        <view class="card-thumb thumb-recipe">
          <image v-if="r.cover" class="thumb-img" :src="fullUrl(r.cover)" mode="aspectFill" lazy-load />
          <text v-else class="thumb-ico">🥗</text>
          <text class="thumb-badge badge-alt">营养精选</text>
        </view>
        <view class="card-body">
          <view class="recipe-name">{{ r.name }}</view>
          <view class="card-sub muted">{{ r.calorie }} 千卡 · 营养食谱</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { get, post } from '../../utils/request'
import { getUserInfo, goalText, levelText, avatarColor, avatarText, fmtDate } from '../../utils/index'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      user: {},
      plans: [],
      recipes: [],
      notices: [],
      summary: {},
      checkin: {},
      checked: false
    }
  },
  onShow() {
    if (this.$mp && this.getTabBar && this.getTabBar()) {
      this.getTabBar().init(0)
    }
    this.user = getUserInfo()
    this.load()
  },
  methods: {
    goalText,
    levelText,
    avatarColor,
    avatarText,
    fullUrl,
    async load() {
      try {
        this.plans = await get('/recommend/plan', { limit: 3 })
      } catch (e) {}
      try {
        this.recipes = await get('/recommend/recipe', { limit: 3 })
      } catch (e) {}
      try {
        const data = await get('/article', { type: 3, page: 1, pageSize: 5 })
        this.notices = data.list || []
      } catch (e) {}
      try {
        this.summary = await get('/diet/summary')
      } catch (e) {}
      try {
        this.checkin = await get('/health/checkin')
        const today = fmtDate(new Date() + '')
        this.checked = (this.checkin.checkDates || []).includes(today)
      } catch (e) {}
    },
    async doCheckIn() {
      if (this.checked) return
      try {
        await post('/health/checkin', { duration: 30, calorie: 200 })
        this.checked = true
        uni.showToast({ title: '打卡成功！', icon: 'success' })
        this.load()
      } catch (e) { /* 已提示 */ }
    },
    go(url) {
      uni.navigateTo({ url })
    },
    clickPlan(p) {
      post('/recommend/feedback', { type: 1, targetId: p.id, action: 'click' }).catch(() => {})
      this.go('/pages/plan/template')
    },
    clickRecipe(r) {
      post('/recommend/feedback', { type: 2, targetId: r.id, action: 'click' }).catch(() => {})
      this.go('/pages/recipe/detail?id=' + r.id)
    }
  }
}
</script>

<style>
.home {
  padding: 16rpx 24rpx 160rpx;
}
.search-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: #fff;
  border-radius: 999rpx;
  padding: 18rpx 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.05);
}
.search-ico {
  font-size: 30rpx;
  color: #9ca3af;
}
.search-text {
  font-size: 28rpx;
}
.notice {
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 20rpx 28rpx;
  margin-top: 16rpx;
}
.notice-icon {
  font-size: 32rpx;
}
.notice-swiper {
  flex: 1;
  height: 40rpx;
}
.notice-text {
  font-size: 26rpx;
  color: #4b5563;
}
.greet-row {
  display: flex;
  align-items: center;
  margin-top: 24rpx;
}
.greet {
  flex: 1;
}
.greet-name {
  font-size: 36rpx;
  font-weight: 700;
  color: #1f2937;
}
.greet-tags {
  display: flex;
  gap: 12rpx;
  margin-top: 12rpx;
}
.greet-pill {
  background: #ecfdf5;
  color: #065f46;
  padding: 6rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
}
.avatar {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42rpx;
  font-weight: 700;
}
.hero-card {
  background: linear-gradient(135deg, #34d399, #10b981);
  border-radius: 32rpx;
  padding: 32rpx;
  color: #fff;
  margin-top: 20rpx;
  box-shadow: 0 8rpx 24rpx rgba(16, 185, 129, 0.25);
}
.hero-row {
  display: flex;
  align-items: center;
}
.hero-item {
  flex: 1;
}
.hero-item.center {
  text-align: center;
}
.hero-item.right {
  text-align: right;
}
.hero-label {
  font-size: 24rpx;
  opacity: 0.9;
}
.hero-val {
  font-size: 44rpx;
  font-weight: 700;
  margin-top: 8rpx;
}
.hero-unit {
  font-size: 22rpx;
  font-weight: 400;
  margin-left: 6rpx;
  opacity: 0.85;
}
.check-btn {
  width: 100%;
  padding: 20rpx 0;
  border-radius: 44rpx;
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
  font-size: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.4);
  margin-top: 24rpx;
}
.check-btn.done {
  background: rgba(255, 255, 255, 0.15);
  opacity: 0.85;
}
.check-btn::after {
  border: none;
}
.quick-grid {
  display: flex;
  background: #fff;
  border-radius: 28rpx;
  padding: 32rpx 0;
  box-shadow: 0 6rpx 24rpx rgba(16, 185, 129, 0.06);
  margin-top: 20rpx;
}
.quick-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12rpx;
  font-size: 24rpx;
  color: #4b5563;
}
.quick-ico {
  width: 88rpx;
  height: 88rpx;
  border-radius: 28rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 44rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 32rpx 8rpx 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.more {
  font-size: 24rpx;
  color: #9ca3af;
  font-weight: 400;
}
.rec-list {
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
.card-thumb {
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
.card-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  padding: 4rpx 0;
}
.plan-name, .recipe-name {
  font-size: 32rpx;
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-sub {
  margin-top: 10rpx;
  font-size: 26rpx;
}
.rec-tags {
  margin-top: auto;
  display: flex;
  gap: 8rpx;
  flex-wrap: wrap;
}
</style>