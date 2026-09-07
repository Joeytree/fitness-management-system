<template>
  <view class="page" v-if="course.id">
    <!-- 沉浸式 Hero：覆满到状态栏下方 -->
    <view class="hero">
      <image v-if="course.cover" class="hero-img" :src="fullUrl(course.cover)" mode="aspectFill" />
      <view v-else class="hero-img hero-fallback" :style="{ background: coverGrad(course.category) }">
        <text class="hero-emoji">{{ catEmoji(course.category) }}</text>
      </view>
      <!-- 底部深色渐变（同时盖住右下角水印） -->
      <view class="hero-shade" />
      <!-- 分类角标（浮左下） -->
      <view class="hero-badge">{{ course.category }}</view>
      <!-- 顶部操作栏（返回 / 更多） -->
      <view class="hero-top">
        <view class="top-btn" @click="goBack">
          <text class="top-icon">←</text>
        </view>
        <view class="top-right">
          <view class="top-btn" @click="onMore">
            <text class="top-icon">⋯</text>
          </view>
        </view>
      </view>
    </view>

    <!-- 主内容区 -->
    <view class="content">
      <!-- 标题区 -->
      <view class="title-row">
        <text class="title">{{ course.name }}</text>
        <view class="meta-row">
          <view class="diff-pill">{{ diffText(course.difficulty) }}</view>
          <text class="meta-dot">·</text>
          <text class="meta-item">{{ course.duration || '-' }}分钟</text>
          <text class="meta-dot">·</text>
          <text class="meta-item">{{ course.calorie || '-' }}千卡</text>
        </view>
      </view>

      <!-- 课程介绍 -->
      <view class="section">
        <text class="section-title">课程介绍</text>
        <view class="intro" :class="{ expanded: introExpanded }">
          <text class="intro-text">{{ course.intro || '本课程暂无介绍' }}</text>
        </view>
        <view v-if="canExpandIntro" class="more-row" @click="toggleIntro">
          <text class="more-text">{{ introExpanded ? '收起' : '更多' }}</text>
          <text class="more-arrow" :class="{ up: introExpanded }">▾</text>
        </view>
      </view>

      <!-- 课程内容（动作列表） -->
      <view class="section">
        <text class="section-title">课程内容</text>
        <view v-if="actions.length" class="action-list">
          <view v-for="a in actions" :key="a.id" class="action-card" @click="goAction(a)">
            <view class="action-thumb">
              <image v-if="a.mediaUrl" :src="fullUrl(a.mediaUrl)" mode="aspectFill" class="thumb-img" />
              <view v-else class="thumb-fallback">{{ (a.name || '动')[0] }}</view>
            </view>
            <view class="action-info">
              <text class="action-name">{{ a.name }}</text>
              <text class="action-meta">{{ formatSetsReps(a) }}</text>
            </view>
            <text class="action-chev">›</text>
          </view>
        </view>
        <view v-else class="empty">课程动作整理中，敬请期待</view>
      </view>

      <!-- 评论 -->
      <view class="section">
        <text class="section-title">评论</text>
        <view class="comment-input">
          <input v-model="commentContent" placeholder="发表评论" confirm-type="send" @confirm="submitComment" />
          <button class="send-btn" @click="submitComment">发表</button>
        </view>
        <view v-for="c in comments" :key="c.id" class="comment-card">
          <view class="c-head">
            <text class="c-name">{{ c.nickname }}</text>
            <text v-if="c.rating" class="c-rating">{{ '★'.repeat(c.rating) }}</text>
          </view>
          <view class="c-content">{{ c.content }}</view>
        </view>
        <view v-if="!comments.length" class="empty">暂无评论</view>
      </view>
    </view>

    <!-- 底部固定栏 -->
    <view class="bottom-bar">
      <view class="bottom-side">
        <view class="icon-btn" :class="{ on: favorite }" @click="toggleFavorite">
          <text class="ib-icon">{{ favorite ? '★' : '☆' }}</text>
          <text class="ib-label">收藏</text>
        </view>
        <view class="icon-btn" :class="{ on: liked }" @click="toggleLike">
          <text class="ib-icon">{{ liked ? '♥' : '♡' }}</text>
          <text class="ib-label">点赞</text>
        </view>
      </view>
      <button class="start-btn" :class="{ disabled: !actions.length }" @click="startTrain">{{ actions.length ? '开始训练' : '课程动作整理中' }}</button>
    </view>

    <!-- Home Indicator 占位 -->
    <view class="home-indicator" />
  </view>
</template>

