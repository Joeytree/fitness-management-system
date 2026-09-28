<template>
  <view class="page-pad">
    <!-- ===== 热量概览卡片（薄荷健康风） ===== -->
    <view class="cal-card">
      <view class="cc-top">
        <!-- 左：饮食摄入 -->
        <view class="cc-side">
          <text class="cc-side-label">饮食摄入</text>
          <text class="cc-side-val intake">{{ intake }}</text>
          <text class="cc-side-unit">千卡</text>
        </view>

        <!-- 中：缺口圆环 -->
        <view class="cc-ring-wrap">
          <canvas id="ringCanvas" type="2d" class="ring"></canvas>
          <view class="cc-ring-text">
            <text class="cc-remain-label">还可摄入</text>
            <text class="cc-remain-val" :class="{ over: summary.over }">{{ remain }}</text>
            <text class="cc-target">推荐摄入 {{ target }}</text>
          </view>
        </view>

        <!-- 右：运动消耗 -->
        <view class="cc-side">
          <text class="cc-side-label">运动消耗</text>
          <text class="cc-side-val">{{ burn }}</text>
          <text class="cc-side-unit">千卡</text>
        </view>
      </view>

      <!-- 三大营养素 -->
      <view class="nutrients">
        <view class="n-item">
          <text class="n-val" style="color:#3b82f6">{{ fmt(summary.protein) }}<text class="n-u">克</text></text>
          <text class="n-label">蛋白质</text>
        </view>
        <view class="n-item">
          <text class="n-val" style="color:#f59e0b">{{ fmt(summary.carb) }}<text class="n-u">克</text></text>
          <text class="n-label">碳水</text>
        </view>
        <view class="n-item">
          <text class="n-val" style="color:#ec4899">{{ fmt(summary.fat) }}<text class="n-u">克</text></text>
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
    intake() {
      return Number(this.summary.totalCalorie || 0)
    },
    target() {
      return Number(this.summary.targetCalorie || 0)
    },
    burn() {
      return Number(this.summary.burnCalorie || 0)
    },
    // 还可摄入：优先后端计算值，兜底前端算
    remain() {
      if (this.summary.remainCalorie != null) return Number(this.summary.remainCalorie)
      return Math.max(0, this.target - this.intake + this.burn)
    },
    mealGroups() {
      // 按 mealType 分组 + 计算每组总热量
      return this.mealDefs.map(def => {
        const items = this.records.filter(r => r.mealType === def.type)
        const totalCal = items.reduce((s, i) => s + (Number(i.calorie) || 0), 0)
        return { ...def, items, totalCal: Math.round(totalCal) }
      }).filter(g => g.items.length > 0)
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    fmt(v) {
      const n = Number(v || 0)
      return n % 1 === 0 ? String(n) : n.toFixed(1)
    },
    async load() {
      try {
        this.summary = await get('/diet/summary')
      } catch (e) { /* 忽略 */ }
      try {
        this.records = await get('/diet/record')
        this.$nextTick(() => setTimeout(() => this.drawRing(), 60))
      } catch (e) { /* 忽略 */ }
    },
    // 缺口圆环：缺口居中在顶部，绿色段表示 已摄入/推荐
    drawRing() {
      const query = uni.createSelectorQuery().in(this)
      query.select('#ringCanvas').fields({ node: true, size: true }).exec((res) => {
        if (!res || !res[0] || !res[0].node || !res[0].width) return
        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        const dpr = (uni.getSystemInfoSync().pixelRatio) || 2
        const w = res[0].width
        const h = res[0].height
        canvas.width = w * dpr
        canvas.height = h * dpr
        ctx.scale(dpr, dpr)
        ctx.clearRect(0, 0, w, h)

        const lw = 14
        const cx = w / 2
        const cy = h / 2
        const r = Math.min(w, h) / 2 - lw / 2 - 2

        // 顶部缺口 40°
        const gap = Math.PI * 2 * (40 / 360)
        const start = -Math.PI / 2 + gap / 2
        const sweep = Math.PI * 2 - gap

        // 底环
        ctx.beginPath()
        ctx.arc(cx, cy, r, start, start + sweep)
        ctx.strokeStyle = '#e6f7ef'
        ctx.lineWidth = lw
        ctx.lineCap = 'round'
        ctx.stroke()

        // 进度环（已摄入 / 推荐）
        const target = Number(this.summary.targetCalorie) || 0
        if (target > 0) {
          const ratio = Math.min(1, (Number(this.summary.totalCalorie) || 0) / target)
          if (ratio > 0) {
            ctx.beginPath()
            ctx.arc(cx, cy, r, start, start + sweep * ratio)
            ctx.strokeStyle = this.summary.over ? '#f59e0b' : '#10b981'
            ctx.lineWidth = lw
            ctx.lineCap = 'round'
            ctx.stroke()
          }
        }
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
/* ===== 热量概览卡片 ===== */
.cal-card {
  background: #fff;
  border-radius: 32rpx;
  padding: 36rpx 24rpx 28rpx;
  box-shadow: 0 6rpx 24rpx rgba(16, 185, 129, 0.06);
  margin-bottom: 24rpx;
}
.cc-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.cc-side {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.cc-side-label {
  font-size: 22rpx;
  color: #9ca3af;
}
.cc-side-val {
  font-size: 46rpx;
  font-weight: 800;
  color: #1f2937;
  line-height: 1.15;
  margin-top: 6rpx;
  font-variant-numeric: tabular-nums;
}
.cc-side-val.intake {
  color: #10b981;
}
.cc-side-unit {
  font-size: 20rpx;
  color: #9ca3af;
  margin-top: 2rpx;
}

/* 中间圆环 */
.cc-ring-wrap {
  position: relative;
  width: 244rpx;
  height: 244rpx;
  flex-shrink: 0;
}
.ring {
  width: 244rpx;
  height: 244rpx;
}
.cc-ring-text {
  position: absolute;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}
.cc-remain-label {
  font-size: 20rpx;
  color: #9ca3af;
}
.cc-remain-val {
  font-size: 52rpx;
  font-weight: 800;
  color: #1f2937;
  line-height: 1.05;
  margin-top: 2rpx;
  font-variant-numeric: tabular-nums;
}
.cc-remain-val.over {
  color: #f59e0b;
}
.cc-target {
  font-size: 20rpx;
  color: #9ca3af;
  margin-top: 4rpx;
}

/* 三大营养素 */
.nutrients {
  margin-top: 28rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #f3f4f6;
  display: flex;
  justify-content: space-around;
}
.n-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.n-val {
  font-size: 36rpx;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
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

/* ===== 今日餐饮 ===== */
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
