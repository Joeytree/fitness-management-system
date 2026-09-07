<template>
  <div class="page-card">
    <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:4px">
      <div style="font-weight:600">内容审核中心</div>
      <div class="muted">规则：UGC 内容（动态 / 评论 / 自定义动作 / 自定义课程）默认待审核，审核通过后立即对用户可见</div>
    </div>

    <el-tabs v-model="tab" @tab-change="onTab">
      <el-tab-pane v-for="t in tabs" :key="t.key" :name="t.key">
        <template #label>
          <span>{{ t.label }}</span>
          <el-badge v-if="pendingOf(t.key) > 0" :value="pendingOf(t.key)" style="margin-left:8px" />
        </template>

        <div v-if="pendingList.length" style="margin-bottom:14px">
          <div class="muted" style="margin-bottom:4px">待审核 {{ pendingList.length }} 条：</div>
          <div v-for="row in pendingList" :key="row.id" class="review-item">
            <span class="avatar-circle" :style="{ background: avColor(row.id) }">{{ avText(row.userName) }}</span>
            <div class="body">
              <div class="name">
                {{ row.userName }}
                <span v-if="row.type === 'comment'" class="muted">评论「{{ row.extra }}」</span>
                <span v-if="row.type === 'action'" class="muted">自定义动作 · {{ row.extra }}</span>
                <span v-if="row.type === 'course'" class="muted">自定义课程 · {{ row.extra }}</span>
                <span v-if="row.type === 'moment_comment'" class="muted">评论动态</span>
              </div>
              <div class="content">
                <template v-if="row.type === 'action' || row.type === 'course'">【{{ row.name }}】{{ row.content }}</template>
                <template v-else>{{ row.content }}</template>
              </div>
              <div class="thumb-imgs" v-if="row.type === 'moment' && row.images">
                <div v-for="(img, i) in (row.images || '').split(',').filter(Boolean)" :key="i" class="ti">🖼️</div>
              </div>
              <div class="meta">
                <span>🕐 {{ row.createTime }}</span>
              </div>
            </div>
            <div class="actions">
              <el-button type="primary" size="small" @click="pass(row)">通过</el-button>
              <el-button type="danger" size="small" plain @click="openReject(row)">驳回</el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无待审核内容" :image-size="80" />

        <el-divider content-position="left">已处理记录（{{ doneList.length }}）</el-divider>
        <el-table :data="doneList" size="small" stripe>
          <el-table-column label="内容" min-width="240">
            <template #default="{ row }">
              <span class="muted">{{ row.content || row.name || '（评论）' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="提交人" width="120">
            <template #default="{ row }">{{ row.userName }}</template>
          </el-table-column>
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'" effect="light">{{ row.status === 1 ? '已通过' : '已驳回' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原因 / 时间" min-width="200">
            <template #default="{ row }">
              <span class="muted">{{ row.rejectReason || '—' }} · {{ row.createTime }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="rejectVisible" title="驳回原因" width="420px">
      <el-input v-model="rejectReason" type="textarea" :rows="3" placeholder="请输入驳回原因，将通知到发布用户" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import { useAdminStore } from '../../store/admin'

const store = useAdminStore()
const AVATAR_COLORS = ['#2f6bff', '#12b886', '#f76707', '#7950f2', '#e64980', '#1098ad', '#f59f00', '#5c940d']

const tabs = [
  { key: 'moment', label: '用户动态' },
  { key: 'moment_comment', label: '动态评论' },
  { key: 'comment', label: '课程评论' },
  { key: 'action', label: 'UGC 动作' },
  { key: 'course', label: 'UGC 课程' }
]

const tab = ref('moment')
const pendingDetail = ref({})
const pendingList = ref([])
const doneList = ref([])
const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectTarget = ref(null)

function avColor(id) { return AVATAR_COLORS[id % AVATAR_COLORS.length] }
function avText(n) { return (n || '?').slice(0, 1) }

function pendingOf(key) {
  return pendingDetail.value[key] || 0
}

async function loadCount() {
  const count = await request.get('/admin/review/count')
  pendingDetail.value = count
  store.setPendingCount(count.total || 0)
  store.setPendingDetail(count)
}

async function loadPending() {
  pendingList.value = await request.get('/admin/review', { params: { type: tab.value, page: 1, pageSize: 100 } })
}

async function loadDone() {
  doneList.value = await request.get('/admin/review/done', { params: { type: tab.value } })
}

async function onTab() {
  await Promise.all([loadPending(), loadDone()])
}

async function refresh() {
  await Promise.all([loadCount(), loadPending(), loadDone()])
}

async function pass(row) {
  await request.put(`/admin/review/${row.id}/approve?type=${tab.value}`)
  ElMessage.success('已通过，内容即刻对用户可见')
  refresh()
}

function openReject(row) {
  rejectTarget.value = row
  rejectReason.value = ''
  rejectVisible.value = true
}

async function confirmReject() {
  if (!rejectTarget.value) return
  await request.put(`/admin/review/${rejectTarget.value.id}/reject?type=${tab.value}`, { reason: rejectReason.value || '内容不符合社区规范' })
  rejectVisible.value = false
  ElMessage.warning('已驳回，原因将通知发布用户')
  refresh()
}

onMounted(() => { loadCount(); onTab() })
</script>
