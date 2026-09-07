<template>
  <div>
    <div class="page-card">
      <div class="toolbar">
        <el-input v-model="q" placeholder="搜索课程名称" clearable style="width:200px" @keyup.enter="page=1;load()" @clear="page=1;load()" />
        <el-select v-model="catFilter" placeholder="全部分类" clearable style="width:130px" @change="page=1;load()">
          <el-option v-for="c in cats" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width:130px" @change="page=1;load()">
          <el-option v-for="(v,k) in dict.courseStatus" :key="k" :label="v" :value="Number(k)" />
        </el-select>
        <el-button @click="resetFilter">重置</el-button>
        <div class="spacer"></div>
        <el-button type="primary" @click="$router.push('/course/edit')">+ 新增课程</el-button>
      </div>

      <el-table :data="list" stripe>
        <el-table-column label="课程" min-width="220">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px">
              <div class="cover-thumb">{{ (row.category || '').slice(0,2) }}</div>
              <div>
                <div style="font-weight:600">{{ row.name }}</div>
                <div class="muted">{{ row.duration }} 分钟 · 约 {{ row.calorie }} 千卡</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.category }}</el-tag></template>
        </el-table-column>
        <el-table-column label="难度" width="80">
          <template #default="{ row }">
            <span :style="{ color: ['#12b886','#f59f00','#f76707'][row.difficulty-1] }">{{ dict.diff[row.difficulty] || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="浏览 / 收藏" width="110">
          <template #default="{ row }"><span>{{ row.viewCount ?? 0 }} / {{ row.favoriteCount ?? 0 }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="dict.courseStatusType[row.status]" size="small" effect="light" class="tag-status">{{ dict.courseStatus[row.status] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ row.createTime || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="$router.push('/course/edit?id=' + row.id)">编辑</el-button>
            <el-button v-if="row.status !== 2" link type="success" size="small" @click="setStatus(row, 2)">发布</el-button>
            <el-button v-if="row.status === 2" link type="warning" size="small" @click="setStatus(row, 3)">下架</el-button>
            <el-popconfirm title="删除后不可恢复，确认删除该课程？" @confirm="del(row)">
              <template #reference><el-button link type="danger" size="small">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager-wrap">
        <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" v-model:current-page="page" @current-change="load" />
      </div>
    </div>

    <div class="page-card">
      <div style="font-weight:600;margin-bottom:6px">⚠️ 状态流转说明</div>
      <div class="muted">草稿 / 待审核 / 已下架 的课程对普通用户不可见；发布后立即对移动端用户可见。</div>
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
  diff: { 1: '入门', 2: '进阶', 3: '高级' }
}
const cats = ['增肌', '减脂', '塑形', '瑜伽', 'HIIT', '康复拉伸']

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const q = ref('')
const catFilter = ref(null)
const statusFilter = ref(null)

async function load() {
  const data = await request.get('/admin/course', {
    params: { keyword: q.value || undefined, category: catFilter.value || undefined, status: statusFilter.value, page: page.value, pageSize: size.value }
  })
  list.value = data.list
  total.value = data.total
}

function resetFilter() { q.value = ''; catFilter.value = null; statusFilter.value = null; page.value = 1; load() }

async function setStatus(row, st) {
  await request.put(`/admin/course/${row.id}/status`, { status: st })
  row.status = st
  ElMessage.success(st === 2 ? `「${row.name}」已发布，移动端即刻可见` : `「${row.name}」已下架，对用户隐藏`)
}

async function del(row) {
  await request.delete(`/admin/course/${row.id}`)
  ElMessage.success(`已删除「${row.name}」`)
  load()
}

onMounted(load)
</script>
