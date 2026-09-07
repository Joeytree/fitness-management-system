<template>
  <view class="page-pad">
    <view class="card">
      <view class="form-row">
        <text class="label">课程名称 *</text>
        <input class="input" v-model="form.name" placeholder="如：居家零器械全身训练" />
      </view>
      <view class="form-row">
        <text class="label">分类</text>
        <picker :range="cats" @change="onCat">
          <view class="picker-value">{{ form.category }} ▾</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="label">时长(分钟)</text>
        <input class="input" v-model="form.duration" type="number" placeholder="如：30" />
      </view>
      <view class="form-row col">
        <text class="label">课程简介</text>
        <textarea class="textarea" v-model="form.intro" placeholder="介绍课程内容与适合人群" />
      </view>
    </view>

    <button class="btn-primary submit-btn" @click="submit">提交审核</button>
    <view class="tip muted">提交后由管理员审核，通过后展示给所有用户</view>
  </view>
</template>

<script>
import { post } from '../../utils/request'
import { COURSE_CATS } from '../../utils/index'

export default {
  data() {
    return {
      cats: COURSE_CATS,
      form: { name: '', category: '塑形', duration: 30, intro: '' }
    }
  },
  methods: {
    onCat(e) {
      this.form.category = this.cats[Number(e.detail.value)]
    },
    async submit() {
      if (!this.form.name) {
        uni.showToast({ title: '课程名称为必填', icon: 'none' })
        return
      }
      try {
        await post('/course/ugc', {
          ...this.form,
          duration: Number(this.form.duration) || 30,
          difficulty: 1,
          calorie: 150
        })
        uni.showToast({ title: '提交成功，等待审核', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 800)
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
.form-row.col {
  flex-direction: column;
  align-items: flex-start;
  gap: 16rpx;
}
.label {
  width: 180rpx;
  font-size: 28rpx;
  color: #6b7280;
  flex: none;
}
.input {
  flex: 1;
  font-size: 28rpx;
}
.picker-value {
  font-size: 28rpx;
  color: #1f2937;
}
.textarea {
  width: 100%;
  min-height: 140rpx;
  font-size: 28rpx;
}
.submit-btn {
  width: 100%;
  padding: 24rpx 0;
  font-size: 32rpx;
  border: none;
  margin-top: 40rpx;
}
.submit-btn::after {
  border: none;
}
.tip {
  text-align: center;
  margin-top: 20rpx;
}
</style>
