<template>
  <view class="page-pad">
    <view class="card">
      <view class="form-row">
        <text class="label">动作名称 *</text>
        <input class="input" v-model="form.name" placeholder="如：自重臂屈伸" />
      </view>
      <view class="form-row">
        <text class="label">训练部位</text>
        <picker :range="parts" @change="onPart">
          <view class="picker-value">{{ form.part }} ▾</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="label">所需器械</text>
        <input class="input" v-model="form.equipment" placeholder="如：哑铃 / 双杠 / 徒手" />
      </view>
      <view class="form-row col">
        <text class="label">文字步骤 *</text>
        <textarea class="textarea" v-model="form.steps" placeholder="按顺序描述动作过程" />
      </view>
      <view class="form-row col">
        <text class="label">动作要点</text>
        <textarea class="textarea" v-model="form.tips" placeholder="选填" />
      </view>
      <view class="form-row col">
        <text class="label">常见错误</text>
        <textarea class="textarea" v-model="form.errors" placeholder="选填" />
      </view>
      <view class="form-row">
        <text class="label">呼吸方法</text>
        <input class="input" v-model="form.breath" placeholder="选填" />
      </view>
    </view>

    <button class="btn-primary submit-btn" @click="submit">提交审核</button>
    <view class="tip muted">提交后由管理员审核，通过后展示给所有用户</view>
  </view>
</template>

<script>
import { post } from '../../utils/request'
import { PARTS } from '../../utils/index'

export default {
  data() {
    return {
      parts: PARTS,
      form: { name: '', part: '胸', equipment: '', steps: '', tips: '', errors: '', breath: '' }
    }
  },
  methods: {
    onPart(e) {
      this.form.part = this.parts[Number(e.detail.value)]
    },
    async submit() {
      if (!this.form.name || !this.form.steps) {
        uni.showToast({ title: '动作名称与步骤为必填', icon: 'none' })
        return
      }
      try {
        await post('/action/ugc', this.form)
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
