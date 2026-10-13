<template>
  <view class="page-pad">
    <view class="tabs">
      <view
        v-for="g in goals"
        :key="g.value"
        class="tab"
        :class="{ active: goal === g.value }"
        @click="selectGoal(g.value)"
      >{{ g.label }}</view>
    </view>

    <view class="list">
      <view v-for="r in list" :key="r.id" class="recipe-card" @click="goDetail(r)">
        <view class="r-cover">
          <image v-if="r.cover" class="r-cover-img" :src="fullUrl(r.cover)" mode="aspectFill" lazy-load />
          <text v-else class="r-cover-ico">🍱</text>
        </view>
        <view class="r-name">{{ r.name }}</view>
        <view class="r-meta">
          <text class="pill">{{ goalText(r.goal) }}</text>
          <text class="muted">{{ r.calorie }} 千卡</text>
        </view>
      </view>
      <view v-if="!list.length" class="empty muted">暂无食谱</view>
      <view v-else class="load-tip">{{ loading ? '加载中…' : (hasMore ? '上拉加载更多' : '没有更多了') }}</view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { goalText } from '../../utils/index'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      goal: 0,
      goals: [
        { value: 0, label: '全部' },
        { value: 1, label: '增肌' },
        { value: 2, label: '减脂' },
        { value: 3, label: '塑形' },
        { value: 4, label: '保持' }
      ],
      list: [],
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
    goalText,
    fullUrl,
    selectGoal(g) {
      this.goal = g
      this.load()
    },
    async load() {
      this.page = 1
      this.hasMore = true
      this.loading = false
      try {
        const data = await get('/recipe', { goal: this.goal || undefined, page: 1, pageSize: 20 })
        this.list = data.list || []
        this.hasMore = this.list.length < (data.total || 0)
      } catch (e) { /* 忽略 */ }
    },
    async loadMore() {
      if (this.loading || !this.hasMore) return
      this.loading = true
      try {
        const data = await get('/recipe', { goal: this.goal || undefined, page: this.page + 1, pageSize: 20 })
        const list = data.list || []
        if (list.length) {
          this.list = this.list.concat(list)
          this.page += 1
          this.hasMore = this.list.length < (data.total || 0)
        } else {
          this.hasMore = false
        }
      } catch (e) { /* 忽略 */ }
      this.loading = false
    },
    goDetail(r) {
      uni.navigateTo({ url: '/pages/recipe/detail?id=' + r.id })
    }
  }
}
</script>

<style>
.tabs {
  display: flex;
  gap: 20rpx;
  margin-bottom: 24rpx;
}
.tab {
  padding: 12rpx 36rpx;
  border-radius: 999rpx;
  background: #fff;
  font-size: 26rpx;
  color: #4b5563;
}
.tab.active {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-weight: 600;
}
.recipe-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.r-cover {
  height: 320rpx;
  border-radius: 16rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #fde68a, #fbbf24);
}
.r-cover-img {
  width: 100%;
  height: 100%;
}
.r-cover-ico {
  font-size: 100rpx;
}
.r-name {
  font-size: 30rpx;
  font-weight: 600;
  margin-top: 20rpx;
}
.r-meta {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
.load-tip {
  text-align: center;
  padding: 24rpx 0;
  font-size: 24rpx;
  color: #9ca3af;
}
</style>
