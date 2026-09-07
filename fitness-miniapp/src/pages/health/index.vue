<template>
  <view class="page-pad">
    <!-- 顶部渐变大卡片 -->
    <view class="hero-card">
      <view class="hero-row">
        <view class="hero-item">
          <view class="hero-label">今日体重</view>
          <view class="hero-val">{{ latest.weight || '--' }}<text class="hero-unit">kg</text></view>
          <view v-if="diff" class="hero-diff" :class="{ down: diff < 0, up: diff > 0 }">
            较上次 {{ diff > 0 ? '+' : '' }}{{ diff.toFixed(1) }}kg
          </view>
        </view>
        <view class="hero-divider"></view>
        <view class="hero-item">
          <view class="hero-label">BMI</view>
          <view class="hero-val">{{ latest.bmi || '--' }}</view>
          <view v-if="bmiLevel" class="hero-diff" :class="bmiLevel.cls">{{ bmiLevel.label }}</view>
        </view>
        <view class="hero-divider"></view>
        <view class="hero-item">
          <view class="hero-label">体脂率</view>
          <view class="hero-val">{{ latest.bodyFat || '--' }}<text class="hero-unit">%</text></view>
        </view>
      </view>
    </view>

    <!-- 打卡 -->
    <button class="btn-primary checkin-btn" @click="doCheckIn">🔥 今日训练打卡</button>

    <!-- 趋势图 -->
    <view class="trend-card">
      <view class="trend-head">
        <view class="t-title">体重趋势</view>
        <view class="range-tabs">
          <view
            v-for="r in ranges"
            :key="r.value"
            class="range-tab"
            :class="{ active: range === r.value }"
            @click="switchRange(r.value)"
          >{{ r.label }}</view>
        </view>
      </view>
      <view class="trend-unit muted">单位 kg</view>
      <canvas
        id="trendCanvas"
        type="2d"
        class="trend-canvas"
        v-if="trendPoints.length > 1"
      ></canvas>
      <view v-else class="trend-empty muted">至少录入 2 条体重数据后展示趋势</view>
    </view>

    <!-- 健康评估 -->
    <view v-if="assessment.level" class="assess-card">
      <view class="a-title">健康评估：{{ assessment.level }}</view>
      <view class="a-advice">{{ assessment.advice }}</view>
    </view>

    <!-- 入口 -->
    <view class="menu-group">
      <view class="menu-item" @click="go('/pages/health/body')">
        <text class="m-ico">📏</text><text class="m-label">身体数据录入</text><text class="m-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/health/goal')">
        <text class="m-ico">🎯</text><text class="m-label">健康目标</text><text class="m-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/health/checkin')">
        <text class="m-ico">📅</text><text class="m-label">打卡日历</text><text class="m-arrow">›</text>
      </view>
    </view>
  </view>
</template>

<script>
import { get, post } from '../../utils/request'

