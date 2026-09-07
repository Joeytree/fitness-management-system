// 通用工具函数与字典

export const dict = {
  gender: { 0: '未知', 1: '男', 2: '女' },
  goal: { 1: '增肌', 2: '减脂', 3: '塑形', 4: '保持' },
  level: { 1: '新手', 2: '初级', 3: '进阶' },
  diff: { 1: '入门', 2: '进阶', 3: '高级' },
  mealType: { 1: '早餐', 2: '午餐', 3: '晚餐', 4: '加餐' },
  articleType: { 1: '饮食知识', 2: '资讯', 3: '系统公告' },
  mediaType: { 1: '动图', 2: '视频', 3: '图片' }
}

export const COURSE_CATS = ['增肌', '减脂', '塑形', '瑜伽', 'HIIT', '康复拉伸']
export const PARTS = ['胸', '背', '肩', '手臂', '腿', '核心', '全身']
export const FOOD_CATS = ['主食', '蛋白', '蔬果', '零食', '饮品']

const AVATAR_COLORS = ['#10b981', '#3b82f6', '#f59e0b', '#8b5cf6', '#ec4899', '#06b6d4', '#f97316', '#84cc16']

export function avatarColor(id) {
  return AVATAR_COLORS[(id || 0) % AVATAR_COLORS.length]
}

export function avatarText(name) {
  return (name || '?').slice(0, 1)
}

export function goalText(g) {
  return dict.goal[g] || ''
}

export function levelText(l) {
  return dict.level[l] || ''
}

export function diffText(d) {
  return dict.diff[d] || ''
}

export function fmtDate(str) {
  return (str || '').slice(0, 10)
}

export function fmtTime(str) {
  return (str || '').slice(0, 16)
}

// 相对时间（如 "3 天前"、"刚刚"、"2 小时前"）
export function relativeTime(str) {
  if (!str) return ''
  const t = new Date(str.replace(/-/g, '/')).getTime()
  if (isNaN(t)) return str
  const diff = Date.now() - t
  const min = Math.floor(diff / 60000)
  if (min < 1) return '刚刚'
  if (min < 60) return min + ' 分钟前'
  const hr = Math.floor(min / 60)
  if (hr < 24) return hr + ' 小时前'
  const day = Math.floor(hr / 24)
  if (day < 30) return day + ' 天前'
  return fmtDate(str)
}

// 登录态管理
export function isLogin() {
  return !!uni.getStorageSync('token')
}

export function getUserInfo() {
  return uni.getStorageSync('userInfo') || {}
}

export function setLogin(token, user) {
  uni.setStorageSync('token', token)
  uni.setStorageSync('userInfo', user)
}

export function logout() {
  uni.removeStorageSync('token')
  uni.removeStorageSync('userInfo')
}
