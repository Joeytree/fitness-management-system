<template>
  <div class="page-card" style="max-width:960px">
    <el-page-header @back="$router.push('/action')" :content="isEdit ? '编辑动作' : '新增动作'" style="margin-bottom:18px" />
    <el-form :model="form" label-width="90px" style="max-width:760px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="动作名称" required><el-input v-model="form.name" placeholder="如：标准俯卧撑" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="训练部位" required>
            <el-select v-model="form.part" style="width:100%">
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
          <el-form-item label="所需器械"><el-input v-model="form.equipment" placeholder="如：哑铃 / 徒手" /></el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="文字步骤" required>
            <el-input v-model="form.steps" type="textarea" :rows="4" placeholder="按顺序描述动作过程" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="动作要点"><el-input v-model="form.tips" type="textarea" :rows="2" /></el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="常见错误"><el-input v-model="form.errors" type="textarea" :rows="2" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="呼吸方法"><el-input v-model="form.breath" placeholder="如：下蹲吸气，站起呼气" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="媒体类型">
            <el-select v-model="form.mediaType" style="width:100%" clearable placeholder="可选，支持纯文字讲解">
              <el-option v-for="(v,k) in mediaTypeDict" :key="k" :label="v" :value="Number(k)" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24" v-if="form.mediaType">
          <el-form-item label="媒体地址">
            <el-input v-model="form.mediaUrl" placeholder="媒体 URL（动图 / 视频 / 图片）" />
            <div class="form-tip" style="width:100%">动作详情页应展示文字步骤 + 动图/视频 + 注意事项</div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="保存状态">
            <el-radio-group v-model="form.status">
              <el-radio :value="0">草稿</el-radio>
              <el-radio :value="2">直接发布</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <div style="display:flex;gap:10px;padding-left:90px;margin-top:6px">
      <el-button type="primary" @click="save">保存</el-button>
      <el-button @click="$router.push('/action')">取消</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const route = useRoute()
const router = useRouter()
const parts = ['胸', '背', '肩', '手臂', '腿', '臀', '核心', '全身']
const diffDict = { 1: '入门', 2: '进阶', 3: '高级' }
const mediaTypeDict = { 1: '动图', 2: '视频', 3: '图片' }
const editId = route.query.id
const isEdit = ref(!!editId)
const form = ref({ name: '', part: '胸', difficulty: 1, equipment: '', steps: '', tips: '', errors: '', breath: '', mediaType: 1, mediaUrl: '', status: 0 })

async function load() {
  if (!editId) return
  const data = await request.get(`/admin/action/${editId}`)
  form.value = { ...data }
}

async function save() {
  if (!form.value.name || !form.value.steps) {
    ElMessage.warning('请填写动作名称与文字步骤')
    return
  }
  if (editId) {
    await request.put(`/admin/action/${editId}`, form.value)
  } else {
    await request.post('/admin/action', form.value)
  }
  ElMessage.success('保存成功')
  router.push('/action')
}

onMounted(load)
</script>
