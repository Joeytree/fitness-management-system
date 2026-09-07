<template>
  <view class="login-page">
    <view class="brand">
      <view class="logo">💪</view>
      <view class="app-name">多功能健身</view>
      <view class="slogan">课程学习 · 饮食管理 · 健康监测</view>
    </view>

    <view class="login-card">
      <view class="tabs">
        <view class="tab active">密码登录</view>
      </view>

      <input
        class="input"
        v-model="phone"
        type="number"
        placeholder="请输入手机号"
        maxlength="11"
      />
      <input
        class="input"
        v-model="password"
        type="password"
        placeholder="请输入密码"
      />

      <button class="btn-primary login-btn" :loading="loading" @click="doLogin">登 录</button>

      <view class="wx-btn" @click="wxLogin">
        <text class="wx-icon">💬</text>
        <text>微信一键登录</text>
      </view>

      <view class="footer">
        <text class="muted" @click="goRegister">没有账号？去注册</text>
      </view>
    </view>

    <view class="tip muted">演示账号：13800000001 / 123456</view>
  </view>
</template>

<script>
import { post } from '../../utils/request'
import { setLogin } from '../../utils/index'

export default {
  data() {
    return {
      phone: '13800000001',
      password: '',
      loading: false
    }
  },
  methods: {
    async doLogin() {
      if (!this.phone || !this.password) {
        uni.showToast({ title: '请输入手机号和密码', icon: 'none' })
        return
      }
      this.loading = true
      try {
        const data = await post('/auth/login', { phone: this.phone, password: this.password })
        setLogin(data.token, data.user)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => {
          uni.switchTab({ url: '/pages/home/index' })
        }, 500)
      } catch (e) {
        // 已提示
      } finally {
        this.loading = false
      }
    },
    wxLogin() {
      // #ifdef MP-WEIXIN
      uni.login({
        provider: 'weixin',
        success: async (loginRes) => {
          try {
            const data = await post('/auth/wxlogin', { code: loginRes.code })
            setLogin(data.token, data.user)
            uni.showToast({ title: '登录成功', icon: 'success' })
            setTimeout(() => {
              uni.switchTab({ url: '/pages/home/index' })
            }, 500)
          } catch (e) { /* 已提示 */ }
        },
        fail: () => {
          uni.showToast({ title: '微信登录失败，请重试', icon: 'none' })
        }
      })
      // #endif
      // #ifndef MP-WEIXIN
      uni.showToast({ title: '请在微信小程序中体验微信登录', icon: 'none' })
      // #endif
    },
    goRegister() {
      uni.navigateTo({ url: '/pages/register/index' })
    }
  }
}
</script>

<style>
.login-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #d1fae5 0%, #f6faf8 45%);
  padding: 100rpx 48rpx;
  box-sizing: border-box;
}
.brand {
  text-align: center;
  margin-bottom: 60rpx;
}
.logo {
  font-size: 100rpx;
}
.app-name {
  font-size: 44rpx;
  font-weight: 700;
  color: #065f46;
  margin-top: 16rpx;
}
.slogan {
  font-size: 26rpx;
  color: #10b981;
  margin-top: 12rpx;
}
.login-card {
  background: #ffffff;
  border-radius: 32rpx;
  padding: 48rpx 40rpx;
  box-shadow: 0 12rpx 40rpx rgba(16, 185, 129, 0.12);
}
.tabs {
  display: flex;
  margin-bottom: 32rpx;
}
.tab {
  font-size: 32rpx;
  font-weight: 600;
  color: #065f46;
  border-bottom: 6rpx solid #10b981;
  padding-bottom: 16rpx;
}
.input {
  background: #f3f4f6;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  font-size: 30rpx;
  margin-bottom: 24rpx;
}
.login-btn {
  width: 100%;
  padding: 24rpx 0;
  font-size: 32rpx;
  border: none;
  margin-top: 12rpx;
}
.login-btn::after {
  border: none;
}
.wx-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 24rpx 0;
  border-radius: 20rpx;
  background: #ecfdf5;
  color: #065f46;
  font-size: 28rpx;
  font-weight: 500;
}
.wx-icon {
  font-size: 32rpx;
}
.footer {
  text-align: center;
  margin-top: 28rpx;
}
.tip {
  text-align: center;
  margin-top: 40rpx;
}
</style>
