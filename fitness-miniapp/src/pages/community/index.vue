<template>
  <view class="social-page">
    <!-- Tab 切换 -->
    <view class="feed-tabs">
      <view class="feed-tab" :class="{ active: tab === 'recommend' }" @click="switchTab('recommend')">推荐</view>
      <view class="feed-tab" :class="{ active: tab === 'follow' }" @click="switchTab('follow')">关注</view>
    </view>

    <view class="list">
      <view v-for="m in list" :key="m.id" class="moment-card" @click="goMoment(m)">
        <view class="m-head">
          <view class="m-user" @click.stop="goUser(m)">
            <view class="avatar" :style="{ background: avatarColor(m.userId) }">{{ avatarText(m.nickname) }}</view>
            <text class="m-name">{{ m.nickname }}</text>
          </view>
          <text class="m-time muted">{{ rel(m.createTime) }}</text>
        </view>
        <view class="m-content">{{ m.content }}</view>
        <view v-if="m.images" class="m-images">
          <image
            v-for="(img, i) in imageList(m.images)"
            :key="i"
            :src="img"
            class="m-img"
            mode="aspectFill"
          />
        </view>
        <view class="m-actions">
          <view class="m-action" @click.stop="like(m)">
            <text>{{ m.liked ? '❤️' : '🤍' }}</text>
            <text>{{ m.likeCount || 0 }}</text>
          </view>
          <view class="m-action">
            <text>💬</text>
            <text>{{ m.commentCount || 0 }}</text>
          </view>
        </view>
      </view>
      <view v-if="!list.length" class="empty muted">暂无动态，去发布第一条吧</view>
    </view>

    <!-- 发布按钮 -->
    <view class="publish-fab" @click="goPublish">✏️</view>
  </view>
</template>

<script>
import { get, post, del } from '../../utils/request'
import { avatarColor, avatarText, relativeTime } from '../../utils/index'

export default {
  data() {
    return {
      tab: 'recommend',
      list: []
    }
  },
  onShow() {
    if (this.$mp && this.getTabBar && this.getTabBar()) {
      this.getTabBar().init(2)
    }
    this.load()
  },
  methods: {
    avatarColor,
    avatarText,
    switchTab(t) {
      this.tab = t
      this.load()
    },
    imageList(images) {
      if (Array.isArray(images)) return images
      return (images || '').split(',').filter(Boolean)
    },
    rel(t) { return relativeTime(t) },
    async load() {
      try {
        const data = await get('/moment', { tab: this.tab, page: 1, pageSize: 20 })
        this.list = data.list || []
      } catch (e) { /* 忽略 */ }
    },
    async like(m) {
      try {
        if (m.liked) {
          await del(`/moment/${m.id}/like`)
        } else {
          await post(`/moment/${m.id}/like`)
        }
        this.load()
      } catch (e) { /* 忽略 */ }
    },
    goMoment(m) {
      uni.navigateTo({ url: '/pages/community/moment?id=' + m.id })
    },
    goUser(m) {
      uni.navigateTo({ url: '/pages/user/index?id=' + m.userId })
    },
    goPublish() {
      uni.navigateTo({ url: '/pages/community/publish' })
    }
  }
}
</script>

<style>
.social-page {
  padding: 0 24rpx 160rpx;
}
.feed-tabs {
  display: flex;
  gap: 40rpx;
  padding: 24rpx 8rpx;
}
.feed-tab {
  font-size: 30rpx;
  color: #6b7280;
  padding-bottom: 12rpx;
}
.feed-tab.active {
  color: #065f46;
  font-weight: 700;
  border-bottom: 6rpx solid #10b981;
}
.list {
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}
.moment-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.m-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.m-user {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.avatar {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28rpx;
  font-weight: 600;
}
.m-name {
  font-size: 28rpx;
  font-weight: 600;
}
.m-content {
  margin-top: 20rpx;
  font-size: 30rpx;
  color: #1f2937;
}
.m-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
}
.m-img {
  width: 200rpx;
  height: 200rpx;
  border-radius: 16rpx;
}
.m-actions {
  display: flex;
  gap: 40rpx;
  margin-top: 20rpx;
  color: #6b7280;
  font-size: 26rpx;
}
.m-action {
  display: flex;
  gap: 8rpx;
  align-items: center;
}
.publish-fab {
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
  font-size: 44rpx;
  box-shadow: 0 8rpx 24rpx rgba(16, 185, 129, 0.4);
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
