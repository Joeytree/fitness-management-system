<template>
  <view class="page-pad">
    <view v-for="c in list" :key="c.courseId" class="course-item" @click="goDetail(c)">
      <view class="c-name">{{ c.courseName }}</view>
      <view class="c-meta">
        <text class="pill">{{ statusText(c.status) }}</text>
        <text class="muted">{{ c.category }} · 学习 {{ c.learnCount }} 次</text>
      </view>
      <view v-if="c.lastLearnTime" class="muted c-time">最近学习：{{ c.lastLearnTime }}</view>
    </view>
    <view v-if="!list.length" class="empty muted">还没有学习过课程，去课程页看看吧</view>
  </view>
</template>

<script>
import { get } from '../../utils/request'

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
    statusText(s) {
      return { 1: '未开始', 2: '进行中', 3: '已完成' }[s] || ''
    },
    async load() {
      try {
        this.list = await get('/course/progress')
      } catch (e) { /* 忽略 */ }
    },
    goDetail(c) {
      uni.navigateTo({ url: '/pages/course/detail?id=' + c.courseId })
    }
  }
}
</script>

<style>
.course-item {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.05);
}
.c-name {
  font-size: 30rpx;
  font-weight: 600;
}
.c-meta {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.c-time {
  margin-top: 10rpx;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
