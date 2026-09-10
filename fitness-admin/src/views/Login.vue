<template>
  <div class="login-page">
    <div class="login-brand">
      <div class="logo">
        <div class="logo-icon">F</div>
        <div>
          <h1>智慧健身系统</h1>
          <div class="sub">Fitness Management System · 管理端</div>
        </div>
      </div>
      <div class="feat">
        <div class="item"><div class="dot">📊</div><div><b>数据仪表盘</b><span>用户量 · 日活 · 课程热度 · 打卡率</span></div></div>
        <div class="item"><div class="dot">📋</div><div><b>内容管理</b><span>课程 · 动作 · 食物库 · 食谱 · 资讯</span></div></div>
        <div class="item"><div class="dot">✅</div><div><b>内容审核</b><span>动态 · 评论 · UGC 内容一键处理</span></div></div>
        <div class="item"><div class="dot">🔐</div><div><b>权限体系</b><span>角色 · 菜单 · 按钮三级 RBAC</span></div></div>
      </div>
    </div>
    <div class="login-panel">
      <div class="login-card">
        <h2>管理员登录</h2>
        <div class="tip">请输入管理员账号密码进入系统</div>
        <el-form :model="form" size="large" @submit.prevent>
          <el-form-item>
            <el-input v-model="form.username" placeholder="账号" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" show-password @keyup.enter="doLogin" />
          </el-form-item>
          <el-form-item>
            <el-checkbox v-model="form.remember">记住登录状态</el-checkbox>
          </el-form-item>
          <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">登 录</el-button>
          <div class="hint">
            演示账号：<code>admin</code> / <code>admin123</code>（超级管理员）
          </div>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { useAdminStore } from '../store/admin'

const router = useRouter()
const store = useAdminStore()
const loading = ref(false)
const form = ref({ username: 'admin', password: '', remember: true })

async function doLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入账号与密码')
    return
  }
  loading.value = true
  try {
    const data = await request.post('/admin/auth/login', form.value)
    localStorage.setItem('admin_token', data.token)
    localStorage.setItem('admin_nickname', data.admin.nickname || data.admin.username)
    ElMessage.success('登录成功，欢迎回来：' + data.admin.nickname)
    router.push('/dashboard')
  } catch (e) {
    /* 拦截器已提示 */
  } finally {
    loading.value = false
  }
}
</script>
