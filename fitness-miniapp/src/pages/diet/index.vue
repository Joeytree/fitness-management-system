<template>
  <view class="page-pad">
    <!-- 热量概览 -->
    <view class="cal-card">
      <canvas id="ringCanvas" type="2d" class="ring"></canvas>
      <view class="ring-text">
        <view class="ring-val">{{ summary.totalCalorie || 0 }}</view>
        <view class="ring-unit">/ {{ summary.targetCalorie || 0 }} 千卡</view>
        <view class="ring-tip" :class="{ over: summary.over }">
          {{ summary.over ? '⚠️ 已超出目标' : (summary.targetCalorie ? '剩余 ' + Math.max(0, summary.targetCalorie - summary.totalCalorie) + ' 千卡' : '') }}
        </view>
      </view>
      <view class="nutrients">
        <view class="n-item">
          <text class="n-val" style="color:#3b82f6">{{ (summary.protein || 0).toFixed(1) }}<text class="n-u">g</text></text>
          <text class="n-label">蛋白质</text>
        </view>
        <view class="n-item">
          <text class="n-val" style="color:#f59e0b">{{ (summary.carb || 0).toFixed(1) }}<text class="n-u">g</text></text>
          <text class="n-label">碳水</text>
        </view>
        <view class="n-item">
          <text class="n-val" style="color:#ec4899">{{ (summary.fat || 0).toFixed(1) }}<text class="n-u">g</text></text>
          <text class="n-label">脂肪</text>
        </view>
      </view>
    </view>

    <!-- 餐次入口 -->
    <view class="section-title">
      <text>今日餐饮</text>
      <text class="add-btn" @click="goRecord(1)">+ 记录饮食</text>
    </view>

    <view v-for="m in mealGroups" :key="m.type" class="meal-card">
      <view class="meal-head">
        <view class="meal-left">
          <text class="meal-ico">{{ m.icon }}</text>
          <text class="meal-name">{{ m.label }}</text>
        </view>
        <text class="meal-cal">{{ m.totalCal }} 千卡</text>
        <text class="meal-add" @click="goRecord(m.type)">＋</text>
      </view>
      <view v-for="r in m.items" :key="r.id" class="meal-row">
        <text class="row-ico">🍽</text>
        <text class="row-name">{{ r.foodName }}</text>
        <text class="row-amount muted">{{ r.amount }}g</text>
        <text class="row-cal">{{ r.calorie }}千卡</text>
        <text class="row-x" @click="delRecord(r.id)">✕</text>
      </view>
    </view>

    <view v-if="!records.length" class="empty muted">今天还没有饮食记录，去添加第一顿吧～</view>
  </view>
</template>

<script>
import { get, del } from '../../utils/request'
import { dict } from '../../utils/index'

