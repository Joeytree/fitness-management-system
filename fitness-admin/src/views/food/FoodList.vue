<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="q" placeholder="搜索食物名称" clearable style="width:200px" @keyup.enter="page=1;load()" @clear="page=1;load()" />
      <el-select v-model="catFilter" placeholder="全部分类" clearable style="width:130px" @change="page=1;load()">
        <el-option v-for="c in cats" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width:120px" @change="page=1;load()">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-button @click="resetFilter">重置</el-button>
      <div class="spacer"></div>
      <el-button @click="openImport">📥 Excel 批量导入</el-button>
      <el-button type="primary" @click="openForm()">+ 新增食物</el-button>
    </div>

    <el-table :data="list" stripe>
      <el-table-column label="食物名称" width="140">
        <template #default="{ row }"><span style="font-weight:600">{{ row.name }}</span></template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="90">
        <template #default="{ row }"><el-tag size="small" effect="plain">{{ row.category }}</el-tag></template>
      </el-table-column>
      <el-table-column label="每100g热量" width="120">
        <template #default="{ row }"><span style="font-weight:600;color:#f76707">{{ row.calorie }}</span> 千卡</template>
      </el-table-column>
      <el-table-column label="三大营养素" width="230">
        <template #default="{ row }">
          <span style="color:#2f6bff">蛋白 {{ row.protein }}</span> ·
          <span style="color:#f59f00">碳水 {{ row.carb }}</span> ·
          <span style="color:#e64980">脂肪 {{ row.fat }}</span> g
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" inline-prompt active-text="上架" inactive-text="下架" @change="v => toggle(row, v)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openForm(row)">编辑</el-button>
          <el-popconfirm title="确认删除该食物？" @confirm="del(row)">
            <template #reference><el-button link type="danger" size="small">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager-wrap">
      <el-pagination background layout="total, prev, pager, next" :total="total" :page-size="size" v-model:current-page="page" @current-change="load" />
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑食物' : '新增食物'" width="520px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="食物名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" style="width:100%">
            <el-option v-for="c in cats" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="热量(千卡)"><el-input-number v-model="form.calorie" :min="0" :max="1000" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="计量单位"><el-input v-model="form.unit" placeholder="g / 碗 / 个" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="蛋白(g)"><el-input-number v-model="form.protein" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="碳水(g)"><el-input-number v-model="form.carb" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="脂肪(g)"><el-input-number v-model="form.fat" :min="0" :max="100" :precision="1" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveForm">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="Excel 批量导入" width="620px">
      <div v-if="!importResult">
        <el-upload drag :show-file-list="false" :http-request="handleImport" accept=".xlsx,.xls">
          <div class="media-placeholder" style="height:120px;border:none">📁 点击选择 Excel 文件<br><span class="muted">字段：名称 / 分类 / 热量 / 蛋白 / 碳水 / 脂肪 / 单位</span></div>
        </el-upload>
      </div>
      <div v-else>
        <el-result icon="success" title="导入完成" :sub-title="`共 ${importResult.total || 0} 条，成功 ${importResult.success} 条，失败 ${importResult.failed} 条`">
          <template #extra>
            <el-alert v-for="(e, i) in importResult.errors" :key="i" :title="e" type="error" :closable="false" style="margin-bottom:8px" />
          </template>
        </el-result>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const cats = ['主食', '蛋白', '蔬果', '零食', '饮品']

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const q = ref('')
const catFilter = ref(null)
const statusFilter = ref(null)
const formVisible = ref(false)
const form = ref({})
const importVisible = ref(false)
const importResult = ref(null)

function blank() { return { name: '', category: '主食', calorie: 100, protein: 5, carb: 20, fat: 2, unit: 'g', status: 1 } }

async function load() {
  const data = await request.get('/admin/food', {
    params: { keyword: q.value || undefined, category: catFilter.value || undefined, status: statusFilter.value, page: page.value, pageSize: size.value }
  })
  list.value = data.list
  total.value = data.total
}

function resetFilter() { q.value = ''; catFilter.value = null; statusFilter.value = null; page.value = 1; load() }

async function toggle(row, v) {
  await request.put(`/admin/food/${row.id}`, { ...row, status: v ? 1 : 0 })
  row.status = v ? 1 : 0
  ElMessage.success(v ? `「${row.name}」已上架` : `「${row.name}」已下架`)
}

async function del(row) {
  await request.delete(`/admin/food/${row.id}`)
  ElMessage.success('食物已删除')
  load()
}

function openForm(row) {
  form.value = row ? { ...row } : blank()
  formVisible.value = true
}

async function saveForm() {
  if (!form.value.name) { ElMessage.warning('请填写食物名称'); return }
  if (form.value.id) {
    await request.put(`/admin/food/${form.value.id}`, form.value)
  } else {
    await request.post('/admin/food', form.value)
  }
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

function openImport() { importResult.value = null; importVisible.value = true }

async function handleImport({ file }) {
  const fd = new FormData()
  fd.append('file', file)
  importResult.value = await request.post('/admin/food/import', fd)
  load()
}

onMounted(load)
</script>
