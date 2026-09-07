<template>
  <view class="mine-page">
    <!-- 个人信息 + 4 统计 -->
    <view class="profile-hero">
      <view class="hero-top">
        <view class="avatar-big" @click="go('/pages/mine/profile')">{{ avatarText(user.nickname) }}</view>
        <view class="p-info">
          <view class="p-name">{{ user.nickname || '未登录' }}</view>
          <view class="p-tags">
            <text v-if="goalText(user.goal)" class="p-tag">{{ goalText(user.goal) }}</text>
            <text v-if="levelText(user.level)" class="p-tag">{{ levelText(user.level) }}</text>
          </view>
        </view>
        <text class="p-edit" @click="go('/pages/mine/profile')">编辑 ›</text>
      </view>
      <view class="stats-row">
        <view class="stat-item" @click="go('/pages/user/following?id=' + userId + '&tab=following')">
          <text class="stat-val">{{ stats.following }}</text>
          <text class="stat-label">关注</text>
        </view>
        <view class="stat-item" @click="go('/pages/user/following?id=' + userId + '&tab=followers')">
          <text class="stat-val">{{ stats.followers }}</text>
          <text class="stat-label">粉丝</text>
        </view>
        <view class="stat-item" @click="go('/pages/mine/favorites')">
          <text class="stat-val">{{ stats.favorites }}</text>
          <text class="stat-label">收藏</text>
        </view>
        <view class="stat-item" @click="go('/pages/health/checkin')">
          <text class="stat-val">{{ stats.checkins }}</text>
          <text class="stat-label">打卡</text>
        </view>
      </view>
    </view>

    <!-- 功能列表 -->
    <view class="menu-group">
      <view class="menu-item" @click="go('/pages/plan/list')">
        <text class="menu-ico">🎯</text>
        <text class="menu-label">我的计划</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/mine/favorites')">
        <text class="menu-ico">⭐</text>
        <text class="menu-label">我的收藏</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/mine/courses')">
        <text class="menu-ico">📖</text>
        <text class="menu-label">我的课程（学习进度）</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/action/index')">
        <text class="menu-ico">💪</text>
        <text class="menu-label">动作库</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/action/ugc')">
        <text class="menu-ico">✍️</text>
        <text class="menu-label">提交自定义动作</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/course/ugc')">
        <text class="menu-ico">📚</text>
        <text class="menu-label">提交自定义课程</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/health/checkin')">
        <text class="menu-ico">🔥</text>
        <text class="menu-label">训练打卡</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/health/index')">
        <text class="menu-ico">📊</text>
        <text class="menu-label">健康管理</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/diet/index')">
        <text class="menu-ico">🥗</text>
        <text class="menu-label">饮食管理</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/mine/notifications')">
        <text class="menu-ico">🔔</text>
        <text class="menu-label">消息通知</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <button class="logout-btn" @click="doLogout">退出登录</button>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { getUserInfo, logout, goalText, levelText, avatarText } from '../../utils/index'

export default {
  data() {
    return {
      user: {},
      userId: null,
      stats: { following: 0, followers: 0, favorites: 0, checkins: 0 }
    }
  },
  onShow() {
    if (this.$mp && this.getTabBar && this.getTabBar()) {
      this.getTabBar().init(3)
    }
    this.user = getUserInfo()
    this.userId = this.user.id
    this.loadStats()
  },
  methods: {
    goalText,
    levelText,
    avatarText,
    go(url) {
      uni.navigateTo({ url })
    },
    async loadStats() {
      if (!this.userId) return
      const reqs = [
        get(`/user/${this.userId}/following`).then(r => (r || []).length).catch(() => 0),
        get(`/user/${this.userId}/followers`).then(r => (r || []).length).catch(() => 0),
        get('/course/favorites').then(r => (r || []).length).catch(() => 0),
        get('/health/checkin').then(r => (r && r.totalCount) || 0).catch(() => 0)
      ]
      const [following, followers, favorites, checkins] = await Promise.all(reqs)
      this.stats = { following, followers, favorites, checkins }
    },
    doLogout() {
      uni.showModal({
        title: '提示',
        content: '确认退出登录？',
        success: (res) => {
          if (res.confirm) {
            logout()
            uni.reLaunch({ url: '/pages/login/index' })
          }
        }
      })
    }
  }
}
</script>

<style>
.mine-page {
  padding: 24rpx 24rpx 160rpx;
}
.profile-hero {
  background: linear-gradient(135deg, #34d399, #10b981);
  border-radius: 32rpx;
  padding: 32rpx 32rpx 28rpx;
  color: #fff;
  margin-bottom: 24rpx;
}
.hero-top {
  display: flex;
  align-items: center;
  gap: 24rpx;
}
.avatar-big {
  width: 110rpx;
  height: 110rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  font-weight: 700;
}
.p-info {
  flex: 1;
}
.p-name {
  font-size: 36rpx;
  font-weight: 700;
}
.p-tags {
  display: flex;
  gap: 12rpx;
  margin-top: 12rpx;
}
.stats-row {
  display: flex;
  margin-top: 32rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid rgba(255, 255, 255, 0.25);
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.stat-val {
  font-size: 40rpx;
  font-weight: 700;
}
.stat-label {
  font-size: 22rpx;
  opacity: 0.85;
  margin-top: 6rpx;
}
.p-tags {
  display: flex;
  gap: 12rpx;
  margin-top: 12rpx;
}
.p-tag {
  background: rgba(255, 255, 255, 0.2);
  padding: 4rpx 20rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
}
.p-edit {
  font-size: 24rpx;
  opacity: 0.9;
}
.menu-group {
  background: #fff;
  border-radius: 28rpx;
  overflow: hidden;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 30rpx 32rpx;
  border-bottom: 1rpx solid #f3f4f6;
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-ico {
  font-size: 36rpx;
}
.menu-label {
  flex: 1;
  font-size: 30rpx;
  color: #1f2937;
}
.menu-arrow {
  color: #d1d5db;
  font-size: 36rpx;
}
.logout-btn {
  margin-top: 40rpx;
  background: #fff;
  color: #ef4444;
  border-radius: 28rpx;
  font-size: 30rpx;
}
.logout-btn::after {
  border: none;
}
</style>
