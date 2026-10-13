<template>
  <view class="page-pad">
    <view class="card">
      <view class="form-row">
        <text class="label">动作名称 *</text>
        <input class="input" v-model="form.name" placeholder="如：自重臂屈伸" />
      </view>
      <view class="form-row">
        <text class="label">训练部位</text>
        <picker :range="parts" @change="onPart">
          <view class="picker-value">{{ form.part || '全身' }} ▾</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="label">所需器械</text>
        <input class="input" v-model="form.equipment" placeholder="如：哑铃 / 双杠 / 徒手" />
      </view>

      <!-- 媒体上传（可选） -->
      <view class="form-row col">
        <text class="label">演示媒体（可选）</text>
        <view class="media-box">
          <view v-if="form.mediaUrl" class="media-preview" @click="removeMedia">
            <image v-if="form.mediaType !== 2" class="media-img" :src="fullUrl(form.mediaUrl)" mode="widthFix" />
            <view v-else class="media-video">🎬 已上传视频</view>
            <view class="media-del">点击移除</view>
          </view>
          <view v-else class="media-actions">
            <view class="media-btn" @click="chooseMedia('image')">🖼 图片/动图</view>
            <view class="media-btn" @click="chooseMedia('video')">🎬 视频</view>
          </view>
        </view>
      </view>

      <view class="form-row col">
        <text class="label">文字步骤 *</text>
        <textarea class="textarea" v-model="form.steps" placeholder="按顺序描述动作过程" />
      </view>
      <view class="form-row col">
        <text class="label">动作要点</text>
        <textarea class="textarea" v-model="form.tips" placeholder="选填" />
      </view>
      <view class="form-row col">
        <text class="label">常见错误</text>
        <textarea class="textarea" v-model="form.errors" placeholder="选填" />
      </view>
      <view class="form-row">
        <text class="label">呼吸方法</text>
        <input class="input" v-model="form.breath" placeholder="选填" />
      </view>
    </view>

    <!-- 可见性二选一 -->
    <view class="vis-card">
      <text class="vis-tip">提交方式（决定内容的可见范围）</text>
      <view class="vis-row">
        <view class="vis-btn ghost" @click="submit('private')">
          <text class="vis-title">仅自用</text>
          <text class="vis-sub">仅自己可见，永不公开</text>
        </view>
        <view class="vis-btn primary" @click="submit('public')">
          <text class="vis-title">申请公开</text>
          <text class="vis-sub">提交审核，通过后全站可见</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { get, post, put } from '../../utils/request'
import { PARTS } from '../../utils/index'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      parts: PARTS,
      id: null,
      form: { name: '', part: '全身', equipment: '', steps: '', tips: '', errors: '', breath: '', mediaType: null, mediaUrl: '' }
    }
  },
  onLoad(options) {
    this.id = options.id
    if (this.id) {
      this.load()
    }
  },
  methods: {
    fullUrl,
    onPart(e) {
      this.form.part = this.parts[Number(e.detail.value)]
    },
    async load() {
      try {
        const a = await get('/action/' + this.id)
        if (a) {
          this.form = {
            name: a.name || '',
            part: a.part || '全身',
            equipment: a.equipment || '',
            steps: a.steps || '',
            tips: a.tips || '',
            errors: a.errors || '',
            breath: a.breath || '',
            mediaType: a.mediaType,
            mediaUrl: a.mediaUrl || ''
          }
        }
      } catch (e) { /* 已提示 */ }
    },
    chooseMedia(type) {
      uni.chooseMedia({
        count: 1,
        mediaType: type === 'video' ? ['video'] : ['image'],
        success: (res) => {
          const f = res.tempFiles && res.tempFiles[0]
          if (!f) return
          this.uploadMedia(f.tempFilePath, type)
        }
      })
    },
    async uploadMedia(filePath, type) {
      try {
        const url = await uploadFile(filePath)
        this.form.mediaUrl = url
        this.form.mediaType = type === 'video' ? 2 : (/\.gif$/i.test(url) ? 1 : 3)
      } catch (e) { /* 已提示 */ }
    },
    removeMedia() {
      this.form.mediaUrl = ''
      this.form.mediaType = null
    },
    async submit(visibility) {
      if (!this.form.name || !this.form.steps) {
        uni.showToast({ title: '动作名称与步骤为必填', icon: 'none' })
        return
      }
      const payload = { ...this.form, visibility }
      try {
        if (this.id) {
          await put('/action/ugc/' + this.id, payload)
        } else {
          await post('/action/ugc', payload)
        }
        const tip = visibility === 'private' ? '已保存为仅自用' : '已提交，等待审核'
        uni.showToast({ title: tip, icon: 'success' })
        setTimeout(() => uni.navigateBack(), 800)
      } catch (e) { /* 已提示 */ }
    }
  }
}
</script>

<style>
.card {
  background: #fff;
  border-radius: 28rpx;
  padding: 8rpx 28rpx;
}
.form-row {
  display: flex;
  align-items: center;
  padding: 26rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}
.form-row.col {
  flex-direction: column;
  align-items: flex-start;
  gap: 16rpx;
}
.label {
  width: 180rpx;
  font-size: 28rpx;
  color: #6b7280;
  flex: none;
}
.input {
  flex: 1;
  font-size: 28rpx;
}
.picker-value {
  font-size: 28rpx;
  color: #1f2937;
}
.textarea {
  width: 100%;
  min-height: 140rpx;
  font-size: 28rpx;
}
.media-box {
  width: 100%;
}
.media-actions {
  display: flex;
  gap: 20rpx;
}
.media-btn {
  flex: 1;
  text-align: center;
  padding: 28rpx 0;
  border: 2rpx dashed #d1d5db;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #6b7280;
}
.media-preview {
  position: relative;
  width: 100%;
}
.media-img {
  width: 100%;
  border-radius: 16rpx;
}
.media-video {
  padding: 60rpx 0;
  text-align: center;
  background: #f3f4f6;
  border-radius: 16rpx;
  color: #6b7280;
}
.media-del {
  position: absolute;
  right: 16rpx;
  bottom: 16rpx;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
}
.vis-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-top: 28rpx;
}
.vis-tip {
  font-size: 26rpx;
  color: #6b7280;
  display: block;
  margin-bottom: 20rpx;
}
.vis-row {
  display: flex;
  gap: 20rpx;
}
.vis-btn {
  flex: 1;
  border-radius: 20rpx;
  padding: 28rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.vis-btn.ghost {
  background: #f3f4f6;
}
.vis-btn.primary {
  background: linear-gradient(135deg, #34d399, #10b981);
}
.vis-title {
  font-size: 30rpx;
  font-weight: 600;
}
.vis-btn.ghost .vis-title {
  color: #4b5563;
}
.vis-btn.primary .vis-title {
  color: #fff;
}
.vis-sub {
  font-size: 22rpx;
}
.vis-btn.ghost .vis-sub {
  color: #9ca3af;
}
.vis-btn.primary .vis-sub {
  color: rgba(255, 255, 255, 0.9);
}
</style>
