<template>
  <view class="page-pad">
    <view class="tabs">
      <view
        v-for="t in types"
        :key="t.value"
        class="tab"
        :class="{ active: type === t.value }"
        @click="selectType(t.value)"
      >{{ t.label }}</view>
    </view>

    <view class="list">
      <view v-for="a in list" :key="a.id" class="article-item" @click="goDetail(a)">
        <view class="a-title">{{ a.title }}</view>
        <view class="a-meta muted">{{ typeText(a.type) }} · {{ a.createTime }}</view>
      </view>
      <view v-if="!list.length" class="empty muted">暂无文章</view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { dict } from '../../utils/index'

export default {
  data() {
    return {
      type: 0,
      types: [
        { value: 0, label: '全部' },
        { value: 1, label: '饮食知识' },
        { value: 2, label: '资讯' },
        { value: 3, label: '公告' }
      ],
      list: []
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    typeText(t) {
      return dict.articleType[t] || ''
    },
    selectType(t) {
      this.type = t
      this.load()
    },
    async load() {
      try {
        const data = await get('/article', { type: this.type || undefined, page: 1, pageSize: 20 })
        this.list = data.list || []
      } catch (e) { /* 忽略 */ }
    },
    goDetail(a) {
      uni.navigateTo({ url: '/pages/knowledge/detail?id=' + a.id })
    }
  }
}
</script>

<style>
.tabs {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
  flex-wrap: wrap;
}
.tab {
  padding: 12rpx 32rpx;
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
.article-item {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 16rpx;
}
.a-title {
  font-size: 28rpx;
  font-weight: 600;
  line-height: 1.5;
}
.a-meta {
  margin-top: 12rpx;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