export default {
  data() {
    return {
      summary: {},
      records: [],
      mealDefs: [
        { type: 1, label: '早餐', icon: '🌅' },
        { type: 2, label: '午餐', icon: '☀️' },
        { type: 3, label: '晚餐', icon: '🌙' },
        { type: 4, label: '加餐', icon: '🍎' }
      ]
    }
  },
  computed: {
    mealGroups() {
      // 按 mealType 分组 + 计算每组总热量
      return this.mealDefs.map(def => {
        const items = this.records.filter(r => r.mealType === def.type)
        const totalCal = items.reduce((s, i) => s + (Number(i.calorie) || 0), 0)
        return { ...def, items, totalCal }
      }).filter(g => g.items.length > 0)
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    async load() {
      try {
        this.summary = await get('/diet/summary')
      } catch (e) { /* 忽略 */ }
      try {
        this.records = await get('/diet/record')
        this.$nextTick(() => this.drawRing())
      } catch (e) { /* 忽略 */ }
    },
    drawRing() {
      const query = uni.createSelectorQuery().in(this)
      query.select('#ringCanvas').fields({ node: true, size: true }).exec((res) => {
        if (!res || !res[0] || !res[0].node) return
        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        const dpr = (uni.getSystemInfoSync().pixelRatio) || 2
        const w = res[0].width
        const h = res[0].height
        canvas.width = w * dpr
        canvas.height = h * dpr
        ctx.scale(dpr, dpr)
        const cx = w / 2
        const cy = h / 2
        const r = Math.min(w, h) / 2 - 14
        const lw = 16

        // 背景环
        ctx.strokeStyle = '#e6f7ef'
        ctx.lineWidth = lw
        ctx.lineCap = 'round'
        ctx.beginPath()
        ctx.arc(cx, cy, r, 0, Math.PI * 2)
        ctx.stroke()

        // 进度环
        const total = Number(this.summary.totalCalorie) || 0
        const target = Number(this.summary.targetCalorie) || 1
        const ratio = Math.min(1, total / target)
        const over = total > target
        ctx.strokeStyle = over ? '#f59e0b' : '#10b981'
        ctx.beginPath()
        ctx.arc(cx, cy, r, -Math.PI / 2, -Math.PI / 2 + Math.PI * 2 * ratio)
        ctx.stroke()
      })
    },
    goRecord(type) {
      uni.navigateTo({ url: '/pages/diet/search?mealType=' + type })
    },
    async delRecord(id) {
      try {
        await del('/diet/record/' + id)
        uni.showToast({ title: '已删除', icon: 'none' })
        this.load()
      } catch (e) { /* 忽略 */ }
    }
  }
}
</script>

<style>
.cal-card {
  position: relative;
  background: #fff;
  border-radius: 32rpx;
  padding: 40rpx 32rpx 32rpx;
  box-shadow: 0 6rpx 24rpx rgba(16, 185, 129, 0.06);
  margin-bottom: 24rpx;
  text-align: center;
}
.ring {
  width: 320rpx;
  height: 320rpx;
  margin: 0 auto;
}
.ring-text {
  position: absolute;
  top: 56rpx;
  left: 0;
  right: 0;
  text-align: center;
  pointer-events: none;
}
.ring-val {
  font-size: 56rpx;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}
.ring-unit {
  font-size: 22rpx;
  color: #9ca3af;
  margin-top: 8rpx;
}
.ring-tip {
  font-size: 24rpx;
  color: #10b981;
  margin-top: 12rpx;
}
.ring-tip.over {
  color: #f59e0b;
}
.nutrients {
  margin-top: 24rpx;
  display: flex;
  justify-content: space-around;
  padding-top: 24rpx;
  border-top: 1rpx solid #f3f4f6;
}
.n-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.n-val {
  font-size: 36rpx;
  font-weight: 700;
}
.n-u {
  font-size: 20rpx;
  margin-left: 4rpx;
  font-weight: 400;
  color: #9ca3af;
}
.n-label {
  font-size: 22rpx;
  color: #6b7280;
  margin-top: 6rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 32rpx 8rpx 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.add-btn {
  font-size: 24rpx;
  font-weight: 500;
  color: #10b981;
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
  background: #ecfdf5;
}
.meal-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.05);
}
.meal-head {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding-bottom: 16rpx;
  border-bottom: 1rpx dashed #f3f4f6;
}
.meal-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
}
.meal-ico {
  font-size: 36rpx;
}
.meal-name {
  font-size: 30rpx;
  font-weight: 700;
  color: #1f2937;
}
.meal-cal {
  font-size: 26rpx;
  color: #10b981;
  font-weight: 600;
}
.meal-add {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #ecfdf5;
  color: #10b981;
  font-size: 36rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 56rpx;
  text-align: center;
}
.meal-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 20rpx 0 0;
}
.row-ico {
  font-size: 28rpx;
}
.row-name {
  flex: 1;
  font-size: 28rpx;
  color: #374151;
}
.row-amount {
  font-size: 24rpx;
}
.row-cal {
  font-size: 26rpx;
  color: #6b7280;
}
.row-x {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  background: #f3f4f6;
  color: #9ca3af;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24rpx;
}
.empty {
  text-align: center;
  padding: 80rpx 0;
}
</style>