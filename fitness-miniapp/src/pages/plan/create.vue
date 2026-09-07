<template>
  <view class="page-pad">
    <view class="card">
      <view class="form-row">
        <text class="label">计划名</text>
        <input class="input" v-model="name" placeholder="如：胸背日" />
      </view>
      <view class="form-row">
        <text class="label">训练目标</text>
        <view class="radio-group">
          <text
            v-for="g in goals"
            :key="g.value"
            class="radio"
            :class="{ on: goal === g.value }"
            @click="goal = g.value"
          >{{ g.label }}</text>
        </view>
      </view>
      <view class="form-row col">
        <text class="label">每周练</text>
        <view class="dot-group">
          <view
            v-for="f in freqList"
            :key="f"
            class="dot-item"
            :class="{ on: freq === f }"
            @click="freq = f"
          >
            <text class="dot-num">{{ f }}</text>
            <text class="dot-unit">练</text>
          </view>
        </view>
      </view>
      <view class="form-row col">
        <text class="label">每次时长</text>
        <view class="dot-group">
          <view
            v-for="d in durationList"
            :key="d"
            class="dot-item"
            :class="{ on: duration === d }"
            @click="duration = d"
          >
            <text class="dot-num">{{ d }}</text>
            <text class="dot-unit">min</text>
          </view>
        </view>
      </view>
    </view>

    <view class="section-title">动作列表</view>
    <view v-for="(a, i) in selected" :key="i" class="action-item">
      <view class="a-head">
        <text class="a-name">{{ a.name }}</text>
        <text class="a-remove" @click="removeAction(i)">✕</text>
      </view>
      <view class="a-params">
        <view class="stepper">
          <text @click="dec(a, 'day', 1)">－</text>
          <text class="stepper-val day-val">第{{ a.day }}天</text>
          <text @click="inc(a, 'day', 7)">＋</text>
        </view>
        <view class="stepper">
          <text @click="dec(a, 'sets')">－</text>
          <text class="stepper-val">{{ a.sets }}</text>
          <text @click="inc(a, 'sets')">＋</text>
        </view>
        <text class="p-label">组</text>
        <view class="stepper">
          <text @click="dec(a, 'reps')">－</text>
          <text class="stepper-val">{{ a.reps }}</text>
          <text @click="inc(a, 'reps')">＋</text>
        </view>
        <text class="p-label">次</text>
      </view>
      <view class="a-params">
        <input class="half-input" v-model="a.weight" type="digit" placeholder="负重kg（空=自重）" />
        <input class="rest-input" v-model="a.rest" type="number" placeholder="休息s" />
      </view>
    </view>

    <view class="add-btn" @click="openPicker">＋ 添加动作</view>

    <button class="btn-primary save-btn" @click="save">保存计划</button>

    <!-- 动作选择弹窗 -->
    <view v-if="pickerVisible" class="picker-mask" @click="pickerVisible = false">
      <view class="picker-panel" @click.stop>
        <view class="picker-title">选择动作</view>
        <scroll-view scroll-y class="picker-list">
          <view v-for="a in actions" :key="a.id" class="picker-item" @click="pickAction(a)">
            <text>{{ a.name }}</text>
            <text class="muted">{{ a.part }}</text>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { get, post } from '../../utils/request'

