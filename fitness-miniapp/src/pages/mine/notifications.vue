<template>
  <view class="page-pad">
    <view class="section-title">审核结果</view>
    <view v-for="(n, i) in audits" :key="'a' + i" class="notice-item audit">
      <view class="n-title">⚠️ {{ n.title }}</view>
      <view class="n-content muted">{{ n.content }}</view>
      <view v-if="n.reason" class="n-reason">驳回原因：{{ n.reason }}</view>
      <view class="n-time muted">{{ n.time }}</view>
    </view>
    <view v-if="!audits.length" class="empty muted">暂无审核通知</view>

    <view class="section-title">系统公告</view>
    <view v-for="a in notices" :key="a.id" class="notice-item" @click="goDetail(a)">
      <view class="n-title">📢 {{ a.title }}</view>
      <view class="n-content muted">{{ a.content }}</view>
      <view class="n-time muted">{{ a.createTime }}</view>
    </view>
    <view v-if="!notices.length" class="empty muted">暂无公告</view>
    <view v-if="notices.length" class="load-tip">{{ loading ? '加载中…' : (hasMore ? '上拉加载更多' : '没有更多了') }}</view>
  </view>
</template>

<script>
import { get } from '../../utils/request'

export default {
  data() {
    return {
      audits: [],
      notices: [],
      page: 1,
      hasMore: true,
      loading: false
    }
  },
  onShow() {
    this.load()
  },
  onReachBottom() {
    this.loadMore()
  },
  methods: {
    async load() {
      this.page = 1
      this.hasMore = true
      this.loading = false
      try {
        this.audits = await get('/notifications')
      } catch (e) { /* 忽略 */ }
      try {
        const data = await get('/article', { type: 3, page: 1, pageSize: 20 })
        this.notices = data.list || []
        this.hasMore = this.notices.length < (data.total || 0)
      } catch (e) { /* 忽略 */ }
    },
    async loadMore() {
      if (this.loading || !this.hasMore) return
      this.loading = true
      try {
        const data = await get('/article', { type: 3, page: this.page + 1, pageSize: 20 })
        const list = data.list || []
        if (list.length) {
          this.notices = this.notices.concat(list)
          this.page += 1
          this.hasMore = this.notices.length < (data.total || 0)
        } else {
          this.hasMore = false
        }
      } catch (e) { /* 忽略 */ }
      this.loading = false
    },
    goDetail(a) {
      uni.navigateTo({ url: '/pages/knowledge/detail?id=' + a.id })
    }
  }
}
</script>

<style>
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 24rpx 8rpx 20rpx;
}
.notice-item {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}
.n-title {
  font-size: 28rpx;
  font-weight: 600;
}
.n-content {
  margin-top: 12rpx;
  font-size: 26rpx;
  line-height: 1.6;
}
.n-reason {
  margin-top: 10rpx;
  font-size: 26rpx;
  color: #ef4444;
}
.n-time {
  margin-top: 12rpx;
}
.empty {
  text-align: center;
  padding: 60rpx 0;
}
.load-tip {
  text-align: center;
  padding: 24rpx 0;
  font-size: 24rpx;
  color: #9ca3af;
}
</style>
