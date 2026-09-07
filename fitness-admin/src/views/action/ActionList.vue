<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q" placeholder="搜索动作名称" clearable style="width:200px" @keyup.enter="page=1;load()" @clear="page=1;load()" />
      <el-select v-model="partFilter" placeholder="全部部位" clearable style="width:120px" @change="page=1;load()">
        <el-option v-for="p in parts" :key="p" :label="p" :value="p" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width:130px" @change="page=1;load()">
        <el-option v-for="(v,k) in dict.courseStatus" :key="k" :label="v" :value="Number(k)" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" @click="$router.push('/action/edit')">+ 新增动作</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column label="动作名称" min-width="160">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:8px">
            <span style="font-weight:600">{{ row.name }}</span>
            <el-tag v-if="row.mediaType" size="small" type="info" effect="plain">{{ dict.mediaType[row.mediaType] }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="part" label="部位" width="90">
        <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.part }}</el-tag></template>
      </el-table-column>
      <el-table-column label="难度" width="80">
        <template #default="{ row }"><span :style="{ color: ['#12b886','#f59f00','#f76707'][row.difficulty-1] }">{{ dict.diff[row.difficulty] || '—' }}</span></template>
      </el-table-column>
      <el-table-column prop="equipment" label="器械" width="110" />
      <el-table-column label="讲解要点" min-width="220">
        <template #default="{ row }">
          <el-tooltip :content="row.steps" placement="top" :show-after="300">
            <span class="cell-line muted">{{ (row.steps || '').slice(0, 30) }}{{ (row.steps || '').length > 30 ? '…' : '' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="dict.courseStatusType[row.status]" size="small" effect="light" class="tag-status">{{ dict.courseStatus[row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$router.push('/action/edit?id=' + row.id)">编辑</el-button>
          <el-button v-if="row.status !== 2" link type="success" size="small" @click="setStatus(row, 2)">发布</el-button>
          <el-button v-if="row.status === 2" link type="warning" size="small" @click="setStatus(row, 3)">下架</el-button>
          <el-popconfirm title="确认删除该动作？" @confirm="del(row)">
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
  courseStatus: { 0: '草稿', 1: '待审核', 2: '已发布', 3: '已下架' },
  courseStatusType: { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' },
  diff: { 1: '入门', 2: '进阶', 3: '高级' },
  mediaType: { 1: '动图', 2: '视频', 3: '图片' }
}
const parts = ['胸', '背', '肩', '手臂', '腿', '臀', '核心', '全身']

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const q = ref('')
const partFilter = ref(null)
const statusFilter = ref(null)

async function load() {
  const data = await request.get('/admin/action', {
    params: { part: partFilter.value || undefined, status: statusFilter.value, keyword: q.value || undefined, page: page.value, pageSize: size.value }
  })
  list.value = data.list
  total.value = data.total
}

function resetFilter() { q.value = ''; partFilter.value = null; statusFilter.value = null; page.value = 1; load() }

async function setStatus(row, st) {
  await request.put(`/admin/action/${row.id}/status`, { status: st })
  row.status = st
  ElMessage.success(st === 2 ? `「${row.name}」已发布` : `「${row.name}」已下架`)
}

async function del(row) {
  ElMessage.info('动作删除为逻辑删除，已下架处理')
}

onMounted(load)
</script>