<script>
import { get, post, del } from '../../utils/request'
import { diffText } from '../../utils/index'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      id: null,
      course: {},
      actions: [],
      favorite: false,
      liked: false,
      introExpanded: false,
      commentContent: '',
      comments: []
    }
  },
  computed: {
    canExpandIntro() {
      return (this.course.intro || '').length > 60
    }
  },
  onLoad(options) {
    this.id = options.id
  },
  onShow() {
    if (this.id) this.load()
  },
  methods: {
    diffText,
    fullUrl,
    coverGrad(cat) {
      const map = {
        '增肌': 'linear-gradient(135deg,#1FCB8A,#0FA56C)',
        '减脂': 'linear-gradient(135deg,#f97316,#ea580c)',
        '塑形': 'linear-gradient(135deg,#a855f7,#7e22ce)',
        '瑜伽': 'linear-gradient(135deg,#06b6d4,#0891b2)',
        'HIIT': 'linear-gradient(135deg,#ef4444,#dc2626)',
        '康复拉伸': 'linear-gradient(135deg,#10b981,#059669)'
      }
      return map[cat] || 'linear-gradient(135deg,#1FCB8A,#0FA56C)'
    },
    catEmoji(cat) {
      const map = {
        '增肌': '💪', '减脂': '🔥', '塑形': '🌸',
        '瑜伽': '🧘', 'HIIT': '⚡', '康复拉伸': '🌿'
      }
      return map[cat] || '🏋️'
    },
    formatSetsReps(a) {
      if (a.sets && a.reps) return `${a.sets} 组 · ${a.reps} 次`
      if (a.sets) return `${a.sets} 组`
      if (a.reps) return `${a.reps} 次`
      return `${a.part || ''}${a.equipment ? ' · ' + a.equipment : ''}`.replace(/^· /, '')
    },
    goBack() {
      uni.navigateBack({ delta: 1 })
    },
    onMore() {
      uni.showActionSheet({
        itemList: ['分享课程', '举报课程'],
        success: (res) => {
          if (res.tapIndex === 0) uni.showToast({ title: '分享功能待接入', icon: 'none' })
          if (res.tapIndex === 1) uni.showToast({ title: '已收到举报', icon: 'none' })
        }
      })
    },
    toggleIntro() {
      this.introExpanded = !this.introExpanded
    },
    goAction(a) {
      uni.navigateTo({ url: '/pages/action/detail?id=' + a.id })
    },
    async load() {
      try {
        const data = await get('/course/' + this.id)
        this.course = data.course || {}
        this.actions = data.actions || []
        this.favorite = !!data.favorite
        this.liked = !!data.liked
      } catch (e) {}
      try {
        const c = await get(`/course/${this.id}/comments`)
        this.comments = c || []
      } catch (e) {}
    },
    async toggleFavorite() {
      try {
        if (this.favorite) {
          await del(`/course/${this.id}/favorite`)
        } else {
          await post(`/course/${this.id}/favorite`)
        }
        this.favorite = !this.favorite
        uni.showToast({ title: this.favorite ? '已收藏' : '已取消收藏', icon: 'none' })
      } catch (e) {}
    },
    async toggleLike() {
      if (this.liked) {
        uni.showToast({ title: '已点赞过啦', icon: 'none' })
        return
      }
      try {
        await post(`/course/${this.id}/like`)
        this.liked = true
        uni.showToast({ title: '点赞成功', icon: 'none' })
      } catch (e) {}
    },
    async submitComment() {
      if (!this.commentContent.trim()) {
        uni.showToast({ title: '请输入评论内容', icon: 'none' })
        return
      }
      try {
        await post(`/course/${this.id}/comments`, { content: this.commentContent, rating: 5 })
        this.commentContent = ''
        uni.showToast({ title: '评论已提交，等待审核', icon: 'none' })
        this.load()
      } catch (e) {}
    },
    async startTrain() {
      if (!this.actions.length) {
        uni.showToast({ title: '课程动作整理中，敬请期待', icon: 'none' })
        return
      }
      uni.navigateTo({ url: '/pages/course/train?id=' + this.id })
    }
  }
}
</script>

<style>
.page {
  min-height: 100vh;
  background: #f6faf8;
  padding-bottom: 220rpx;
}

/* ===== Hero 沉浸式大图 ===== */
.hero {
  position: relative;
  width: 100%;
  height: 600rpx;
  overflow: hidden;
}
.hero-img {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
}
.hero-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
}
.hero-emoji {
  font-size: 220rpx;
  opacity: 0.55;
  filter: drop-shadow(0 6rpx 16rpx rgba(0, 0, 0, 0.35));
}
.hero-shade {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.35) 0%, rgba(0, 0, 0, 0) 35%, rgba(0, 0, 0, 0) 50%, rgba(0, 0, 0, 0.7) 100%);
}
.hero-badge {
  position: absolute;
  left: 32rpx;
  bottom: 28rpx;
  padding: 6rpx 22rpx;
  border-radius: 999rpx;
  background: rgba(255, 72, 72, 0.92);
  color: #fff;
  font-size: 24rpx;
  font-weight: 600;
  letter-spacing: 1rpx;
  box-shadow: 0 4rpx 12rpx rgba(0, 0, 0, 0.25);
}
.hero-top {
  position: absolute;
  top: calc(env(safe-area-inset-top) + 12rpx);
  left: 0;
  right: 0;
  padding: 0 20rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 2;
}
.top-right {
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.top-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 999rpx;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(8rpx);
}
.top-btn.on {
  background: rgba(31, 203, 138, 0.85);
}
.top-icon {
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  line-height: 1;
}

