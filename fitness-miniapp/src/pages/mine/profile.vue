<template>
  <view class="page-pad">
    <view class="card">
      <view class="form-row">
        <text class="label">头像 URL</text>
        <input class="input" v-model="form.avatar" placeholder="头像图片链接（选填）" />
      </view>
      <view class="form-row">
        <text class="label">昵称</text>
        <input class="input" v-model="form.nickname" />
      </view>
      <view class="form-row">
        <text class="label">个人简介</text>
        <input class="input" v-model="form.intro" placeholder="一句话介绍自己（选填）" />
      </view>
      <view class="form-row">
        <text class="label">生日</text>
        <picker mode="date" :value="form.birthday || '2000-01-01'" @change="onBirthday">
          <view class="picker-value">{{ form.birthday || '选择日期' }} ▾</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="label">性别</text>
        <view class="radio-group">
          <text class="radio" :class="{ on: form.gender === 1 }" @click="form.gender = 1">男</text>
          <text class="radio" :class="{ on: form.gender === 2 }" @click="form.gender = 2">女</text>
        </view>
      </view>
      <view class="form-row">
        <text class="label">身高(cm)</text>
        <input class="input" v-model="form.height" type="digit" />
      </view>
      <view class="form-row">
        <text class="label">健身目标</text>
        <view class="radio-group">
          <text
            v-for="g in goals"
            :key="g.value"
            class="radio"
            :class="{ on: form.goal === g.value }"
            @click="form.goal = g.value"
          >{{ g.label }}</text>
        </view>
      </view>
      <view class="form-row">
        <text class="label">训练水平</text>
        <view class="radio-group">
          <text
            v-for="l in levels"
            :key="l.value"
            class="radio"
            :class="{ on: form.level === l.value }"
            @click="form.level = l.value"
          >{{ l.label }}</text>
        </view>
      </view>
    </view>

    <button class="btn-primary save-btn" @click="save">保存（更新推荐标签）</button>
  </view>
</template>

<script>
import { get, put } from '../../utils/request'
import { setLogin, getUserInfo } from '../../utils/index'

export default {
  data() {
    return {
      form: { nickname: '', avatar: '', intro: '', birthday: '', gender: 1, height: 170, goal: 1, level: 1 },
      goals: [
        { value: 1, label: '增肌' },
        { value: 2, label: '减脂' },
        { value: 3, label: '塑形' },
        { value: 4, label: '保持' }
      ],
      levels: [
        { value: 1, label: '新手' },
        { value: 2, label: '初级' },
        { value: 3, label: '进阶' }
      ]
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    async load() {
      try {
        this.form = await get('/user/profile')
      } catch (e) { /* 忽略 */ }
    },
    onBirthday(e) {
      this.form.birthday = e.detail.value
    },
    async save() {
      try {
        const data = await put('/user/profile', {
          nickname: this.form.nickname,
          avatar: this.form.avatar || undefined,
          intro: this.form.intro || undefined,
          birthday: this.form.birthday || undefined,
          gender: this.form.gender,
          height: Number(this.form.height),
          goal: this.form.goal,
          level: this.form.level
        })
        const token = uni.getStorageSync('token')
        setLogin(token, data)
        uni.showToast({ title: '保存成功', icon: 'success' })
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
  width: 160rpx;
  font-size: 28rpx;
  color: #6b7280;
}
.input {
  flex: 1;
  font-size: 28rpx;
}
.radio-group {
  flex: 1;
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}
.radio {
  padding: 8rpx 28rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  font-size: 24rpx;
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
