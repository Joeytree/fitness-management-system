<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="muted">管理端登录账号管理：账号绑定角色，角色决定菜单与按钮权限。</div>
      <div class="spacer"></div>
      <el-button type="primary" @click="openForm()">+ 新增管理员</el-button>
    </div>
    <el-table :data="list" stripe>
      <el-table-column label="登录账号" width="150">
        <template #default="{ row }">
          <span style="font-weight:600">{{ row.username }}</span>
          <el-tag v-if="row.username === 'admin'" size="small" type="danger" effect="light" style="margin-left:4px">主账号</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="nickname" label="显示名" min-width="140" />
      <el-table-column label="角色" width="150">
        <template #default="{ row }">{{ roleName(row.roleId) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="light">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ row.createTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openForm(row)" :disabled="row.username === 'admin'">编辑</el-button>
          <el-popconfirm :title="row.status === 1 ? '禁用后该账号无法登录' : '确认恢复该账号？'" @confirm="toggle(row)">
            <template #reference>
              <el-button link :type="row.status === 1 ? 'danger' : 'success'" size="small" :disabled="row.username === 'admin'">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑管理员' : '新增管理员'" width="460px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="登录账号" required><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="显示名"><el-input v-model="form.nickname" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码" required>
          <el-input v-model="form.password" type="password" show-password placeholder="默认 admin123" />
        </el-form-item>
        <el-form-item label="角色" required>
          <el-select v-model="form.roleId" style="width:100%">
            <el-option v-for="r in roles" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const list = ref([])
const roles = ref([])
const formVisible = ref(false)
const form = ref({})

function roleName(id) {
  const r = roles.value.find(x => x.id === id)
  return r ? r.name : '—'
}

async function load() {
  list.value = await request.get('/admin/admin')
  roles.value = await request.get('/admin/role')
}

function openForm(row) {
  form.value = row ? { ...row } : { username: '', nickname: '', password: '', roleId: roles.value[1]?.id || 1 }
  formVisible.value = true
}

async function saveForm() {
  if (!form.value.username) { ElMessage.warning('请填写登录账号'); return }
  if (!form.value.roleId) { ElMessage.warning('请选择角色'); return }
  if (form.value.id) {
    await request.put(`/admin/admin/${form.value.id}`, { nickname: form.value.nickname, roleId: form.value.roleId })
  } else {
    await request.post('/admin/admin', form.value)
  }
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

async function toggle(row) {
  await request.put(`/admin/admin/${row.id}`, { status: row.status === 1 ? 0 : 1 })
  row.status = row.status === 1 ? 0 : 1
  ElMessage.success(row.status === 1 ? '账号已启用' : '账号已禁用，无法再登录')
}

onMounted(load)
</script>
