<template>
  <div class="page-card" style="max-width:960px">
    <el-page-header @back="$router.push('/course')" :content="isEdit ? '编辑课程' : '新增课程'" style="margin-bottom:18px" />
    <el-form :model="form" label-width="90px" style="max-width:760px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="课程名称" required><el-input v-model="form.name" placeholder="如：杠铃深蹲入门" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分类" required>
            <el-select v-model="form.category" style="width:100%">
              <el-option v-for="c in cats" :key="c" :label="c" :value="c" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="部位">
            <el-select v-model="form.part" placeholder="请选择锻炼部位" clearable style="width:100%">
              <el-option v-for="p in parts" :key="p" :label="p" :value="p" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="难度">
            <el-select v-model="form.difficulty" style="width:100%">
              <el-option v-for="(v,k) in diffDict" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="时长(分钟)"><el-input-number v-model="form.duration" :min="5" :max="180" style="width:100%" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="消耗(千卡)"><el-input-number v-model="form.calorie" :min="0" :max="1000" style="width:100%" /></el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="课程简介">
            <el-input v-model="form.intro" type="textarea" :rows="3" placeholder="介绍课程适合人群与训练目标" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="封面图">
            <el-upload
              class="cover-uploader"
              action="/api/admin/upload"
              :headers="uploadHeaders"
              :show-file-list="false"
              :before-upload="beforeCoverUpload"
              :on-success="onCoverSuccess"
              :on-error="onCoverError"
              accept="image/*"
              name="file"
            >
              <div v-if="form.cover" class="cover-preview">
                <img :src="form.cover" class="cover-img" />
                <div class="cover-mask">
                  <el-icon><Refresh /></el-icon>
                  <span>点击替换</span>
                </div>
              </div>
              <div v-else class="cover-empty">
                <el-icon class="cover-icon"><Plus /></el-icon>
                <div class="cover-text">点击上传封面图</div>
                <div class="cover-sub">支持 jpg/png/webp，≤2MB</div>
              </div>
            </el-upload>
            <div class="form-tip" v-if="form.cover">当前封面：{{ form.cover }}　<el-link type="danger" :underline="false" @click="form.cover = ''">清除</el-link></div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="关联动作">
            <div class="action-filter">
              <el-select v-model="filterPart" placeholder="运动部位" clearable style="width:180px" @clear="filterPart = ''">
                <el-option label="全部部位" value="" />
                <el-option v-for="p in parts" :key="p" :label="p" :value="p" />
              </el-select>
              <el-select v-model="filterEquipment" placeholder="所需器械" clearable style="width:180px" @clear="filterEquipment = ''">
                <el-option label="全部器械" value="" />
                <el-option v-for="e in equipments" :key="e" :label="e" :value="e" />
              </el-select>
              <span class="filter-stat">已显示 {{ filteredActions.length }} / {{ actions.length }} 个动作</span>
            </div>
            <el-select v-model="selectedIds" multiple filterable collapse-tags collapse-tags-tooltip placeholder="从动作库中选择，可多选" style="width:100%">
              <el-option v-for="a in filteredActions" :key="a.id" :label="a.name + '（' + a.part + '）'" :value="a.id" />
            </el-select>
            <div class="action-items" v-if="form.actionItems && form.actionItems.length">
              <div class="action-item" v-for="item in form.actionItems" :key="item.actionId">
                <div class="ai-name">{{ actionName(item.actionId) }}</div>
                <div class="ai-fields">
                  <span class="ai-label">组数</span>
                  <el-input-number v-model="item.sets" :min="1" :max="20" size="small" controls-position="right" style="width:110px" />
                  <span class="ai-label">次数</span>
                  <el-input-number v-model="item.reps" :min="1" :max="100" size="small" controls-position="right" style="width:110px" />
                  <span class="ai-label">重量(kg)</span>
                  <el-input-number v-model="item.weight" :min="0" :max="500" size="small" controls-position="right" style="width:110px" />
                  <el-button type="danger" text size="small" @click="removeAction(item.actionId)">移除</el-button>
                </div>
              </div>
            </div>
            <div class="form-tip">已选 {{ (form.actionItems || []).length }} 个动作，可配置组数/次数/重量，将组成课程跟练的动作列表</div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="保存状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="0">草稿</el-radio>
              <el-radio :value="2">直接发布</el-radio>
            </el-radio-group>
            <div class="form-tip" style="width:100%">草稿仅管理员可见；发布后移动端用户立即可见。</div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <div style="display:flex;gap:10px;padding-left:90px;margin-top:6px">
      <el-button type="primary" @click="save">保存</el-button>
      <el-button @click="$router.push('/course')">取消</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import request from '../../api/request'

