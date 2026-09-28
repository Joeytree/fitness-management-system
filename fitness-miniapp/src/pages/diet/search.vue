<template>
  <view class="page-pad">
    <view class="search-bar">
      <input class="search-input" v-model="keyword" placeholder="搜索食物" confirm-type="search" @confirm="load" />
    </view>

    <scroll-view scroll-x class="cat-scroll">
      <view
        v-for="c in cats"
        :key="c"
        class="cat-item"
        :class="{ active: category === c }"
        @click="selectCat(c)"
      >{{ c }}</view>
    </scroll-view>

    <view class="list">
      <view v-for="f in list" :key="f.id" class="food-item" @click="goRecord(f)">
        <view>
          <view class="f-name">{{ f.name }}</view>
          <view class="f-meta muted">{{ f.category }} · 每100g {{ f.calorie }}千卡</view>
        </view>
        <text class="f-arrow">›</text>
      </view>
      <view v-if="!list.length" class="empty muted">暂无食物</view>
      <view v-else class="load-tip">{{ loading ? '加载中…' : (hasMore ? '上拉加载更多' : '没有更多了') }}</view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { FOOD_CATS } from '../../utils/index'

export default {
  data() {
    return {
      cats: ['全部', ...FOOD_CATS],
      category: '全部',
      keyword: '',
      mealType: 1,
      list: [],
      page: 1,
      hasMore: true,
      loading: false
    }
  },
  onLoad(options) {
    this.mealType = options.mealType || 1
    this.load()
  },
  onReachBottom() {
    this.loadMore()
  },
  methods: {
    selectCat(c) {
      this.category = c
      this.load()
    },
    async load() {
      this.page = 1
      this.hasMore = true
      this.loading = false
      try {
        const data = await get('/food', {
          keyword: this.keyword || undefined,
          category: this.category === '全部' ? undefined : this.category,
          page: 1,
          pageSize: 20
        })
        this.list = data.list || []
        this.hasMore = this.list.length < (data.total || 0)
      } catch (e) { /* 忽略 */ }
    },
    async loadMore() {
      if (this.loading || !this.hasMore) return
      this.loading = true
      try {
        const data = await get('/food', {
          keyword: this.keyword || undefined,
          category: this.category === '全部' ? undefined : this.category,
          page: this.page + 1,
          pageSize: 20
        })
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
    goRecord(f) {
      uni.navigateTo({ url: `/pages/diet/record?foodId=${f.id}&foodName=${encodeURIComponent(f.name)}&mealType=${this.mealType}` })
    }
  }
}
</script>

<style>
.search-bar {
  background: #fff;
  border-radius: 24rpx;
  padding: 12rpx 20rpx;
  margin-bottom: 20rpx;
}
.search-input {
  font-size: 28rpx;
  padding: 12rpx 0;
}
.cat-scroll {
  white-space: nowrap;
  margin-bottom: 24rpx;
}
.cat-item {
  display: inline-block;
  padding: 12rpx 32rpx;
  margin-right: 16rpx;
  border-radius: 999rpx;
  background: #fff;
  font-size: 26rpx;
  color: #4b5563;
}
.cat-item.active {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-weight: 600;
}
.food-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 20rpx;
  padding: 26rpx 28rpx;
  margin-bottom: 16rpx;
}
.f-name {
  font-size: 28rpx;
  font-weight: 600;
}
.f-meta {
  margin-top: 6rpx;
}
.f-arrow {
  color: #d1d5db;
  font-size: 32rpx;
}
.empty {
  text-align: center;
  padding: 80rpx 0;
}
.load-tip {
  text-align: center;
  padding: 24rpx 0;
  font-size: 24rpx;
  color: #9ca3af;
}
</style>
