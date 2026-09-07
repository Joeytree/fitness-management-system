<template>
  <view class="page-pad">
    <view class="tabs">
      <view class="tab" :class="{ active: tab === 'following' }" @click="switchTab('following')">关注</view>
      <view class="tab" :class="{ active: tab === 'followers' }" @click="switchTab('followers')">粉丝</view>
    </view>

    <view v-for="u in list" :key="u.id" class="user-item">
      <view class="avatar" :style="{ background: avatarColor(u.id) }" @click="goUser(u)">{{ avatarText(u.nickname) }}</view>
      <text class="u-name" @click="goUser(u)">{{ u.nickname }}</text>
      <button
        v-if="u.id !== myId"
        class="follow-btn"
        :class="{ following: u.followed }"
        @click="toggleFollow(u)"
      >{{ u.followed ? '已关注' : '关注' }}</button>
    </view>
    <view v-if="!list.length" class="empty muted">暂无数据</view>
  </view>
</template>

<script>
import { get, post, del } from '../../utils/request'
import { avatarColor, avatarText, getUserInfo } from '../../utils/index'

export default {
  data() {
    return {
      id: null,
      tab: 'following',
      list: [],
      myId: null
    }
  },
  onLoad(options) {
    this.id = options.id
    this.tab = options.tab || 'following'
    this.myId = getUserInfo().id
    this.load()
  },
  methods: {
    avatarColor,
    avatarText,
    switchTab(t) {
      this.tab = t
      this.load()
    },
    async load() {
      try {
        this.list = await get(`/user/${this.id}/${this.tab}`)
      } catch (e) { /* 忽略 */ }
    },
    async toggleFollow(u) {
      try {
        if (u.followed) {
          await del(`/user/${u.id}/follow`)
        } else {
          await post(`/user/${u.id}/follow`)
        }
        this.load()
      } catch (e) { /* 忽略 */ }
    },
    goUser(u) {
      uni.navigateTo({ url: '/pages/user/index?id=' + u.id })
    }
  }
}
</script>

<style>
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
.user-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 16rpx;
}
.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30rpx;
  font-weight: 600;
}
.u-name {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
}
.follow-btn {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-size: 24rpx;
  border-radius: 999rpx;
  padding: 0 32rpx;
  line-height: 60rpx;
}
.follow-btn::after {
  border: none;
}
.follow-btn.following {
  background: #f3f4f6;
  color: #6b7280;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
