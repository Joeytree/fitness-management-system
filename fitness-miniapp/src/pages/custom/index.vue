<template>
  <view class="custom-page">
    <!-- Tab 切换 -->
    <view class="tab-bar">
      <view class="tab" :class="{ active: tab === 'action' }" @click="switchTab('action')">自定义动作</view>
      <view class="tab" :class="{ active: tab === 'course' }" @click="switchTab('course')">自定义课程</view>
    </view>

    <!-- 新建 -->
    <view class="create-btn" @click="create">
      <text class="create-ico">＋</text>
      <text>新建{{ tab === 'action' ? '动作' : '课程' }}</text>
    </view>

    <!-- 列表 -->
    <view v-if="list.length" class="list">
      <view class="card" v-for="item in list" :key="item.id" @click="view(item)">
        <view class="card-head">
          <text class="name">{{ item.name }}</text>
          <text class="tag" :class="'tag-' + item.status">{{ statusText(item.status) }}</text>
        </view>
        <view class="sub" v-if="item.part || item.category">
          {{ tab === 'action' ? (item.part || '') : (item.category || '') }}
        </view>
        <view class="reject" v-if="item.status === 3 && item.rejectReason">驳回原因：{{ item.rejectReason }}</view>
        <view class="card-actions">
          <text class="act" @click.stop="edit(item)">编辑</text>
          <text class="act" @click.stop="view(item)">查看</text>
          <text class="act danger" @click.stop="del(item)">删除</text>
        </view>
      </view>
    </view>
    <view v-else class="empty">
      <text class="empty-ico">📝</text>
      <text class="empty-txt">还没有自定义内容，点击上方按钮创建</text>
    </view>
  </view>
</template>

<script>
import { get, del } from '../../utils/request'

export default {
  data() {
    return {
      tab: 'action',
      list: []
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    switchTab(t) {
      this.tab = t
      this.load()
    },
    async load() {
      const url = this.tab === 'action' ? '/action/mine' : '/course/mine'
      try {
        this.list = (await get(url)) || []
      } catch (e) {
        this.list = []
      }
    },
    statusText(s) {
      return { 1: '待审核', 2: '已公开', 3: '已驳回', 4: '仅自用' }[s] || ''
    },
    create() {
      const url = this.tab === 'action' ? '/pages/custom/action-edit' : '/pages/custom/course-edit'
      uni.navigateTo({ url })
    },
    edit(item) {
      const url = this.tab === 'action'
        ? '/pages/custom/action-edit?id=' + item.id
        : '/pages/custom/course-edit?id=' + item.id
      uni.navigateTo({ url })
    },
    view(item) {
      const url = this.tab === 'action'
        ? '/pages/action/detail?id=' + item.id
        : '/pages/course/detail?id=' + item.id
      uni.navigateTo({ url })
    },
    del(item) {
      uni.showModal({
        title: '删除确认',
        content: '确定删除「' + item.name + '」？删除后不可恢复。',
        success: async (res) => {
          if (!res.confirm) return
          const url = this.tab === 'action' ? '/action/ugc/' + item.id : '/course/ugc/' + item.id
          try {
            await del(url)
            uni.showToast({ title: '已删除', icon: 'success' })
            this.load()
          } catch (e) { /* 已提示 */ }
        }
      })
    }
  }
}
</script>

<style>
.custom-page {
  padding: 24rpx 24rpx 120rpx;
}
.tab-bar {
  display: flex;
  background: #fff;
  border-radius: 999rpx;
  padding: 8rpx;
  margin-bottom: 24rpx;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: #6b7280;
  border-radius: 999rpx;
}
.tab.active {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-weight: 600;
}
.create-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  background: #fff;
  border: 2rpx dashed #34d399;
  color: #10b981;
  border-radius: 24rpx;
  padding: 26rpx 0;
  font-size: 30rpx;
  margin-bottom: 24rpx;
}
.create-ico {
  font-size: 32rpx;
  font-weight: 700;
}
.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.name {
  font-size: 32rpx;
  font-weight: 600;
  color: #1f2937;
  flex: 1;
  margin-right: 16rpx;
}
.tag {
  font-size: 22rpx;
  padding: 4rpx 16rpx;
  border-radius: 999rpx;
  flex: none;
}
.tag-1 { background: #fff7e6; color: #d97706; }
.tag-2 { background: #ecfdf5; color: #059669; }
.tag-3 { background: #fef2f2; color: #dc2626; }
.tag-4 { background: #f3f4f6; color: #6b7280; }
.sub {
  font-size: 24rpx;
  color: #9ca3af;
  margin-top: 12rpx;
}
.reject {
  font-size: 24rpx;
  color: #dc2626;
  background: #fef2f2;
  border-radius: 12rpx;
  padding: 12rpx 16rpx;
  margin-top: 12rpx;
}
.card-actions {
  display: flex;
  gap: 40rpx;
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #f3f4f6;
}
.act {
  font-size: 26rpx;
  color: #10b981;
}
.act.danger {
  color: #ef4444;
}
.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 0;
  color: #9ca3af;
}
.empty-ico {
  font-size: 80rpx;
  margin-bottom: 20rpx;
}
.empty-txt {
  font-size: 26rpx;
}
</style>
