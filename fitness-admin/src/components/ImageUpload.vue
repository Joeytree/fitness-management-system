<template>
  <div class="image-upload">
    <el-upload
      class="iu-uploader"
      action="/api/admin/upload"
      :headers="headers"
      :show-file-list="false"
      :before-upload="beforeUpload"
      :on-success="onSuccess"
      :on-error="onError"
      :accept="accept"
      name="file"
    >
      <div v-if="modelValue" class="iu-preview">
        <img v-if="!isVideo" :src="modelValue" class="iu-img" />
        <video v-else :src="modelValue" class="iu-img" controls />
        <div class="iu-mask">
          <el-icon><Refresh /></el-icon>
          <span>点击替换</span>
        </div>
      </div>
      <div v-else class="iu-empty">
        <el-icon class="iu-icon"><Plus /></el-icon>
        <div class="iu-text">点击上传</div>
        <div class="iu-sub">{{ tip }}</div>
      </div>
    </el-upload>
    <div v-if="modelValue" class="iu-actions">
      <span class="iu-url">{{ modelValue }}</span>
      <el-link type="danger" :underline="false" @click="clear">清除</el-link>
    </div>
    <div v-if="showManual" class="iu-manual">
      <el-input v-model="manualUrl" size="small" placeholder="或手动粘贴媒体 URL" clearable @change="onManual" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  // 文件选择 accept，如 'image/*' 或 'image/*,video/*'
  accept: { type: String, default: 'image/*' },
  maxSizeMB: { type: Number, default: 2 },
  tip: { type: String, default: '支持 jpg/png/webp，≤2MB' },
  // 是否显示「手动粘贴 URL」输入框（动作媒体需保留手填能力）
  showManual: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const manualUrl = ref('')

const headers = computed(() => ({ Authorization: 'Bearer ' + (localStorage.getItem('admin_token') || '') }))

const isVideo = computed(() => /\.(mp4|mov|m4v|avi|webm)$/i.test(props.modelValue || ''))

function beforeUpload(file) {
  const type = file.type || ''
  const isImageOk = props.accept.includes('image') && type.startsWith('image/')
  const isVideoOk = props.accept.includes('video') && type.startsWith('video/')
  if (!isImageOk && !isVideoOk) {
    ElMessage.warning('不支持该文件类型')
    return false
  }
  if (file.size / 1024 / 1024 > props.maxSizeMB) {
    ElMessage.warning(`文件需 ≤ ${props.maxSizeMB}MB`)
    return false
  }
  return true
}

function onSuccess(res) {
  if (res && res.code === 0 && res.data && res.data.url) {
    emit('update:modelValue', res.data.url)
    ElMessage.success('上传成功')
  } else {
    ElMessage.error(res?.message || '上传失败')
  }
}

function onError() {
  ElMessage.error('上传失败，请重试')
}

function onManual(val) {
  emit('update:modelValue', val || '')
}

function clear() {
  manualUrl.value = ''
  emit('update:modelValue', '')
}
</script>

<style scoped>
.image-upload { width: 100%; }
.iu-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  width: 200px;
  height: 120px;
  overflow: hidden;
  transition: border-color .15s;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.iu-uploader :deep(.el-upload:hover) { border-color: #409eff; }
.iu-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #909399;
}
.iu-icon { font-size: 28px; }
.iu-text { margin-top: 6px; font-size: 13px; }
.iu-sub { font-size: 11px; color: #c0c4cc; }
.iu-preview { position: relative; width: 100%; height: 100%; }
.iu-img { width: 200px; height: 120px; object-fit: cover; display: block; background: #000; }
.iu-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, .5);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  opacity: 0;
  transition: opacity .15s;
  font-size: 13px;
}
.iu-preview:hover .iu-mask { opacity: 1; }
.iu-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
.iu-url {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 320px;
}
.iu-manual { margin-top: 6px; }
</style>
