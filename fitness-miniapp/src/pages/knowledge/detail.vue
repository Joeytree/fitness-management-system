<template>
  <view class="page-pad" v-if="article.id">
    <view class="title">{{ article.title }}</view>
    <view class="meta muted">{{ typeText(article.type) }} · {{ article.createTime }}</view>
    <view class="body">{{ article.content }}</view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { dict } from '../../utils/index'

export default {
  data() {
    return {
      id: null,
      article: {}
    }
  },
  onLoad(options) {
    this.id = options.id
    this.load()
  },
  methods: {
    typeText(t) {
      return dict.articleType[t] || ''
    },
    async load() {
      try {
        this.article = await get('/article/' + this.id)
      } catch (e) { /* 忽略 */ }
    }
  }
}
</script>

<style>
.title {
  font-size: 40rpx;
  font-weight: 700;
  line-height: 1.5;
}
.meta {
  margin: 20rpx 0 32rpx;
}
.body {
  font-size: 30rpx;
  color: #374151;
  line-height: 1.9;
  white-space: pre-wrap;
}
</style>