export default {
  data() {
    return {
      name: '',
      goal: 1,
      freq: 3,
      duration: 45,
      freqList: [3, 4, 5],
      durationList: [30, 45, 60],
      goals: [
        { value: 1, label: '增肌' },
        { value: 2, label: '减脂' },
        { value: 3, label: '塑形' }
      ],
      actions: [],
      selected: [],
      pickerVisible: false
    }
  },
  onLoad() {
    this.loadActions()
  },
  methods: {
    async loadActions() {
      try {
        const data = await get('/action', { page: 1, pageSize: 100 })
        this.actions = data.list || []
      } catch (e) { /* 忽略 */ }
    },
    openPicker() {
      this.pickerVisible = true
    },
    pickAction(a) {
      this.selected.push({ actionId: a.id, name: a.name, day: 1, sets: 3, reps: 12, weight: '', rest: 60 })
      this.pickerVisible = false
    },
    removeAction(i) {
      this.selected.splice(i, 1)
    },
    inc(a, key, max) {
      a[key] = (a[key] || 0) + 1
      if (max && a[key] > max) a[key] = max
    },
    dec(a, key, min) {
      const lo = min || 1
      if (a[key] > lo) a[key] = a[key] - 1
    },
    async save() {
      if (!this.name) {
        uni.showToast({ title: '请填写计划名', icon: 'none' })
        return
      }
      if (!this.selected.length) {
        uni.showToast({ title: '请添加动作', icon: 'none' })
        return
      }
      try {
        await post('/plan', {
          name: this.name,
          goal: this.goal,
          level: 1,
          cycle: `每周${this.freq}练·每次${this.duration}分钟`,
          actions: this.selected.map((a, i) => ({
            actionId: a.actionId,
            dayNo: a.day || 1,
            sets: a.sets,
            reps: a.reps,
            weight: a.weight ? Number(a.weight) : null,
            rest: Number(a.rest) || 0,
            sort: i + 1
          }))
        })
        uni.showToast({ title: '保存成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 600)
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
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}
.form-row:last-child {
  border-bottom: none;
}
.label {
  width: 140rpx;
  font-size: 28rpx;
  color: #6b7280;
}
.input {
  flex: 1;
  font-size: 28rpx;
}
.radio-group {
  flex: 1;
  display: flex;
  gap: 20rpx;
}
.radio {
  padding: 10rpx 32rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  font-size: 26rpx;
  color: #4b5563;
}
.radio.on {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
}
.form-row.col {
  flex-direction: column;
  align-items: flex-start;
  gap: 16rpx;
}
.dot-group {
  display: flex;
  gap: 16rpx;
  flex-wrap: wrap;
}
.dot-item {
  display: flex;
  align-items: center;
  gap: 4rpx;
  padding: 14rpx 32rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  font-size: 28rpx;
  color: #4b5563;
}
.dot-item.on {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
}
.dot-num {
  font-size: 32rpx;
  font-weight: 700;
}
.dot-unit {
  font-size: 22rpx;
}
.section-title {
  font-size: 32rpx;
  font-weight: 700;
  margin: 32rpx 8rpx 20rpx;
}
.action-item {
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 16rpx;
}
.a-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.a-name {
  font-size: 28rpx;
  font-weight: 600;
}
.a-remove {
  color: #ef4444;
  font-size: 32rpx;
}
.a-params {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-top: 20rpx;
}
.stepper {
  display: flex;
  align-items: center;
  background: #f3f4f6;
  border-radius: 16rpx;
  overflow: hidden;
}
.stepper text {
  padding: 12rpx 24rpx;
  font-size: 28rpx;
  color: #10b981;
}
.stepper-val {
  background: #fff;
  color: #1f2937 !important;
}
.p-label {
  font-size: 24rpx;
  color: #6b7280;
}
.rest-input {
  flex: 1;
  background: #f3f4f6;
  border-radius: 16rpx;
  padding: 12rpx 20rpx;
  font-size: 26rpx;
}
.half-input {
  flex: 1;
  background: #f3f4f6;
  border-radius: 16rpx;
  padding: 12rpx 20rpx;
  font-size: 26rpx;
}
.day-val {
  min-width: 92rpx;
  text-align: center;
}
.add-btn {
  text-align: center;
  padding: 24rpx;
  border: 2rpx dashed #10b981;
  border-radius: 24rpx;
  color: #10b981;
  font-size: 28rpx;
  margin-top: 8rpx;
}
.save-btn {
  width: 100%;
  padding: 24rpx 0;
  font-size: 32rpx;
  border: none;
  margin-top: 40rpx;
}
.save-btn::after {
  border: none;
}
.picker-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 100;
  display: flex;
  align-items: flex-end;
}
.picker-panel {
  width: 100%;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 32rpx;
  max-height: 70vh;
}
.picker-title {
  font-size: 32rpx;
  font-weight: 700;
  text-align: center;
  margin-bottom: 20rpx;
}
.picker-list {
  max-height: 60vh;
}
.picker-item {
  display: flex;
  justify-content: space-between;
  padding: 26rpx 8rpx;
  border-bottom: 1rpx solid #f3f4f6;
  font-size: 28rpx;
}
</style>
