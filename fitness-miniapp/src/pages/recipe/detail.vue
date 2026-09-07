<template>
  <view class="page-pad" v-if="recipe.id">
    <view class="cover" :style="{ background: coverGrad(recipe.goal) }">
      <text class="cover-ico">🍱</text>
    </view>
    <view class="head-card">
      <view class="name">{{ recipe.name }}</view>
      <view class="meta">
        <text class="pill">{{ goalText(recipe.goal) }}</text>
        <view class="fav" @click="toggleFav">{{ favorited ? '🌟 已收藏' : '☆ 收藏' }}</view>
      </view>
    </view>

    <view class="nutri-row">
      <view class="nutri-item">
        <text class="n-val">{{ recipe.calorie }}</text>
        <text class="n-label">千卡</text>
      </view>
      <view class="nutri-item">
        <text class="n-val">{{ recipe.protein }}g</text>
        <text class="n-label">蛋白质</text>
      </view>
      <view class="nutri-item">
        <text class="n-val">{{ recipe.carb }}g</text>
        <text class="n-label">碳水</text>
      </view>
      <view class="nutri-item">
        <text class="n-val">{{ recipe.fat }}g</text>
        <text class="n-label">脂肪</text>
      </view>
    </view>

    <view class="block">
      <view class="block-title">所需食材</view>
      <view class="ingredient-row" v-for="(item, i) in ingredients" :key="i">
        <text class="ing-idx">{{ i + 1 }}</text>
        <text class="ing-name">{{ item.name }}</text>
        <text class="ing-amount muted">{{ item.amount }}</text>
      </view>
    </view>

    <view class="block">
      <view class="block-title">做法</view>
      <view class="block-text">{{ recipe.content }}</view>
    </view>
  </view>
</template>

<script>
import { get, post, del } from '../../utils/request'
import { goalText } from '../../utils/index'

const GRADS = {
  1: 'linear-gradient(135deg,#3b82f6,#1d4ed8)',
  2: 'linear-gradient(135deg,#f97316,#ea580c)',
  3: 'linear-gradient(135deg,#a855f7,#7e22ce)'
}

export default {
  data() {
    return {
      id: null,
      recipe: {},
      favorited: false,
      ingredients: []
    }
  },
  onLoad(options) {
    this.id = options.id
    this.load()
  },
  methods: {
    goalText,
    coverGrad(g) {
      return GRADS[g] || 'linear-gradient(135deg,#10b981,#059669)'
    },
    async load() {
      try {
        this.recipe = await get('/recipe/' + this.id)
        // 用 recipe.content 解析食材（如果以"食材"开头）
        const lines = (this.recipe.content || '').split(/\r?\n/).filter(Boolean)
        const ingStart = lines.findIndex(l => /^食材[:：]/.test(l))
        const cookStart = lines.findIndex(l => /^做法[:：]/.test(l))
        if (ingStart >= 0 && cookStart > ingStart) {
          this.ingredients = lines.slice(ingStart + 1, cookStart).map(parseLine)
          this.recipe.content = lines.slice(cookStart + 1).join('\n').trim()
        } else {
          this.ingredients = [
            { name: '主食材', amount: '适量' },
            { name: '配料', amount: '少许' }
          ]
        }
      } catch (e) {}
    },
    toggleFav() {
      this.favorited = !this.favorited
      uni.showToast({ title: this.favorited ? '已收藏' : '已取消', icon: 'none' })
    }
  }
}

function parseLine(line) {
  const m = line.match(/^[\d.、\s]+(.+?)\s+(\d+.*)$/)
  if (m) return { name: m[1], amount: m[2] }
  return { name: line, amount: '' }
}
</script>

<style>
.cover {
  height: 280rpx;
  border-radius: 32rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.cover-ico {
  font-size: 120rpx;
}
.head-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-top: -40rpx;
  position: relative;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.name {
  font-size: 40rpx;
  font-weight: 700;
}
.meta {
  margin-top: 16rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.fav {
  font-size: 26rpx;
  color: #10b981;
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
  background: #ecfdf5;
}
.nutri-row {
  display: flex;
  gap: 12rpx;
  margin: 20rpx 0;
}
.nutri-item {
  flex: 1;
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.n-val {
  font-size: 32rpx;
  font-weight: 700;
  color: #10b981;
}
.n-label {
  font-size: 22rpx;
  color: #9ca3af;
  margin-top: 6rpx;
}
.block {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
}
.block-title {
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 20rpx;
}
.ingredient-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 18rpx 0;
  border-bottom: 1rpx dashed #f3f4f6;
}
.ingredient-row:last-child {
  border-bottom: none;
}
.ing-idx {
  width: 48rpx;
  height: 48rpx;
  border-radius: 50%;
  background: #ecfdf5;
  color: #10b981;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.ing-name {
  flex: 1;
  font-size: 28rpx;
  color: #1f2937;
}
.ing-amount {
  font-size: 24rpx;
}
.block-text {
  font-size: 28rpx;
  color: #374151;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>