/* ===== 主内容 ===== */
.content {
  background: #f6faf8;
  margin-top: -24rpx;
  border-radius: 24rpx 24rpx 0 0;
  padding: 32rpx 24rpx 0;
  position: relative;
  z-index: 1;
}

/* 标题区 */
.title-row {
  padding-bottom: 28rpx;
  border-bottom: 1rpx solid #ecedf0;
}
.title {
  font-size: 56rpx;
  font-weight: 700;
  color: #14181F;
  line-height: 1.3;
}
.meta-row {
  margin-top: 20rpx;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  row-gap: 8rpx;
}
.diff-pill {
  padding: 4rpx 18rpx;
  border-radius: 22rpx;
  background: #ecfdf5;
  color: #0FA56C;
  font-size: 24rpx;
  font-weight: 600;
}
.meta-dot {
  color: #8A94A6;
  font-size: 24rpx;
  margin: 0 12rpx;
}
.meta-item {
  color: #4B5563;
  font-size: 26rpx;
}

/* 通用 section */
.section {
  margin-top: 36rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #14181F;
  margin-bottom: 20rpx;
  display: block;
}

/* 介绍区 */
.intro {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  line-height: 1.7;
}
.intro.expanded {
  display: block;
}
.intro-text {
  color: #4B5563;
  font-size: 28rpx;
}
.more-row {
  margin-top: 8rpx;
  text-align: right;
}
.more-text {
  color: #1FCB8A;
  font-size: 26rpx;
  font-weight: 500;
}
.more-arrow {
  color: #1FCB8A;
  font-size: 24rpx;
  margin-left: 4rpx;
  display: inline-block;
  transition: transform 0.2s;
}
.more-arrow.up {
  transform: rotate(180deg);
}

/* 课程内容 - 动作列表 */
.action-list {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
}
.action-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 14rpx;
  padding: 16rpx;
  box-shadow: 0 2rpx 8rpx rgba(16, 185, 129, 0.04);
}
.action-thumb {
  width: 80rpx;
  height: 80rpx;
  border-radius: 10rpx;
  overflow: hidden;
  flex-shrink: 0;
  background: #f3f4f6;
}
.thumb-img {
  width: 100%;
  height: 100%;
}
.thumb-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  color: #0FA56C;
  font-size: 30rpx;
  font-weight: 700;
}
.action-info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  justify-content: center;
  min-width: 0;
}
.action-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #14181F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.action-meta {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #8A94A6;
}
.action-chev {
  font-size: 36rpx;
  color: #B5BCC7;
  margin-left: 8rpx;
}

/* 空状态 */
.empty {
  text-align: center;
  padding: 60rpx 0;
  color: #8A94A6;
  font-size: 26rpx;
}

/* 评论输入 */
.comment-input {
  display: flex;
  align-items: center;
  gap: 16rpx;
  background: #fff;
  border-radius: 14rpx;
  padding: 16rpx 20rpx;
}
.comment-input input {
  flex: 1;
  font-size: 28rpx;
  color: #14181F;
}
.send-btn {
  background: linear-gradient(135deg, #1FCB8A, #0FA56C);
  color: #fff;
  font-size: 24rpx;
  padding: 0 24rpx;
  height: 56rpx;
  line-height: 56rpx;
  border-radius: 999rpx;
  border: none;
}
.send-btn::after {
  border: none;
}

.comment-card {
  background: #fff;
  border-radius: 14rpx;
  padding: 20rpx;
  margin-top: 16rpx;
}
.c-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.c-name {
  font-weight: 600;
  font-size: 26rpx;
  color: #14181F;
}
.c-rating {
  color: #f59e0b;
  font-size: 22rpx;
}
.c-content {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #4B5563;
  line-height: 1.6;
}

/* ===== 底部固定栏 ===== */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 16rpx 24rpx calc(env(safe-area-inset-bottom) + 16rpx);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(12rpx);
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.bottom-side {
  display: flex;
  align-items: center;
  gap: 8rpx;
  flex-shrink: 0;
}
.icon-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8rpx 16rpx;
  border-radius: 14rpx;
  min-width: 88rpx;
}
.icon-btn.on {
  background: #ecfdf5;
}
.ib-icon {
  font-size: 36rpx;
  color: #14181F;
  line-height: 1;
}
.icon-btn.on .ib-icon {
  color: #FF4848;
}
.ib-label {
  font-size: 20rpx;
  color: #4B5563;
  margin-top: 4rpx;
  line-height: 1.2;
}
.icon-btn.on .ib-label {
  color: #0FA56C;
}
.start-btn {
  flex: 1;
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #1FCB8A, #0FA56C);
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  border-radius: 999rpx;
  border: none;
  box-shadow: 0 6rpx 20rpx rgba(31, 203, 138, 0.35);
}
.start-btn::after {
  border: none;
}
.start-btn.disabled {
  background: #D7DCE3;
  box-shadow: none;
  color: #8A94A6;
}

/* Home Indicator 占位 */
.home-indicator {
  position: fixed;
  left: 0;
  right: 0;
  bottom: env(safe-area-inset-bottom);
  height: 4rpx;
  z-index: 11;
  pointer-events: none;
}
</style>