const route = useRoute()
const router = useRouter()
const cats = ['增肌', '减脂', '塑形', '瑜伽', 'HIIT', '康复拉伸']
const diffDict = { 1: '入门', 2: '进阶', 3: '高级' }
// 动作枚举值（与后端 action.part / action.equipment 保持一致）
const parts = ['胸', '背', '肩', '手臂', '腿', '核心', '全身']
const equipments = ['无', '哑铃', '杠铃', '绳索', '固定器械', '弹力带', '史密斯机', '壶铃', '负重', '瑞士球', '拉伸', '药球', '泡沫轴', '有氧器械', '健腹轮']

const actions = ref([])
const filterPart = ref('')
const filterEquipment = ref('')
const editId = route.query.id
const isEdit = ref(!!editId)
const form = ref({ name: '', category: '增肌', part: '', difficulty: 1, duration: 30, calorie: 200, intro: '', cover: '', actionIds: [], actionItems: [], status: 0 })
const selectedIds = ref([])

// 上传时携带 JWT（/api/upload 是 @RequireLogin 接口）
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + (localStorage.getItem('admin_token') || '') }))

const filteredActions = computed(() => actions.value.filter(a => {
  if (filterPart.value && a.part !== filterPart.value) return false
  if (filterEquipment.value && a.equipment !== filterEquipment.value) return false
  return true
}))

async function loadActions() {
  const data = await request.get('/admin/action', { params: { page: 1, pageSize: 100 } })
  actions.value = data.list || []
}

async function loadCourse() {
  if (!editId) return
  const data = await request.get(`/admin/course/${editId}`)
  const items = data.actionItems || []
  form.value = { ...data.course, actionItems: items }
  selectedIds.value = items.map(i => i.actionId)
}

// 已选动作 id 变化时，同步明细列表（新增补默认组数/次数，删除移除）
watch(selectedIds, (newIds) => {
  const next = new Set(newIds)
  const current = form.value.actionItems || []
  // 新增
  for (const id of newIds) {
    if (!current.some(i => i.actionId === id)) {
      current.push({ actionId: id, sets: 3, reps: 12, weight: null })
    }
  }
  // 删除
  form.value.actionItems = current.filter(i => next.has(i.actionId))
})

function actionName(id) {
  const a = actions.value.find(x => x.id === id)
  return a ? `${a.name}（${a.part}）` : `动作#${id}`
}

function removeAction(id) {
  selectedIds.value = selectedIds.value.filter(x => x !== id)
}

function beforeCoverUpload(file) {
  const ok = file.type.startsWith('image/')
  if (!ok) ElMessage.warning('只支持图片格式')
  const lt2M = file.size / 1024 / 1024 < 2
  if (!lt2M) ElMessage.warning('封面图需 ≤ 2MB')
  return ok && lt2M
}

function onCoverSuccess(res) {
  if (res && res.code === 0 && res.data && res.data.url) {
    form.value.cover = res.data.url
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(res?.message || '上传失败')
  }
}

function onCoverError() {
  ElMessage.error('封面上传失败，请重试')
}

async function save() {
  if (!form.value.name || !form.value.category) {
    ElMessage.warning('请填写课程名称与分类')
    return
  }
  if (editId) {
    await request.put(`/admin/course/${editId}`, form.value)
  } else {
    await request.post('/admin/course', form.value)
  }
  ElMessage.success('保存成功')
  router.push('/course')
}

onMounted(() => { loadActions(); loadCourse() })
</script>

<style scoped>
.cover-uploader :deep(.el-upload) { border: 1px dashed #d9d9d9; border-radius: 6px; cursor: pointer; width: 200px; height: 120px; overflow: hidden; transition: border-color .15s; }
.cover-uploader :deep(.el-upload:hover) { border-color: #409eff; }
.cover-empty { display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #909399; }
.cover-empty .cover-icon { font-size: 28px; }
.cover-empty .cover-text { margin-top: 6px; font-size: 13px; }
.cover-empty .cover-sub { font-size: 11px; color: #c0c4cc; }
.cover-preview { position: relative; width: 100%; height: 100%; }
.cover-preview .cover-img { width: 200px; height: 120px; object-fit: cover; display: block; }
.cover-preview .cover-mask { position: absolute; inset: 0; background: rgba(0,0,0,.5); color: #fff; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 4px; opacity: 0; transition: opacity .15s; font-size: 13px; }
.cover-preview:hover .cover-mask { opacity: 1; }
.action-filter { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; flex-wrap: wrap; }
.filter-stat { font-size: 12px; color: #909399; }
.form-tip { font-size: 12px; color: #909399; line-height: 22px; }
.action-items { margin-top: 10px; display: flex; flex-direction: column; gap: 8px; }
.action-item { border: 1px solid #ebeef5; border-radius: 6px; padding: 10px 12px; }
.ai-name { font-size: 13px; color: #303133; font-weight: 600; margin-bottom: 6px; }
.ai-fields { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.ai-label { font-size: 12px; color: #909399; }
</style>