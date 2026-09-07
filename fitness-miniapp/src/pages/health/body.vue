<template>
  <view class="page-pad">
    <view class="card">
      <view class="form-row">
        <text class="label">体重(kg)</text>
        <input class="input" v-model="form.weight" type="digit" placeholder="必填" />
      </view>
      <view class="form-row">
        <text class="label">体脂率(%)</text>
        <input class="input" v-model="form.bodyFat" type="digit" placeholder="选填" />
      </view>
      <view class="form-row">
        <text class="label">腰围(cm)</text>
        <input class="input" v-model="form.waist" type="digit" placeholder="选填" />
      </view>
      <view class="form-row">
        <text class="label">胸围(cm)</text>
        <input class="input" v-model="form.chest" type="digit" placeholder="选填" />
      </view>
      <view class="form-row">
        <text class="label">臀围(cm)</text>
        <input class="input" v-model="form.hip" type="digit" placeholder="选填" />
      </view>
    </view>

    <button class="btn-primary save-btn" @click="save">保存（自动计算 BMI）</button>
  </view>
</template>

<script>
import { post } from '../../utils/request'

export default {
  data() {
    return {
      form: { weight: '', bodyFat: '', waist: '', chest: '', hip: '' }
    }
  },
  methods: {
    async save() {
      if (!this.form.weight) {
        uni.showToast({ title: '请输入体重', icon: 'none' })
        return
      }
      const body = {}
      Object.keys(this.form).forEach(k => {
        if (this.form[k] !== '') body[k] = Number(this.form[k])
      })
      try {
        await post('/health/body', body)
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
  padding: 26rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}
.form-row:last-child {
  border-bottom: none;
}
.label {
  width: 180rpx;
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
</style>
