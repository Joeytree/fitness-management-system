<template>
  <view class="page-pad">
    <view class="search-bar">
      <text class="search-ico">🔍</text>
      <input
        class="search-input"
        v-model="keyword"
        placeholder="搜索食物 / 课程 / 动作 / 知识库"
        confirm-type="search"
        focus
        @confirm="load"
        @input="onInput"
      />
      <text v-if="keyword" class="clear-btn" @click="clear">✕</text>
    </view>

    <view v-if="searched && !hasResult" class="empty muted">未找到相关内容</view>

    <view v-if="searched && hasResult">
      <view v-if="result.actions && result.actions.length" class="group">
        <view class="group-title">动作</view>
        <view v-for="a in result.actions" :key="a.id" class="item" @click="go('/pages/action/detail?id=' + a.id)">
          <view class="item-main">
            <view class="item-name">{{ a.name }}</view>
            <view class="item-meta muted">{{ a.part }} · {{ a.equipment || '徒手' }}</view>
          </view>
          <text class="arrow">›</text>
        </view>
      </view>

      <view v-if="result.courses && result.courses.length" class="group">
        <view class="group-title">课程</view>
        <view v-for="c in result.courses" :key="c.id" class="item" @click="go('/pages/course/detail?id=' + c.id)">
          <view class="item-main">
            <view class="item-name">{{ c.name }}</view>
            <view class="item-meta muted">{{ c.category }} · {{ c.duration }} 分钟</view>
          </view>
          <text class="arrow">›</text>
        </view>
      </view>

      <view v-if="result.foods && result.foods.length" class="group">
        <view class="group-title">食物</view>
        <view v-for="f in result.foods" :key="f.id" class="item" @click="goFood(f)">
          <view class="item-main">
            <view class="item-name">{{ f.name }}</view>
            <view class="item-meta muted">{{ f.category }} · 每100g {{ f.calorie }} 千卡</view>
          </view>
          <text class="arrow">›</text>
        </view>
      </view>

      <view v-if="result.articles && result.articles.length" class="group">
        <view class="group-title">知识库</view>
        <view v-for="ar in result.articles" :key="ar.id" class="item" @click="go('/pages/knowledge/detail?id=' + ar.id)">
          <view class="item-main">
            <view class="item-name">{{ ar.title }}</view>
            <view class="item-meta muted">资讯文章</view>
          </view>
          <text class="arrow">›</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'

export default {
  data() {
    return {
      keyword: '',
      result: {},
      searched: false
    }
  },
  computed: {
    hasResult() {
      const r = this.result || {}
      return ['actions', 'courses', 'foods', 'articles'].some(k => (r[k] || []).length)
    }
  },
  methods: {
    onInput() {
      if (!this.keyword) {
        this.searched = false
        this.result = {}
      }
    },
    clear() {
      this.keyword = ''
      this.searched = false
      this.result = {}
    },
    async load() {
      if (!this.keyword.trim()) return
      try {
        this.result = await get('/search', { keyword: this.keyword.trim() })
        this.searched = true
      } catch (e) { /* 忽略 */ }
    },
    goFood(f) {
      uni.navigateTo({
        url: `/pages/diet/record?foodId=${f.id}&foodName=${encodeURIComponent(f.name)}&mealType=1`
      })
    },
    go(url) {
      uni.navigateTo({ url })
    }
  }
}
</script>

<style>
.search-bar {
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: #fff;
  border-radius: 999rpx;
  padding: 18rpx 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.05);
  margin-bottom: 24rpx;
}
.search-ico {
  font-size: 30rpx;
  color: #9ca3af;
}
.search-input {
  flex: 1;
  font-size: 28rpx;
}
.clear-btn {
  color: #9ca3af;
  font-size: 30rpx;
  padding: 0 8rpx;
}
.group {
  margin-bottom: 16rpx;
}
.group-title {
  font-size: 28rpx;
  font-weight: 700;
  margin: 24rpx 8rpx 16rpx;
}
.item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 14rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.05);
}
.item-main {
  flex: 1;
  margin-right: 16rpx;
}
.item-name {
  font-size: 28rpx;
  font-weight: 600;
}
.item-meta {
  margin-top: 6rpx;
  font-size: 24rpx;
}
.arrow {
  color: #d1d5db;
  font-size: 32rpx;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
