<template>
  <view class="page-pad">
    <view class="card">
      <view class="form-row">
        <text class="label">头像</text>
        <view class="avatar-box" @click="chooseAvatar">
          <image v-if="form.avatar" class="avatar-img" :src="fullUrl(form.avatar)" mode="aspectFill" />
          <view v-else class="avatar-empty">＋</view>
        </view>
        <text class="avatar-tip muted">点击更换</text>
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
      <view class="form-row">
        <text class="label">偏好部位</text>
        <view class="radio-group">
          <text
            v-for="p in PARTS"
            :key="p"
            class="radio"
            :class="{ on: selectedParts.includes(p) }"
            @click="togglePart(p)"
          >{{ p }}</text>
        </view>
      </view>
    </view>

    <button class="btn-primary save-btn" @click="save">保存（更新推荐标签）</button>
  </view>
</template>

<script>
import { get, put } from '../../utils/request'
import { setLogin, getUserInfo, PARTS } from '../../utils/index'
import { uploadFile } from '../../utils/upload'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      form: { nickname: '', avatar: '', intro: '', birthday: '', gender: 1, height: 170, goal: 1, level: 1 },
      PARTS,
      selectedParts: [],
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
    fullUrl,
    async load() {
      try {
        const data = await get('/user/profile')
        this.form = data
        this.selectedParts = (data.preferParts || '').split(',').map(s => s.trim()).filter(Boolean)
      } catch (e) { /* 忽略 */ }
    },
    onBirthday(e) {
      this.form.birthday = e.detail.value
    },
    togglePart(p) {
      const i = this.selectedParts.indexOf(p)
      if (i >= 0) this.selectedParts.splice(i, 1)
      else this.selectedParts.push(p)
    },
    chooseAvatar() {
      uni.chooseImage({
        count: 1,
        success: async (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0]
          if (!path) return
          try {
            this.form.avatar = await uploadFile(path)
          } catch (e) { /* 已提示 */ }
        }
      })
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
          level: this.form.level,
          preferParts: this.selectedParts.join(',')
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
.avatar-box {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  overflow: hidden;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.avatar-img {
  width: 100%;
  height: 100%;
}
.avatar-empty {
  font-size: 56rpx;
  color: #c0c4cc;
}
.avatar-tip {
  margin-left: 20rpx;
  font-size: 24rpx;
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
