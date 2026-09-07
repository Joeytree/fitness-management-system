import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'Dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '数据仪表盘' } },
      { path: 'user', name: 'UserList', component: () => import('../views/user/UserList.vue'), meta: { title: '用户管理' } },
      { path: 'course', name: 'CourseList', component: () => import('../views/course/CourseList.vue'), meta: { title: '课程管理' } },
      { path: 'course/edit', name: 'CourseEdit', component: () => import('../views/course/CourseEdit.vue'), meta: { title: '课程编辑' } },
      { path: 'action', name: 'ActionList', component: () => import('../views/action/ActionList.vue'), meta: { title: '动作管理' } },
      { path: 'action/edit', name: 'ActionEdit', component: () => import('../views/action/ActionEdit.vue'), meta: { title: '动作编辑' } },
      { path: 'review', name: 'ReviewList', component: () => import('../views/review/ReviewList.vue'), meta: { title: '内容审核' } },
      { path: 'food', name: 'FoodList', component: () => import('../views/food/FoodList.vue'), meta: { title: '食物库管理' } },
      { path: 'recipe', name: 'RecipeList', component: () => import('../views/recipe/RecipeList.vue'), meta: { title: '食谱管理' } },
      { path: 'article', name: 'ArticleList', component: () => import('../views/article/ArticleList.vue'), meta: { title: '资讯管理' } },
      { path: 'article/edit', name: 'ArticleEdit', component: () => import('../views/article/ArticleEdit.vue'), meta: { title: '文章编辑' } },
      { path: 'system/role', name: 'RoleList', component: () => import('../views/system/RoleList.vue'), meta: { title: '角色管理' } },
      { path: 'system/permission', name: 'PermissionList', component: () => import('../views/system/PermissionList.vue'), meta: { title: '权限管理' } },
      { path: 'system/admin', name: 'AdminList', component: () => import('../views/system/AdminList.vue'), meta: { title: '管理员管理' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
