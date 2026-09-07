<template>
  <view class="page-pad" v-if="profile.id">
    <view class="profile-card">
      <view class="avatar" :style="{ background: avatarColor(profile.id) }">{{ avatarText(profile.nickname) }}</view>
      <view class="name">{{ profile.nickname }}</view>
      <view class="stats">
        <text @click="goFollowing('following')">关注 {{ profile.followingCount || 0 }}</text>
        <text @click="goFollowing('followers')">粉丝 {{ profile.followerCount || 0 }}</text>
      </view>
      <button
        class="follow-btn"
        :class="{ following: profile.followed }"
        @click="toggleFollow"
      >{{ profile.followed ? '已关注' : '+ 关注' }}</button>
    </view>

    <!-- 该用户动态 -->
    <view class="section-title">TA 的动态</view>
    <view v-for="m in profile.moments" :key="m.id" class="moment-card" @click="goMoment(m)">
      <view class="m-content">{{ m.content }}</view>
      <view class="m-meta muted">❤️ {{ m.likeCount || 0 }} · 💬 {{ m.commentCount || 0 }} · {{ m.createTime }}</view>
    </view>
    <view v-if="!profile.moments || !profile.moments.length" class="empty muted">TA 还没有发布动态</view>
  </view>
</template>

<script>
import { get, post, del } from '../../utils/request'
import { avatarColor, avatarText } from '../../utils/index'

export default {
  data() {
    return {
      id: null,
      profile: {}
    }
  },
  onLoad(options) {
    this.id = options.id
    this.load()
  },
  methods: {
    avatarColor,
    avatarText,
    async load() {
      try {
        this.profile = await get('/user/' + this.id)
      } catch (e) { /* 忽略 */ }
    },
    async toggleFollow() {
      try {
        if (this.profile.followed) {
          await del(`/user/${this.id}/follow`)
        } else {
          await post(`/user/${this.id}/follow`)
        }
        this.load()
      } catch (e) { /* 忽略 */ }
    },
    goFollowing(tab) {
      uni.navigateTo({ url: `/pages/user/following?id=${this.id}&tab=${tab}` })
    },
    goMoment(m) {
      uni.navigateTo({ url: '/pages/community/moment?id=' + m.id })
    }
  }
}
</script>

<style>
.profile-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 48rpx 32rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.avatar {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 52rpx;
  font-weight: 700;
}
.name {
  font-size: 36rpx;
  font-weight: 700;
  margin-top: 20rpx;
}
.stats {
  display: flex;
  gap: 48rpx;
  margin-top: 24rpx;
  font-size: 26rpx;
  color: #6b7280;
}
.follow-btn {
  margin-top: 32rpx;
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-size: 28rpx;
  border-radius: 999rpx;
  padding: 0 60rpx;
  line-height: 72rpx;
}
.follow-btn::after {
  border: none;
}
.follow-btn.following {
  background: #f3f4f6;
  color: #6b7280;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 32rpx 8rpx 20rpx;
}
.moment-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}
.m-content {
  font-size: 28rpx;
  line-height: 1.7;
}
.m-meta {
  margin-top: 12rpx;
}
.empty {
  text-align: center;
  padding: 60rpx 0;
}
</style>
