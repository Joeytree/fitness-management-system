<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q" placeholder="搜索手机号 / 昵称" clearable style="width:220px" @keyup.enter="page=1;load()" @clear="page=1;load()" />
      <el-select v-model="statusFilter" placeholder="用户状态" clearable style="width:130px" @change="page=1;load()">
        <el-option label="正常" :value="1" />
        <el-option label="已禁用" :value="0" />
      </el-select>
      <el-select v-model="goalFilter" placeholder="健身目标" clearable style="width:130px" @change="page=1;load()">
        <el-option v-for="(v,k) in dict.goal" :key="k" :label="v" :value="Number(k)" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
      <div class="spacer"></div>
      <div class="muted">共 {{ total }} 位用户</div>
    </div>

    <el-table :data="list" stripe>
      <el-table-column label="用户" min-width="180">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:10px">
            <span class="avatar-circle" :style="{ background: avColor(row.id) }">{{ avText(row.nickname) }}</span>
            <div>
              <div style="font-weight:600">{{ row.nickname }}</div>
              <div class="muted">ID: {{ row.id }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column label="性别" width="70">
        <template #default="{ row }"><span>{{ dict.gender[row.gender] || '未知' }}</span></template>
      </el-table-column>
      <el-table-column label="身高" width="90">
        <template #default="{ row }"><span>{{ row.height ? row.height + 'cm' : '—' }}</span></template>
      </el-table-column>
      <el-table-column label="目标 · 水平" width="150">
        <template #default="{ row }">
          <el-tag size="small" type="primary" effect="light">{{ dict.goal[row.goal] || '—' }}</el-tag>
          <el-tag size="small" effect="plain" style="margin-left:4px">{{ dict.level[row.level] || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small" effect="light" class="tag-status">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="注册时间" width="160">
        <template #default="{ row }">{{ row.createTime || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
          <el-switch
            :model-value="row.status === 1"
            inline-prompt
            :active-text="row.status === 1 ? '正常' : '禁用'"
            :inactive-text="row.status === 1 ? '正常' : '禁用'"
            @change="v => toggleStatus(row, v)" />
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" v-model:current-page="page" @current-change="load" />
    </div>

    <el-dialog v-model="detailVisible" :title="'用户详情 · ' + (detail.nickname || '')" width="560px">
      <div v-if="detail.id" style="display:flex;gap:20px">
        <div style="text-align:center">
          <span class="avatar-circle" :style="{ background: avColor(detail.id), width:'72px', height:'72px', fontSize:'26px' }">{{ avText(detail.nickname) }}</span>
        </div>
        <el-descriptions :column="2" border size="small" style="flex:1">
          <el-descriptions-item label="用户ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="手机号">{{ detail.phone }}</el-descriptions-item>
          <el-descriptions-item label="昵称">{{ detail.nickname }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ dict.gender[detail.gender] || '未知' }}</el-descriptions-item>
          <el-descriptions-item label="身高">{{ detail.height ? detail.height + 'cm' : '—' }}</el-descriptions-item>
          <el-descriptions-item label="健身目标">{{ dict.goal[detail.goal] || '—' }}</el-descriptions-item>
          <el-descriptions-item label="训练水平">{{ dict.level[detail.level] || '—' }}</el-descriptions-item>
          <el-descriptions-item label="推荐标签">{{ detail.tags || '—' }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ detail.createTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">{{ detail.status === 1 ? '正常' : '已禁用' }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button :type="detail.status === 1 ? 'danger' : 'success'" @click="toggleStatus(detail, detail.status === 1 ? 0 : 1); detailVisible = false">
          {{ detail.status === 1 ? '禁用该用户' : '恢复启用' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const dict = {
  gender: { 0: '未知', 1: '男', 2: '女' },
  goal: { 1: '增肌', 2: '减脂', 3: '塑形', 4: '保持' },
  level: { 1: '新手', 2: '初级', 3: '进阶' }
}
const AVATAR_COLORS = ['#2f6bff', '#12b886', '#f76707', '#7950f2', '#e64980', '#1098ad', '#f59f00', '#5c940d']

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const q = ref('')
const statusFilter = ref(null)
const goalFilter = ref(null)
const detailVisible = ref(false)
const detail = ref({})

function avColor(id) { return AVATAR_COLORS[id % AVATAR_COLORS.length] }
function avText(n) { return (n || '?').slice(0, 1) }

async function load() {
  const data = await request.get('/admin/user', {
    params: { keyword: q.value || undefined, status: statusFilter.value, goal: goalFilter.value, page: page.value, pageSize: size.value }
  })
  list.value = data.list
  total.value = data.total
}

function resetFilter() {
  q.value = ''; statusFilter.value = null; goalFilter.value = null; page.value = 1; load()
}

function openDetail(row) { detail.value = { ...row }; detailVisible.value = true }

async function toggleStatus(row, v) {
  const target = v ? 1 : 0
  await request.put(`/admin/user/${row.id}/status`, { status: target })
  row.status = target
  ElMessage.success(target ? `用户 ${row.nickname} 已启用` : `用户 ${row.nickname} 已禁用（强制下线、无法再登录）`)
}

onMounted(load)
</script>
