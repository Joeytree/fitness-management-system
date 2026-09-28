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

    <view class="section-title">课程列表（每天完成一门）</view>
    <view v-for="(c, i) in selected" :key="c.id" class="course-item">
      <view class="c-head">
        <text class="c-name">{{ c.name }}</text>
        <text class="c-remove" @click="removeCourse(i)">✕</text>
      </view>
      <view class="c-day">
        <view class="stepper">
          <text @click="dec(c)">－</text>
          <text class="stepper-val day-val">第{{ c.day }}天</text>
          <text @click="inc(c)">＋</text>
        </view>
        <text class="p-label">按天顺序自动排列</text>
      </view>
    </view>

    <view class="add-btn" @click="openPicker">＋ 添加课程</view>

    <button class="btn-primary save-btn" @click="save">保存计划</button>

    <!-- 课程选择弹窗 -->
    <view v-if="pickerVisible" class="picker-mask" @click="pickerVisible = false">
      <view class="picker-panel" @click.stop>
        <view class="picker-title">选择课程</view>
        <scroll-view scroll-y class="picker-list" @scrolltolower="loadMoreCourses">
          <view v-for="c in courses" :key="c.id" class="picker-item" @click="pickCourse(c)">
            <view class="pi-left">
              <text class="pi-name">{{ c.name }}</text>
              <text class="muted">{{ c.category }} · {{ c.part || '全身' }} · {{ c.duration }}分钟</text>
            </view>
            <text class="pi-add">＋</text>
          </view>
          <view v-if="courses.length" class="load-tip">{{ loading ? '加载中…' : (hasMore ? '上拉加载更多' : '没有更多了') }}</view>
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
        { value: 3, label: '塑形' },
        { value: 4, label: '保持' }
      ],
      courses: [],
      selected: [],
      pickerVisible: false,
      page: 1,
      hasMore: true,
      loading: false
    }
  },
  onLoad() {
    this.loadCourses()
  },
  methods: {
    async loadCourses() {
      this.page = 1
      this.hasMore = true
      this.loading = false
      try {
        const data = await get('/course', { page: 1, pageSize: 20 })
        this.courses = data.list || []
        this.hasMore = this.courses.length < (data.total || 0)
      } catch (e) { /* 忽略 */ }
    },
    async loadMoreCourses() {
      if (this.loading || !this.hasMore) return
      this.loading = true
      try {
        const data = await get('/course', { page: this.page + 1, pageSize: 20 })
        const list = data.list || []
        if (list.length) {
          this.courses = this.courses.concat(list)
          this.page += 1
          this.hasMore = this.courses.length < (data.total || 0)
        } else {
          this.hasMore = false
        }
      } catch (e) { /* 忽略 */ }
      this.loading = false
    },
    openPicker() {
      this.pickerVisible = true
    },
    pickCourse(c) {
      if (this.selected.some(x => x.courseId === c.id)) {
        uni.showToast({ title: '该课程已添加', icon: 'none' })
        return
      }
      this.selected.push({ courseId: c.id, name: c.name, day: this.selected.length + 1 })
      this.pickerVisible = false
    },
    removeCourse(i) {
      this.selected.splice(i, 1)
      // 重新按顺序编号
      this.selected.forEach((c, idx) => { c.day = idx + 1 })
    },
    inc(c) {
      if (c.day < 7) c.day = c.day + 1
    },
    dec(c) {
      if (c.day > 1) c.day = c.day - 1
    },
    async save() {
      if (!this.name) {
        uni.showToast({ title: '请填写计划名', icon: 'none' })
        return
      }
      if (!this.selected.length) {
        uni.showToast({ title: '请添加课程', icon: 'none' })
        return
      }
      try {
        await post('/plan', {
          name: this.name,
          goal: this.goal,
          level: 1,
          cycle: `每周${this.freq}练·每次${this.duration}分钟`,
          courses: this.selected.map((c, i) => ({
            courseId: c.courseId,
            dayNo: c.day || 1,
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
.course-item {
  background: #fff;
  border-radius: 24rpx;
  padding: 24rpx 28rpx;
  margin-bottom: 16rpx;
}
.c-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.c-name {
  font-size: 28rpx;
  font-weight: 600;
}
.c-remove {
  color: #ef4444;
  font-size: 32rpx;
}
.c-day {
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
.day-val {
  min-width: 92rpx;
  text-align: center;
}
.p-label {
  font-size: 24rpx;
  color: #6b7280;
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
  height: 56vh;
}
.picker-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 26rpx 8rpx;
  border-bottom: 1rpx solid #f3f4f6;
  font-size: 28rpx;
}
.pi-left {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}
.pi-name {
  font-size: 28rpx;
  font-weight: 600;
}
.pi-add {
  color: #10b981;
  font-size: 36rpx;
  flex-shrink: 0;
  margin-left: 16rpx;
}
.load-tip {
  text-align: center;
  padding: 24rpx 0;
  font-size: 24rpx;
  color: #9ca3af;
}
</style>