export default {
  data() {
    return {
      latest: {},
      bodyList: [],
      assessment: {},
      range: '30d',
      ranges: [
        { value: '7d', label: '7天' },
        { value: '30d', label: '30天' },
        { value: 'all', label: '全部' }
      ],
      trendPoints: [],
      diff: 0
    }
  },
  computed: {
    bmiLevel() {
      const bmi = Number(this.latest.bmi) || 0
      if (!bmi) return null
      if (bmi < 18.5) return { label: '偏瘦', cls: '' }
      if (bmi < 24) return { label: '正常', cls: 'ok' }
      if (bmi < 28) return { label: '超重', cls: 'warn' }
      return { label: '肥胖', cls: 'danger' }
    }
  },
  onShow() {
    this.load()
    this.loadTrend()
  },
  methods: {
    async load() {
      try {
        const list = await get('/health/body')
        this.bodyList = list
        if (list.length) {
          this.latest = list[list.length - 1] || {}
          if (list.length >= 2) {
            const last = Number(list[list.length - 1].weight) || 0
            const prev = Number(list[list.length - 2].weight) || 0
            this.diff = last - prev
          }
        }
        this.assessment = await get('/health/assessment')
      } catch (e) { /* 忽略 */ }
    },
    async loadTrend() {
      try {
        this.trendPoints = await get('/health/trend', { metric: 'weight', range: this.range })
        this.$nextTick(() => this.drawTrend())
      } catch (e) { /* 忽略 */ }
    },
    switchRange(r) {
      this.range = r
      this.loadTrend()
    },
    drawTrend() {
      if (this.trendPoints.length < 2) return
      const query = uni.createSelectorQuery().in(this)
      query.select('#trendCanvas').fields({ node: true, size: true }).exec((res) => {
        if (!res || !res[0] || !res[0].node) return
        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        const dpr = (uni.getSystemInfoSync().pixelRatio) || 2
        const w = res[0].width
        const h = res[0].height
        canvas.width = w * dpr
        canvas.height = h * dpr
        ctx.scale(dpr, dpr)

        const pts = this.trendPoints
        const padL = 36, padR = 16, padT = 20, padB = 32
        const vals = pts.map(p => p.value)
        const min = Math.min(...vals)
        const max = Math.max(...vals)
        const span = (max - min) || 1
        const range = max - min
        const ymin = min - range * 0.1
        const ymax = max + range * 0.1
        const yspan = (ymax - ymin) || 1
        const xStep = (w - padL - padR) / (pts.length - 1)
        const x = i => padL + i * xStep
        const y = v => h - padB - (h - padT - padB) * ((v - ymin) / yspan)

        // 网格
        ctx.strokeStyle = '#f3f4f6'
        ctx.lineWidth = 1
        ctx.fillStyle = '#9ca3af'
        ctx.font = '10px sans-serif'
        ctx.textAlign = 'right'
        for (let g = 0; g <= 4; g++) {
          const v = ymin + (ymax - ymin) * g / 4
          const gy = y(v)
          ctx.beginPath()
          ctx.moveTo(padL, gy)
          ctx.lineTo(w - padR, gy)
          ctx.stroke()
          ctx.fillText(v.toFixed(1), padL - 6, gy + 4)
        }

        // 渐变填充
        const grad = ctx.createLinearGradient(0, padT, 0, h - padB)
        grad.addColorStop(0, 'rgba(16, 185, 129, 0.35)')
        grad.addColorStop(1, 'rgba(16, 185, 129, 0.02)')
        ctx.fillStyle = grad
        ctx.beginPath()
        ctx.moveTo(x(0), y(pts[0].value))
        pts.forEach((p, i) => ctx.lineTo(x(i), y(p.value)))
        ctx.lineTo(x(pts.length - 1), h - padB)
        ctx.lineTo(x(0), h - padB)
        ctx.closePath()
        ctx.fill()

        // 折线
        ctx.strokeStyle = '#10b981'
        ctx.lineWidth = 2.5
        ctx.lineJoin = 'round'
        ctx.beginPath()
        pts.forEach((p, i) => {
          if (i === 0) ctx.moveTo(x(i), y(p.value))
          else ctx.lineTo(x(i), y(p.value))
        })
        ctx.stroke()

        // 数据点 + x 轴日期标签
        ctx.textAlign = 'center'
        const showLabels = pts.length <= 14
        pts.forEach((p, i) => {
          ctx.fillStyle = '#fff'
          ctx.beginPath()
          ctx.arc(x(i), y(p.value), 3.5, 0, Math.PI * 2)
          ctx.fill()
          ctx.strokeStyle = '#10b981'
          ctx.lineWidth = 1.5
          ctx.stroke()
          if (showLabels) {
            ctx.fillStyle = '#9ca3af'
            const d = (p.date || '').slice(5)
            ctx.fillText(d, x(i), h - 12)
          }
        })
      })
    },
    async doCheckIn() {
      try {
        await post('/health/checkin', { duration: 30, calorie: 200 })
        uni.showToast({ title: '打卡成功！', icon: 'success' })
      } catch (e) { /* 已提示 */ }
    },
    go(url) {
      uni.navigateTo({ url })
    }
  }
}
</script>

<style>
.hero-card {
  background: linear-gradient(135deg, #34d399, #10b981);
  border-radius: 32rpx;
  padding: 40rpx 32rpx;
  color: #fff;
  margin-bottom: 20rpx;
}
.hero-row {
  display: flex;
  align-items: center;
}
.hero-item {
  flex: 1;
  text-align: center;
}
.hero-label {
  font-size: 24rpx;
  opacity: 0.9;
}
.hero-val {
  font-size: 52rpx;
  font-weight: 700;
  margin-top: 8rpx;
}
.hero-unit {
  font-size: 22rpx;
  margin-left: 6rpx;
  font-weight: 400;
  opacity: 0.85;
}
.hero-diff {
  font-size: 22rpx;
  margin-top: 6rpx;
  opacity: 0.85;
}
.hero-diff.down {
  color: #d1fae5;
}
.hero-diff.up {
  color: #fef3c7;
}
.hero-diff.ok {
  color: #d1fae5;
}
.hero-diff.warn {
  color: #fef3c7;
}
.hero-diff.danger {
  color: #fee2e2;
}
.hero-divider {
  width: 1rpx;
  height: 72rpx;
  background: rgba(255, 255, 255, 0.25);
}
.checkin-btn {
  width: 100%;
  padding: 24rpx 0;
  font-size: 30rpx;
  border: none;
  margin-top: 8rpx;
}
.checkin-btn::after {
  border: none;
}
.trend-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-top: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.trend-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.t-title {
  font-size: 32rpx;
  font-weight: 700;
}
.range-tabs {
  display: flex;
  gap: 8rpx;
}
.range-tab {
  padding: 8rpx 24rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  color: #6b7280;
  background: #f3f4f6;
}
.range-tab.active {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
}
.trend-unit {
  font-size: 22rpx;
  text-align: right;
  margin-top: 4rpx;
}
.trend-canvas {
  width: 100%;
  height: 320rpx;
  margin-top: 8rpx;
}
.trend-empty {
  text-align: center;
  padding: 100rpx 0;
  font-size: 26rpx;
}
.assess-card {
  background: #fffbeb;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-top: 20rpx;
}
.a-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #f59e0b;
}
.a-advice {
  margin-top: 12rpx;
  font-size: 26rpx;
  color: #6b7280;
  line-height: 1.7;
}
.menu-group {
  background: #fff;
  border-radius: 28rpx;
  overflow: hidden;
  margin-top: 24rpx;
}
.menu-item {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #f3f4f6;
}
.menu-item:last-child {
  border-bottom: none;
}
.m-ico {
  font-size: 36rpx;
}
.m-label {
  flex: 1;
  font-size: 28rpx;
}
.m-arrow {
  color: #d1d5db;
  font-size: 32rpx;
}
</style>