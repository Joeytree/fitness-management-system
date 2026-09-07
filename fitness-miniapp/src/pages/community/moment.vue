<template>
  <view class="page-pad" v-if="moment.id">
    <!-- 动态内容 -->
    <view class="moment-card">
      <view class="m-head">
        <view class="avatar" :style="{ background: avatarColor(moment.userId) }">{{ avatarText(moment.nickname) }}</view>
        <view class="m-user">
          <view class="m-name">{{ moment.nickname }}</view>
          <view class="m-time muted">{{ moment.createTime }}</view>
        </view>
      </view>
      <view class="m-content">{{ moment.content }}</view>
      <view v-if="moment.images" class="m-images">
        <image
          v-for="(img, i) in imageList(moment.images)"
          :key="i"
          :src="img"
          class="m-img"
          mode="aspectFill"
        />
      </view>
      <view class="m-like" @click="like">
        <text>{{ moment.liked ? '❤️' : '🤍' }}</text>
        <text>{{ moment.likeCount || 0 }}</text>
      </view>
    </view>

    <!-- 评论 -->
    <view class="section-title">评论</view>
    <view v-for="c in comments" :key="c.id" class="comment-card">
      <text class="c-name">{{ c.nickname }}：</text>
      <text class="c-content">{{ c.content }}</text>
      <view class="c-actions">
        <text class="c-reply" @click="replyTo(c)">回复</text>
        <text v-if="c.replyNickname" class="muted c-reply-to">回复 {{ c.replyNickname }}</text>
      </view>
    </view>
    <view v-if="!comments.length" class="empty muted">暂无评论</view>

    <!-- 评论输入 -->
    <view class="input-bar">
      <input
        v-model="commentContent"
        :placeholder="replyTarget ? '回复 ' + replyTarget.nickname + '...' : '写评论...'"
      />
      <button class="send-btn" @click="submit">{{ replyTarget ? '回复' : '发送' }}</button>
    </view>
  </view>
</template>

<script>
import { get, post, del } from '../../utils/request'
import { avatarColor, avatarText } from '../../utils/index'

export default {
  data() {
    return {
      id: null,
      moment: {},
      comments: [],
      commentContent: '',
      replyTarget: null
    }
  },
  onLoad(options) {
    this.id = options.id
  },
  onShow() {
    if (this.id) this.load()
  },
  methods: {
    avatarColor,
    avatarText,
    imageList(images) {
      if (Array.isArray(images)) return images
      return (images || '').split(',').filter(Boolean)
    },
    async load() {
      try {
        this.moment = await get('/moment/' + this.id)
        this.comments = await get(`/moment/${this.id}/comments`)
      } catch (e) { /* 忽略 */ }
    },
    async like() {
      try {
        if (this.moment.liked) {
          await del(`/moment/${this.id}/like`)
        } else {
          await post(`/moment/${this.id}/like`)
        }
        this.load()
      } catch (e) { /* 忽略 */ }
    },
    async submit() {
      if (!this.commentContent) return
      try {
        await post(`/moment/${this.id}/comments`, {
          content: this.commentContent,
          replyTo: this.replyTarget ? this.replyTarget.id : undefined
        })
        this.commentContent = ''
        this.replyTarget = null
        uni.showToast({ title: '评论已提交，等待审核', icon: 'none' })
        this.load()
      } catch (e) { /* 忽略 */ }
    },
    replyTo(c) {
      this.replyTarget = c
      this.commentContent = ''
    }
  }
}
</script>

<style>
.moment-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
}
.m-head {
  display: flex;
  align-items: center;
  gap: 16rpx;
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
.m-user {
  flex: 1;
}
.m-name {
  font-size: 28rpx;
  font-weight: 600;
}
.m-content {
  margin-top: 20rpx;
  font-size: 32rpx;
  line-height: 1.7;
}
.m-images {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 20rpx;
}
.m-img {
  width: 210rpx;
  height: 210rpx;
  border-radius: 16rpx;
}
.m-like {
  margin-top: 20rpx;
  display: flex;
  gap: 8rpx;
  align-items: center;
  color: #6b7280;
  font-size: 28rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 32rpx 8rpx 20rpx;
}
.comment-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
  font-size: 28rpx;
}
.c-name {
  color: #10b981;
  font-weight: 600;
}
.c-content {
  color: #374151;
}
.empty {
  text-align: center;
  padding: 60rpx 0;
}
.input-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #f3f4f6;
}
.input-bar input {
  flex: 1;
  background: #f3f4f6;
  border-radius: 999rpx;
  padding: 16rpx 28rpx;
  font-size: 28rpx;
}
.send-btn {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-size: 26rpx;
  border-radius: 999rpx;
  padding: 0 32rpx;
  line-height: 68rpx;
}
.send-btn::after {
  border: none;
}
</style>
