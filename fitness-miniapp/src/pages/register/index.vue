<template>
  <view class="page-pad">
    <view class="card">
      <input class="input" v-model="phone" type="number" placeholder="请输入手机号" maxlength="11" />
      <input class="input" v-model="password" type="password" placeholder="密码（至少6位）" />
      <input class="input" v-model="confirmPwd" type="password" placeholder="确认密码" />
      <input class="input" v-model="nickname" placeholder="昵称（选填，默认自动生成）" />
      <button class="btn-primary reg-btn" :loading="loading" @click="doRegister">注 册</button>
    </view>
  </view>
</template>

<script>
import { post } from '../../utils/request'

export default {
  data() {
    return {
      phone: '',
      password: '',
      confirmPwd: '',
      nickname: '',
      loading: false
    }
  },
  methods: {
    async doRegister() {
      if (!/^1\d{10}$/.test(this.phone)) {
        uni.showToast({ title: '手机号格式不正确', icon: 'none' })
        return
      }
      if (this.password.length < 6) {
        uni.showToast({ title: '密码至少6位', icon: 'none' })
        return
      }
      if (this.password !== this.confirmPwd) {
        uni.showToast({ title: '两次密码不一致', icon: 'none' })
        return
      }
      this.loading = true
      try {
        await post('/auth/register', {
          phone: this.phone,
          password: this.password,
          nickname: this.nickname || undefined
        })
        uni.showToast({ title: '注册成功，请登录', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack()
        }, 800)
      } catch (e) {
        // 已提示
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style>
.input {
  background: #f3f4f6;
  border-radius: 20rpx;
  padding: 24rpx 28rpx;
  font-size: 30rpx;
  margin-bottom: 24rpx;
}
.reg-btn {
  width: 100%;
  padding: 24rpx 0;
  font-size: 32rpx;
  border: none;
  margin-top: 12rpx;
}
.reg-btn::after {
  border: none;
}
</style>
