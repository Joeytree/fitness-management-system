<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="muted">角色是权限的集合：一个角色对应一组菜单与按钮权限；管理员通过角色获得权限。</div>
      <div class="spacer"></div>
      <el-button type="primary" @click="openForm()">+ 新增角色</el-button>
    </div>

    <el-table :data="roles" stripe>
      <el-table-column label="角色名称" min-width="140">
        <template #default="{ row }">
          <span style="font-weight:600">{{ row.name }}</span>
          <el-tag v-if="row.code === 'SUPER_ADMIN'" size="small" type="danger" effect="light" style="margin-left:6px">内置</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色编码" width="170">
        <template #default="{ row }"><code style="background:#f4f6fb;padding:2px 8px;border-radius:4px;font-size:12px">{{ row.code }}</code></template>
      </el-table-column>
      <el-table-column label="描述" min-width="240">
        <template #default="{ row }">{{ row.description || '—' }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ row.createTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openPerm(row)">分配权限</el-button>
          <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑角色' : '新增角色'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称" required><el-input v-model="form.name" placeholder="如：内容编辑" /></el-form-item>
        <el-form-item label="角色编码" required><el-input v-model="form.code" placeholder="如：content_editor" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="permVisible" :title="'分配权限 · ' + (permRole.name || '')" width="520px">
      <el-alert type="info" :closable="false" show-icon title="勾选该角色可访问的菜单与按钮权限（无权限入口前端隐藏、后端拒绝）" style="margin-bottom:12px" />
      <el-tree ref="permTree" :data="permTreeData" show-checkbox node-key="id" default-expand-all :props="{ label: 'name', children: 'children' }" />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const roles = ref([])
const permissions = ref([])
const formVisible = ref(false)
const permVisible = ref(false)
const form = ref({})
const permRole = ref({})
const permTree = ref(null)
const permTreeData = ref([])

function blank() { return { name: '', code: '', description: '' } }

function buildTree(list) {
  const map = {}
  const roots = []
  list.forEach(p => { map[p.id] = { ...p, children: [] } })
  list.forEach(p => {
    if (p.parentId && map[p.parentId]) {
      map[p.parentId].children.push(map[p.id])
    } else {
      roots.push(map[p.id])
    }
  })
  return roots
}

async function load() {
  roles.value = await request.get('/admin/role')
  permissions.value = await request.get('/admin/permission')
  permTreeData.value = buildTree(permissions.value)
}

function openForm(row) {
  form.value = row ? { ...row } : blank()
  formVisible.value = true
}

async function saveForm() {
  if (!form.value.name || !form.value.code) { ElMessage.warning('请填写角色名称与编码'); return }
  if (form.value.id) {
    await request.put(`/admin/role/${form.value.id}`, form.value)
  } else {
    await request.post('/admin/role', form.value)
  }
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

async function openPerm(row) {
  permRole.value = row
  permVisible.value = true
  const ids = await request.get(`/admin/role/${row.id}/permission`)
  setTimeout(() => { permTree.value && permTree.value.setCheckedKeys(ids || []) }, 100)
}

async function savePerm() {
  const checked = permTree.value.getCheckedKeys()
  const half = permTree.value.getHalfCheckedKeys()
  await request.put(`/admin/role/${permRole.value.id}/permission`, { permissionIds: [...checked, ...half] })
  ElMessage.success('权限已保存（重新登录后按新权限生效）')
  permVisible.value = false
}

onMounted(load)
</script>
