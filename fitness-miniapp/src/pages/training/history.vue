<template>
  <view class="page-pad">
    <view v-for="r in list" :key="r.record.id" class="t-card">
      <view class="t-head" @click="toggle(r)">
        <view class="t-date">{{ fmtTime(r.record.startTime) }}</view>
        <view class="t-meta">
          <text class="t-tag">{{ r.record.duration || 0 }} 分钟</text>
          <text class="t-tag">{{ r.record.calorie || 0 }} 千卡</text>
          <text class="t-tag">{{ r.record.completedCount || 0 }} 个动作</text>
        </view>
        <text class="t-arrow">{{ expandedId === r.record.id ? '▾' : '›' }}</text>
      </view>
      <view v-if="expandedId === r.record.id" class="t-detail">
        <view v-for="s in detailSets" :key="s.id" class="set-row">
          <text class="set-name">{{ s.actionName || ('动作' + s.actionId) }}</text>
          <text class="set-meta muted">{{ s.setNo }}组 · {{ s.reps || 0 }}次 · {{ s.weight || 0 }}kg</text>
        </view>
        <view v-if="!detailSets.length" class="muted set-empty">无逐组明细</view>
      </view>
    </view>
    <view v-if="!list.length" class="empty muted">暂无训练记录，去完成一次训练吧</view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { fmtTime } from '../../utils/index'

export default {
  data() {
    return {
      list: [],
      expandedId: null,
      detailSets: []
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    fmtTime,
    async load() {
      try {
        this.list = await get('/training')
      } catch (e) { /* 已提示 */ }
    },
    async toggle(r) {
      if (this.expandedId === r.record.id) {
        this.expandedId = null
        this.detailSets = []
        return
      }
      this.expandedId = r.record.id
      try {
        const detail = await get('/training/' + r.record.id)
        this.detailSets = detail.sets || []
      } catch (e) {
        this.detailSets = []
      }
    }
  }
}
</script>

<style>
.t-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.05);
}
.t-head {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.t-date {
  flex: 1;
  font-size: 28rpx;
  font-weight: 600;
  color: #1f2937;
}
.t-meta {
  display: flex;
  gap: 12rpx;
}
.t-tag {
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  background: #ecfdf5;
  color: #065f46;
  font-size: 22rpx;
}
.t-arrow {
  color: #9ca3af;
  font-size: 32rpx;
}
.t-detail {
  margin-top: 20rpx;
  padding-top: 20rpx;
  border-top: 1rpx dashed #f3f4f6;
}
.set-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12rpx 0;
  font-size: 26rpx;
}
.set-name {
  color: #374151;
}
.set-meta {
  font-size: 24rpx;
}
.set-empty {
  text-align: center;
  padding: 20rpx 0;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
