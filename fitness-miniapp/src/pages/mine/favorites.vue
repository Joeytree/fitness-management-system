<template>
  <view class="page-pad">
    <view v-for="c in list" :key="c.id" class="fav-item" @click="goDetail(c)">
      <view class="cover" :style="{ background: coverGrad(c.id) }">
        <image v-if="c.cover" class="cover-img" :src="fullUrl(c.cover)" mode="aspectFill" lazy-load />
        <text v-else class="cover-cat">{{ c.category }}</text>
      </view>
      <view class="info">
        <view class="name">{{ c.name }}</view>
        <view class="meta muted">{{ c.duration }}分钟</view>
      </view>
      <button class="unfav-btn" @click.stop="unfavorite(c)">取消收藏</button>
    </view>
    <view v-if="!list.length" class="empty muted">还没有收藏课程</view>
  </view>
</template>

<script>
import { get, del } from '../../utils/request'
import { fullUrl } from '../../utils/config'

const GRADS = [
  'linear-gradient(135deg,#6ee7b7,#10b981)',
  'linear-gradient(135deg,#93c5fd,#3b82f6)',
  'linear-gradient(135deg,#fcd34d,#f59e0b)'
]

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
    fullUrl,
    coverGrad(id) {
      return GRADS[(id || 0) % GRADS.length]
    },
    async load() {
      try {
        this.list = await get('/course/favorites')
      } catch (e) { /* 忽略 */ }
    },
    async unfavorite(c) {
      try {
        await del(`/course/${c.id}/favorite`)
        uni.showToast({ title: '已取消收藏', icon: 'none' })
        this.load()
      } catch (e) { /* 忽略 */ }
    },
    goDetail(c) {
      uni.navigateTo({ url: '/pages/course/detail?id=' + c.id })
    }
  }
}
</script>

<style>
.fav-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx;
  margin-bottom: 16rpx;
}
.cover {
  width: 100rpx;
  height: 100rpx;
  border-radius: 16rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.cover-img {
  width: 100rpx;
  height: 100rpx;
  display: block;
}
.cover-cat {
  color: #fff;
  font-size: 22rpx;
  font-weight: 600;
}
.info {
  flex: 1;
}
.name {
  font-size: 28rpx;
  font-weight: 600;
}
.meta {
  margin-top: 8rpx;
}
.unfav-btn {
  background: #fff;
  color: #ef4444;
  border: 1rpx solid #fca5a5;
  font-size: 22rpx;
  border-radius: 999rpx;
  padding: 0 24rpx;
  line-height: 56rpx;
}
.unfav-btn::after {
  border: none;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
