<template>
  <view class="page-pad" v-if="action.id">
    <view class="head-card">
      <view class="name">{{ action.name }}</view>
      <view class="meta">
        <text class="pill">{{ action.part }}</text>
        <text class="muted">难度 {{ action.difficulty }} · 器械：{{ action.equipment || '无' }}</text>
      </view>
    </view>

    <view v-if="action.mediaUrl" class="media">
      <image v-if="action.mediaType !== 2" :src="fullUrl(action.mediaUrl)" mode="widthFix" lazy-load />
      <video v-else :src="fullUrl(action.mediaUrl)" controls />
    </view>

    <view class="block">
      <view class="block-title">动作步骤</view>
      <view class="block-text">{{ action.steps }}</view>
    </view>
    <view v-if="action.tips" class="block">
      <view class="block-title">动作要点</view>
      <view class="block-text">{{ action.tips }}</view>
    </view>
    <view v-if="action.breath" class="block">
      <view class="block-title">呼吸方法</view>
      <view class="block-text">{{ action.breath }}</view>
    </view>
    <view v-if="action.errors" class="block warn">
      <view class="block-title warn-title">常见错误</view>
      <view class="block-text">{{ action.errors }}</view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      id: null,
      action: {}
    }
  },
  onLoad(options) {
    this.id = options.id
    this.load()
  },
  methods: {
    fullUrl,
    async load() {
      try {
        this.action = await get('/action/' + this.id)
      } catch (e) { /* 忽略 */ }
    }
  }
}
</script>

<style>
.head-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 32rpx;
  margin-bottom: 20rpx;
}
.name {
  font-size: 40rpx;
  font-weight: 700;
}
.meta {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.media {
  border-radius: 24rpx;
  overflow: hidden;
  margin-bottom: 20rpx;
}
.media image {
  width: 100%;
}
.media video {
  width: 100%;
}
.block {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.block.warn {
  border-left: 8rpx solid #ef4444;
}
.block-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #10b981;
  margin-bottom: 16rpx;
}
.warn-title {
  color: #ef4444;
}
.block-text {
  font-size: 28rpx;
  color: #4b5563;
  line-height: 1.8;
}
</style>
