<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q" placeholder="搜索文章标题" clearable style="width:220px" @keyup.enter="page=1;load()" @clear="page=1;load()" />
      <el-select v-model="typeFilter" placeholder="全部类型" clearable style="width:130px" @change="page=1;load()">
        <el-option v-for="(v,k) in dict.articleType" :key="k" :label="v" :value="Number(k)" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width:120px" @change="page=1;load()">
        <el-option label="已发布" :value="1" />
        <el-option label="草稿" :value="0" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" @click="$router.push('/article/edit')">+ 发布文章</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column label="标题" min-width="260">
        <template #default="{ row }"><span style="font-weight:600">{{ row.title }}</span></template>
      </el-table-column>
      <el-table-column label="类型" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="dict.articleTypeType[row.type]" effect="light">{{ dict.articleType[row.type] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'info'" effect="light">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" width="170">
        <template #default="{ row }">{{ row.createTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$router.push('/article/edit?id=' + row.id)">编辑</el-button>
          <el-button v-if="row.status === 0" link type="success" size="small" @click="setStatus(row, 1)">发布</el-button>
          <el-button v-if="row.status === 1" link type="warning" size="small" @click="setStatus(row, 0)">下线</el-button>
          <el-popconfirm title="确认删除该文章？" @confirm="del(row)">
            <template #reference><el-button link type="danger" size="small">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" v-model:current-page="page" @current-change="load" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const dict = {
  articleType: { 1: '饮食知识', 2: '资讯', 3: '系统公告' },
  articleTypeType: { 1: 'success', 2: 'primary', 3: 'warning' }
}

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const q = ref('')
const typeFilter = ref(null)
const statusFilter = ref(null)

async function load() {
  const data = await request.get('/admin/article', {
    params: { type: typeFilter.value || undefined, status: statusFilter.value, keyword: q.value || undefined, page: page.value, pageSize: size.value }
  })
  list.value = data.list
  total.value = data.total
}

function resetFilter() { q.value = ''; typeFilter.value = null; statusFilter.value = null; page.value = 1; load() }

async function setStatus(row, st) {
  await request.put(`/admin/article/${row.id}`, { ...row, status: st })
  row.status = st
  ElMessage.success(st === 1 ? `「${row.title}」已发布` : `「${row.title}」已下线`)
}

async function del(row) {
  await request.delete(`/admin/article/${row.id}`)
  ElMessage.success('文章已删除')
  load()
}

onMounted(load)
</script>
