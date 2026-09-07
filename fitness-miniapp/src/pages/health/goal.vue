<template>
  <view class="page-pad">
    <view class="card">
      <view class="form-row">
        <text class="label">目标体重(kg)</text>
        <input class="input" v-model="form.targetWeight" type="digit" placeholder="如：65" />
      </view>
      <view class="form-row">
        <text class="label">目标体脂(%)</text>
        <input class="input" v-model="form.targetBodyFat" type="digit" placeholder="如：15" />
      </view>
    </view>
    <button class="btn-primary save-btn" @click="save">保存目标</button>

    <view v-if="progress.targetWeight" class="progress-card">
      <view class="p-title">达成进度</view>
      <view class="p-row">目标体重：{{ progress.targetWeight }}kg</view>
      <view class="p-row">当前体重：{{ progress.currentWeight || '--' }}kg</view>
      <view v-if="progress.weightDiff !== undefined && progress.weightDiff !== null" class="p-row">
        差距：{{ Math.abs(progress.weightDiff) }}kg
      </view>
    </view>
  </view>
</template>

<script>
import { get, put } from '../../utils/request'

export default {
  data() {
    return {
      form: { targetWeight: '', targetBodyFat: '' },
      progress: {}
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    async load() {
      try {
        this.progress = await get('/health/goal')
      } catch (e) { /* 忽略 */ }
    },
    async save() {
      const body = {}
      if (this.form.targetWeight) body.targetWeight = Number(this.form.targetWeight)
      if (this.form.targetBodyFat) body.targetBodyFat = Number(this.form.targetBodyFat)
      try {
        await put('/health/goal', body)
        uni.showToast({ title: '目标已保存', icon: 'success' })
        this.load()
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
  padding: 26rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}
.form-row:last-child {
  border-bottom: none;
}
.label {
  width: 200rpx;
  font-size: 28rpx;
  color: #6b7280;
}
.input {
  flex: 1;
  font-size: 28rpx;
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
.progress-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 32rpx;
  margin-top: 32rpx;
}
.p-title {
  font-size: 30rpx;
  font-weight: 700;
  margin-bottom: 16rpx;
}
.p-row {
  font-size: 28rpx;
  color: #4b5563;
  margin: 10rpx 0;
}
</style>
