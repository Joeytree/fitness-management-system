<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="side-brand">
        <div class="logo-icon">F</div>
        <b>健身管理后台</b>
      </div>
      <div class="side-menu">
        <template v-for="g in visibleMenus" :key="g.title">
          <div class="menu-group-title">{{ g.title }}</div>
          <div v-for="m in g.items" :key="m.path"
               class="menu-item" :class="{ active: isActive(m) }"
               @click="$router.push(m.path)">
            <span>{{ m.icon }}</span>
            <span style="flex:1">{{ m.label }}</span>
            <span v-if="m.badge && pendingCount > 0" class="badge">{{ pendingCount }}</span>
          </div>
        </template>
      </div>
    </aside>

    <div class="main-area">
      <div class="topbar">
        <div class="crumbs">
          <span>智慧健身系统</span>
          <span>/</span>
          <b>{{ currentTitle }}</b>
        </div>
        <div class="right">
          <div class="bell" @click="$router.push('/review')" title="待审核内容">
            <el-icon :size="18"><Bell /></el-icon>
            <span v-if="pendingCount > 0" class="dot">{{ pendingCount }}</span>
          </div>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user">
              <div class="avatar">{{ nickname.slice(0, 1) }}</div>
              <span>{{ nickname }}</span>
              <el-icon :size="12" style="color:#c0c4cc"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div class="content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../api/request'
import { useAdminStore } from '../store/admin'

const route = useRoute()
const router = useRouter()
const store = useAdminStore()
const nickname = ref(localStorage.getItem('admin_nickname') || '管理员')

const pendingCount = computed(() => store.pendingCount)

const menuGroups = [
  {
    title: '总览',
    items: [{ label: '数据仪表盘', code: 'dashboard', path: '/dashboard', icon: '📊' }]
  },
  {
    title: '运营管理',
    items: [
      { label: '用户管理', code: 'user', path: '/user', icon: '👥' },
      { label: '课程管理', code: 'course', path: '/course', icon: '🏋️' },
      { label: '动作库', code: 'action', path: '/action', icon: '💪' },
      { label: '内容审核', code: 'review', path: '/review', icon: '✅', badge: true }
    ]
  },
  {
    title: '内容管理',
    items: [
      { label: '食物库', code: 'food', path: '/food', icon: '🥗' },
      { label: '食谱推荐', code: 'recipe', path: '/recipe', icon: '🍱' },
      { label: '资讯管理', code: 'article', path: '/article', icon: '📰' }
    ]
  },
  {
    title: '系统管理',
    items: [
      { label: '角色管理', code: 'system:role', path: '/system/role', icon: '👤' },
      { label: '权限管理', code: 'system:permission', path: '/system/permission', icon: '🔐' },
      { label: '管理员管理', code: 'system:admin', path: '/system/admin', icon: '🛡️' }
    ]
  }
]

const visibleMenus = computed(() => {
  return menuGroups.map(g => ({
    ...g,
    items: g.items.filter(m => store.hasAll || store.permissions.includes(m.code))
  })).filter(g => g.items.length > 0)
})

const currentTitle = computed(() => {
  for (const g of menuGroups) {
    for (const m of g.items) {
      if (route.path === m.path) return m.label
      if (route.path.startsWith(m.path + '/')) return m.label + (route.path.includes('/edit') ? ' · 编辑' : '')
    }
  }
  return '数据仪表盘'
})

function isActive(m) {
  return route.path === m.path || route.path.startsWith(m.path + '/')
}

function onCommand(cmd) {
  if (cmd === 'logout') {
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_nickname')
    store.setPermissions([])
    router.push('/login')
  }
}

async function loadAuth() {
  try {
    const me = await request.get('/admin/auth/me')
    store.setPermissions(me.permissions)
    nickname.value = me.admin.nickname || me.admin.username
    localStorage.setItem('admin_nickname', nickname.value)
  } catch (e) { /* 忽略 */ }
  try {
    const count = await request.get('/admin/review/count')
    store.setPendingCount(count.total || 0)
    store.setPendingDetail(count)
  } catch (e) { /* 忽略 */ }
}

onMounted(loadAuth)
</script>
