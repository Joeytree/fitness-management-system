<template>
  <div class="page-card">
    <div class="toolbar">
      <div class="muted">系统权限树：菜单权限（type=1）+ 按钮权限（type=2），为角色分配的最小粒度。</div>
    </div>
    <el-table :data="tree" row-key="id" default-expand-all border>
      <el-table-column label="权限名称" min-width="200">
        <template #default="{ row }">
          <span style="font-weight:600">{{ row.icon || '' }} {{ row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column label="权限编码" min-width="170">
        <template #default="{ row }"><code style="background:#f4f6fb;padding:2px 8px;border-radius:4px;font-size:12px">{{ row.code }}</code></template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 1 ? 'primary' : 'warning'" effect="light">{{ row.type === 1 ? '菜单' : '按钮' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="前端路由" min-width="170">
        <template #default="{ row }"><span class="muted">{{ row.path || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="排序" width="80">
        <template #default="{ row }">{{ row.sort }}</template>
      </el-table-column>
    </el-table>
    <div class="form-tip" style="margin-top:10px">数据结构对应 permission 表：type 1=菜单 2=按钮，parent_id 组织层级，path 对应前端路由。</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../../api/request'

const tree = ref([])

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
  // 清理空的 children
  const clean = (nodes) => nodes.map(n => ({ ...n, children: n.children && n.children.length ? clean(n.children) : undefined }))
  return clean(roots)
}

async function load() {
  const data = await request.get('/admin/permission')
  tree.value = buildTree(data)
}

onMounted(load)
</script>
