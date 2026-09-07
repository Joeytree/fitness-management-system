<template>
  <div class="page-card" style="max-width:960px">
    <el-page-header @back="$router.push('/article')" :content="isEdit ? '编辑文章' : '发布文章'" style="margin-bottom:18px" />
    <el-form :model="form" label-width="80px" style="max-width:820px">
      <el-form-item label="标题" required>
        <el-input v-model="form.title" placeholder="请输入文章标题" />
      </el-form-item>
      <el-form-item label="类型" required>
        <el-radio-group v-model="form.type">
          <el-radio v-for="(v,k) in typeDict" :key="k" :value="Number(k)">{{ v }}</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="封面">
        <el-input v-model="form.cover" placeholder="封面图片 URL（可选）" />
      </el-form-item>
      <el-form-item label="正文" required>
        <div style="width:100%;border:1px solid #e5e8ef;border-radius:8px;overflow:hidden">
          <div style="background:#f7f8fa;padding:8px 12px;border-bottom:1px solid #e5e8ef;display:flex;gap:4px">
            <span class="muted" style="border:1px solid #dcdfe6;border-radius:4px;padding:2px 8px;cursor:pointer">B</span>
            <span class="muted" style="border:1px solid #dcdfe6;border-radius:4px;padding:2px 8px;cursor:pointer">I</span>
            <span class="muted" style="border:1px solid #dcdfe6;border-radius:4px;padding:2px 8px;cursor:pointer">U</span>
            <span class="muted" style="border:1px solid #dcdfe6;border-radius:4px;padding:2px 8px;cursor:pointer">≡ 列表</span>
            <span class="muted" style="border:1px solid #dcdfe6;border-radius:4px;padding:2px 8px;cursor:pointer">🖼️ 图片</span>
            <span class="muted" style="border:1px solid #dcdfe6;border-radius:4px;padding:2px 8px;cursor:pointer">🔗 链接</span>
          </div>
          <el-input v-model="form.content" type="textarea" :rows="12" placeholder="在这里输入正文内容" style="border:none;box-shadow:none" />
        </div>
      </el-form-item>
      <el-form-item label="保存状态">
        <el-radio-group v-model="form.status">
          <el-radio :value="0">存为草稿</el-radio>
          <el-radio :value="1">立即发布</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <div style="display:flex;gap:10px;padding-left:80px;margin-top:6px">
      <el-button type="primary" @click="save">保存</el-button>
      <el-button @click="$router.push('/article')">取消</el-button>
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
const typeDict = { 1: '饮食知识', 2: '资讯', 3: '系统公告' }
const editId = route.query.id
const isEdit = ref(!!editId)
const form = ref({ title: '', type: 1, cover: '', content: '', status: 0 })

async function load() {
  if (!editId) return
  const data = await request.get(`/admin/article/${editId}`)
  form.value = { ...data }
}

async function save() {
  if (!form.value.title || !form.value.content) {
    ElMessage.warning('请填写标题与正文')
    return
  }
  if (editId) {
    await request.put(`/admin/article/${editId}`, form.value)
  } else {
    await request.post('/admin/article', form.value)
  }
  ElMessage.success('保存成功')
  router.push('/article')
}

onMounted(load)
</script>
