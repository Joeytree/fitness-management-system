<template>
  <div class="page-card">
    <div class="toolbar">
      <el-select v-model="goalFilter" placeholder="适用目标" clearable style="width:130px" @change="page=1;load()">
        <el-option label="增肌" :value="1" />
        <el-option label="减脂" :value="2" />
        <el-option label="塑形" :value="3" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width:130px" @change="page=1;load()">
        <el-option label="草稿" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已下架" :value="2" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
      <div class="spacer"></div>
      <el-button type="primary" @click="openForm()">+ 新增食谱</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column label="食谱名称" min-width="200">
        <template #default="{ row }">
          <div style="font-weight:600">{{ row.name }}</div>
          <div style="margin-top:4px">
            <el-tag v-for="t in (row.tags || '').split(',').filter(Boolean)" :key="t" size="small" effect="plain" style="margin-right:4px">{{ t }}</el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="适用目标" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.goal === 1 ? 'primary' : (row.goal === 2 ? 'success' : 'warning')" effect="light">{{ { 1: '增肌', 2: '减脂', 3: '塑形' }[row.goal] || '—' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="热量" width="90">
        <template #default="{ row }"><span style="color:#f76707;font-weight:600">{{ row.calorie }}</span> 千卡</template>
      </el-table-column>
      <el-table-column label="营养构成" width="180">
        <template #default="{ row }">蛋白 {{ row.protein }} · 碳水 {{ row.carb }} · 脂肪 {{ row.fat }} g</template>
      </el-table-column>
      <el-table-column label="做法" min-width="220">
        <template #default="{ row }">
          <el-tooltip :content="row.content" placement="top" :show-after="300">
            <span class="muted cell-line">{{ (row.content || '').slice(0, 30) }}{{ (row.content || '').length > 30 ? '…' : '' }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['info','success','danger'][row.status]" size="small" effect="light">{{ ['草稿','已发布','已下架'][row.status] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
          <el-button v-if="row.status !== 1" link type="success" size="small" @click="setStatus(row, 1)">发布</el-button>
          <el-button v-if="row.status === 1" link type="warning" size="small" @click="setStatus(row, 2)">下架</el-button>
          <el-popconfirm title="确认删除该食谱？" @confirm="del(row)">
            <template #reference><el-button link type="danger" size="small">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" v-model:current-page="page" @current-change="load" />
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑食谱' : '新增食谱'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="食谱名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="适用目标">
          <el-radio-group v-model="form.goal">
            <el-radio :value="1">增肌</el-radio>
            <el-radio :value="2">减脂</el-radio>
            <el-radio :value="3">塑形</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="form.tags" multiple filterable allow-create default-first-option style="width:100%" placeholder="推荐标签，供推荐引擎匹配">
            <el-option v-for="t in ['增肌','减脂','高蛋白','高纤维','低碳水','快手','加餐','均衡']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="6"><el-form-item label="热量"><el-input-number v-model="form.calorie" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="蛋白"><el-input-number v-model="form.protein" :min="0" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="碳水"><el-input-number v-model="form.carb" :min="0" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="脂肪"><el-input-number v-model="form.fat" :min="0" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="做法" required>
          <el-input v-model="form.content" type="textarea" :rows="4" />
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
const total = ref(0)
const page = ref(1)
const size = ref(10)
const goalFilter = ref(null)
const statusFilter = ref(null)
const formVisible = ref(false)
const form = ref({})

function blank() { return { name: '', goal: 1, tags: [], calorie: 400, protein: 30, carb: 40, fat: 10, content: '', status: 1 } }

async function load() {
  const data = await request.get('/admin/recipe', {
    params: { goal: goalFilter.value || undefined, status: statusFilter.value, page: page.value, pageSize: size.value }
  })
  list.value = data.list
  total.value = data.total
}

function resetFilter() { goalFilter.value = null; statusFilter.value = null; page.value = 1; load() }

async function setStatus(row, st) {
  await request.put(`/admin/recipe/${row.id}`, { ...row, status: st })
  row.status = st
  ElMessage.success(st === 1 ? '食谱已发布' : '食谱已下架（推荐结果中不再出现）')
}

async function del(row) {
  await request.delete(`/admin/recipe/${row.id}`)
  ElMessage.success('食谱已删除')
  load()
}

function openForm(row) {
  form.value = row ? { ...row, tags: (row.tags || '').split(',').filter(Boolean) } : blank()
  formVisible.value = true
}

async function saveForm() {
  if (!form.value.name || !form.value.content) { ElMessage.warning('请填写食谱名称与做法'); return }
  const body = { ...form.value, tags: (form.value.tags || []).join(',') }
  if (form.value.id) {
    await request.put(`/admin/recipe/${form.value.id}`, body)
  } else {
    await request.post('/admin/recipe', body)
  }
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

onMounted(load)
</script>
