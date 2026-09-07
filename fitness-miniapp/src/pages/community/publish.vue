<template>
  <view class="page-pad">
    <view class="card">
      <textarea
        v-model="content"
        class="textarea"
        placeholder="分享你的健身日常..."
        maxlength="500"
      />
      <view class="char-count muted">{{ content.length }} / 500</view>
      <view class="image-grid">
        <view v-for="(img, i) in images" :key="i" class="img-item">
          <image :src="fullUrl(img)" mode="aspectFill" class="img" />
          <text class="img-remove" @click="removeImage(i)">✕</text>
        </view>
        <view v-if="images.length < 9" class="add-img" @click="chooseImage">＋</view>
      </view>
      <view class="image-tip muted">最多 9 张图片（演示占位）</view>
    </view>

    <view class="topics-card">
      <view class="topics-title">选择话题</view>
      <view class="topics-list">
        <text
          v-for="t in topics"
          :key="t"
          class="topic"
          :class="{ on: selectedTopics.includes(t) }"
          @click="toggleTopic(t)"
        >#{{ t }}</text>
      </view>
    </view>

    <button class="btn-primary publish-btn" @click="publish">发布动态</button>
  </view>
</template>

<script>
import { post } from '../../utils/request'
import config, { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      content: '',
      images: [],
      topics: ['减脂打卡', '增肌记录', '新手求助', '今日份快乐'],
      selectedTopics: []
    }
  },
  methods: {
    fullUrl,
    toggleTopic(t) {
      const i = this.selectedTopics.indexOf(t)
      if (i >= 0) this.selectedTopics.splice(i, 1)
      else this.selectedTopics.push(t)
    },
    chooseImage() {
      uni.chooseImage({
        count: 9 - this.images.length,
        success: (res) => {
          res.tempFilePaths.forEach(path => this.uploadImage(path))
        }
      })
    },
    uploadImage(path) {
      uni.uploadFile({
        url: config.BASE_URL + '/upload',
        filePath: path,
        name: 'file',
        header: {
          'Authorization': 'Bearer ' + (uni.getStorageSync('token') || '')
        },
        success: (res) => {
          try {
            const data = JSON.parse(res.data)
            if (data.code === 0 && data.data && data.data.url) {
              this.images.push(data.data.url)
            } else {
              uni.showToast({ title: '图片上传失败', icon: 'none' })
            }
          } catch (e) {
            uni.showToast({ title: '图片上传失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: '图片上传失败', icon: 'none' })
        }
      })
    },
    removeImage(i) {
      this.images.splice(i, 1)
    },
    async publish() {
      if (!this.content) {
        uni.showToast({ title: '请输入内容', icon: 'none' })
        return
      }
      const tagText = this.selectedTopics.map(t => '#' + t).join(' ')
      const full = (this.content + (tagText ? ' ' + tagText : '')).slice(0, 500)
      try {
        await post('/moment', {
          content: full,
          images: this.images.length ? this.images : undefined
        })
        uni.showToast({ title: '发布成功，等待审核', icon: 'success' })
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
  padding: 28rpx;
}
.textarea {
  width: 100%;
  min-height: 200rpx;
  font-size: 30rpx;
}
.char-count {
  text-align: right;
  font-size: 22rpx;
  margin-top: 4rpx;
}
.image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 20rpx;
}
.img-item {
  position: relative;
}
.img {
  width: 200rpx;
  height: 200rpx;
  border-radius: 16rpx;
}
.img-remove {
  position: absolute;
  top: -12rpx;
  right: -12rpx;
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  text-align: center;
  line-height: 44rpx;
  font-size: 24rpx;
}
.add-img {
  width: 200rpx;
  height: 200rpx;
  border-radius: 16rpx;
  border: 2rpx dashed #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 60rpx;
  color: #d1d5db;
}
.image-tip {
  margin-top: 12rpx;
  font-size: 22rpx;
}
.topics-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-top: 20rpx;
}
.topics-title {
  font-size: 28rpx;
  font-weight: 700;
}
.topics-list {
  margin-top: 20rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
}
.topic {
  padding: 10rpx 28rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  font-size: 26rpx;
  color: #4b5563;
}
.topic.on {
  background: #ecfdf5;
  color: #065f46;
  font-weight: 600;
}
.publish-btn {
  width: 100%;
  padding: 24rpx 0;
  font-size: 32rpx;
  border: none;
  margin-top: 40rpx;
}
.publish-btn::after {
  border: none;
}
</style>