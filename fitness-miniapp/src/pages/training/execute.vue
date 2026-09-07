<template>
  <view class="page-pad" v-if="detail.plan">
    <!-- 头部 -->
    <view class="head-card">
      <view class="name">{{ detail.plan.name }}</view>
      <view class="timer" v-if="started">
        <text class="timer-val">{{ formatElapsed(elapsed) }}</text>
        <text class="timer-label">训练中</text>
      </view>
    </view>

    <!-- 天数选择 -->
    <scroll-view scroll-x class="day-bar" v-if="days.length > 1">
      <view
        v-for="d in days"
        :key="d.dayNo"
        class="day-chip"
        :class="{ on: currentDay === d.dayNo }"
        @click="selectDay(d.dayNo)"
      >第{{ d.dayNo }}天</view>
    </scroll-view>

    <!-- 动作与逐组 -->
    <view v-for="(a, ai) in workout" :key="ai" class="action-card">
      <view class="a-head">
        <text class="a-name">{{ a.name }}</text>
        <text class="a-meta">{{ a.sets }} 组 × {{ a.reps }} 次</text>
      </view>
      <view v-for="s in a.setList" :key="s.setNo" class="set-row" :class="{ done: s.done }">
        <text class="set-no">第{{ s.setNo }}组</text>
        <input class="num-input" type="digit" v-model="s.weight" placeholder="kg" />
        <text class="unit">kg</text>
        <input class="num-input" type="number" v-model="s.reps" placeholder="次" />
        <text class="unit">次</text>
        <view class="done-box" @click="toggleSet(s)">
          <text v-if="s.done" class="done-tick">✓</text>
        </view>
      </view>
    </view>

    <!-- 操作 -->
    <view class="op-bar">
      <button v-if="!started" class="btn-primary start-btn" @click="start">开始训练</button>
      <button v-else class="btn-primary start-btn" @click="finish">完成训练并打卡</button>
    </view>
  </view>
</template>

<script>
import { get, post } from '../../utils/request'

export default {
  data() {
    return {
      planId: null,
      detail: {},
      days: [],
      currentDay: 1,
      workout: [],
      recordId: null,
      started: false,
      startTs: null,
      elapsed: 0,
      timer: null
    }
  },
  onLoad(options) {
    this.planId = options.id
  },
  onShow() {
    if (this.planId && !this.detail.plan) this.load()
  },
  onUnload() {
    this.stopTimer()
  },
  methods: {
    async load() {
      try {
        this.detail = await get('/plan/' + this.planId)
        this.days = this.detail.days || []
        if (this.days.length) {
          this.currentDay = this.days[0].dayNo
          this.buildWorkout(this.currentDay)
        }
      } catch (e) { /* 忽略 */ }
    },
    selectDay(dayNo) {
      this.currentDay = dayNo
      this.buildWorkout(dayNo)
    },
    buildWorkout(dayNo) {
      const day = this.days.find(d => d.dayNo === dayNo)
      this.workout = (day ? day.actions : []).map(item => ({
        planActionId: item.planActionId,
        actionId: item.action ? item.action.id : null,
        name: item.action ? item.action.name : '',
        sets: item.sets || 1,
        reps: item.reps || 0,
        weight: item.weight,
        setList: this.buildSets(item.sets || 1, item.reps || 0, item.weight)
      }))
    },
    buildSets(count, reps, weight) {
      const list = []
      for (let i = 1; i <= count; i++) {
        list.push({ setNo: i, weight: weight != null ? String(weight) : '', reps: String(reps || ''), done: false })
      }
      return list
    },
    toggleSet(s) {
      s.done = !s.done
    },
    async start() {
      try {
        const rec = await post('/training/start', { planId: Number(this.planId) })
        this.recordId = rec.id
        this.started = true
        this.startTs = Date.now()
        this.elapsed = 0
        this.timer = setInterval(() => {
          this.elapsed = Date.now() - this.startTs
        }, 1000)
      } catch (e) { /* 已提示 */ }
    },
    async finish() {
      const sets = []
      this.workout.forEach(a => {
        a.setList.forEach(s => {
          sets.push({
            actionId: a.actionId,
            setNo: s.setNo,
            weight: s.weight === '' || s.weight == null ? null : Number(s.weight),
            reps: s.reps === '' || s.reps == null ? null : Number(s.reps),
            done: s.done ? 1 : 0
          })
        })
      })
      const duration = Math.max(1, Math.round(this.elapsed / 60000))
      try {
        const data = await post('/training/complete', {
          recordId: this.recordId,
          duration,
          sets
        })
        this.stopTimer()
        const msg = data.checkIn ? '训练完成，已自动打卡' : '训练完成（今日已打卡）'
        uni.showToast({ title: msg, icon: 'none' })
        setTimeout(() => uni.navigateBack(), 900)
      } catch (e) { /* 已提示 */ }
    },
    stopTimer() {
      if (this.timer) {
        clearInterval(this.timer)
        this.timer = null
      }
    },
    formatElapsed(ms) {
      const sec = Math.floor(ms / 1000)
      const m = Math.floor(sec / 60)
      const s = sec % 60
      return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    }
  }
}
</script>

<style>
.head-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 32rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.name {
  font-size: 38rpx;
  font-weight: 700;
}
.timer {
  text-align: center;
}
.timer-val {
  font-size: 44rpx;
  font-weight: 700;
  color: #10b981;
}
.timer-label {
  display: block;
  font-size: 22rpx;
  color: #9ca3af;
}
.day-bar {
  white-space: nowrap;
  margin: 24rpx 0 8rpx;
}
.day-chip {
  display: inline-block;
  padding: 12rpx 32rpx;
  border-radius: 999rpx;
  background: #fff;
  font-size: 26rpx;
  color: #4b5563;
  margin-right: 16rpx;
}
.day-chip.on {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
}
.action-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx 28rpx;
  margin-top: 16rpx;
}
.a-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.a-name {
  font-size: 28rpx;
  font-weight: 600;
}
.a-meta {
  font-size: 24rpx;
  color: #9ca3af;
}
.set-row {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 14rpx 0;
  border-top: 1rpx solid #f3f4f6;
}
.set-row.done {
  background: #f0fdf4;
  border-radius: 12rpx;
}
.set-no {
  width: 96rpx;
  font-size: 26rpx;
  color: #6b7280;
}
.num-input {
  width: 96rpx;
  background: #f3f4f6;
  border-radius: 12rpx;
  padding: 8rpx 12rpx;
  font-size: 26rpx;
  text-align: center;
}
.unit {
  font-size: 24rpx;
  color: #9ca3af;
}
.done-box {
  margin-left: auto;
  width: 52rpx;
  height: 52rpx;
  border-radius: 50%;
  border: 2rpx solid #d1d5db;
  display: flex;
  align-items: center;
  justify-content: center;
}
.set-row.done .done-box {
  background: #10b981;
  border-color: #10b981;
}
.done-tick {
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
}
.op-bar {
  margin-top: 32rpx;
}
.start-btn {
  width: 100%;
  padding: 24rpx 0;
  font-size: 32rpx;
  border: none;
}
.start-btn::after {
  border: none;
}
</style>
