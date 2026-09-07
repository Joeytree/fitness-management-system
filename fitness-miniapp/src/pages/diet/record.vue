<template>
  <view class="page-pad">
    <view class="card">
      <view class="form-row" @click="goSearch">
        <text class="label">食物</text>
        <text class="value" :class="{ muted: !foodName }">{{ foodName || '选择食物' }}</text>
        <text class="arrow">›</text>
      </view>
      <view class="form-row">
        <text class="label">摄入量(g)</text>
        <input class="input" v-model="amount" type="number" placeholder="如：200" />
      </view>
      <view class="form-row">
        <text class="label">餐次</text>
        <view class="radio-group">
          <text
            v-for="m in meals"
            :key="m.type"
            class="radio"
            :class="{ on: mealType === m.type }"
            @click="mealType = m.type"
          >{{ m.label }}</text>
        </view>
      </view>
    </view>

    <button class="btn-primary save-btn" @click="save">保存记录</button>
  </view>
</template>

<script>
import { post } from '../../utils/request'

export default {
  data() {
    return {
      foodId: null,
      foodName: '',
      amount: 100,
      mealType: 1,
      meals: [
        { type: 1, label: '早餐' },
        { type: 2, label: '午餐' },
        { type: 3, label: '晚餐' },
        { type: 4, label: '加餐' }
      ]
    }
  },
  onLoad(options) {
    if (options.foodId) this.foodId = Number(options.foodId)
    if (options.foodName) this.foodName = decodeURIComponent(options.foodName)
    if (options.mealType) this.mealType = Number(options.mealType)
  },
  methods: {
    goSearch() {
      uni.navigateTo({ url: '/pages/diet/search?mealType=' + this.mealType })
    },
    async save() {
      if (!this.foodId) {
        uni.showToast({ title: '请选择食物', icon: 'none' })
        return
      }
      if (!this.amount) {
        uni.showToast({ title: '请输入摄入量', icon: 'none' })
        return
      }
      try {
        await post('/diet/record', {
          foodId: this.foodId,
          amount: Number(this.amount),
          mealType: this.mealType
        })
        uni.showToast({ title: '记录成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 600)
      } catch (e) { /* 已提示 */ }
    }
  }
}
</script>

<style>
.card {
  background: #fff;
  border-radius: 28rpx;
  padding: 8rpx 28rpx;
}
.form-row {
  display: flex;
  align-items: center;
  padding: 28rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}
.form-row:last-child {
  border-bottom: none;
}
.label {
  width: 160rpx;
  font-size: 28rpx;
  color: #6b7280;
}
.value {
  flex: 1;
  font-size: 28rpx;
  color: #1f2937;
}
.arrow {
  color: #d1d5db;
  font-size: 32rpx;
}
.input {
  flex: 1;
  font-size: 28rpx;
}
.radio-group {
  flex: 1;
  display: flex;
  gap: 16rpx;
}
.radio {
  padding: 10rpx 28rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  font-size: 26rpx;
  color: #4b5563;
}
.radio.on {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
}
.save-btn {
  width: 100%;
  padding: 24rpx 0;
  font-size: 32rpx;
  border: none;
  margin-top: 40rpx;
}
.save-btn::after {
  border: none;
}
</style>
