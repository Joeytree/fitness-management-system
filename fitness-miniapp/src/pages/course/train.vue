<template>
  <view class="page" v-if="loaded">
    <!-- ============ 跟练中 / 休息中 ============ -->
    <view v-if="phase !== 'done'" class="train-page">
      <!-- 顶部栏 -->
      <view class="top-bar">
        <view class="tb-btn" @click="askExit">
          <text class="tb-icon">←</text>
        </view>
        <view class="tb-mid">
          <text class="tb-title">{{ course.name }}</text>
          <text class="tb-sub" v-if="phase === 'train'">动作 {{ actIdx + 1 }}/{{ actions.length }}</text>
          <text class="tb-sub rest-sub" v-else>动作 {{ actIdx + 1 }}/{{ actions.length }} · 休息中</text>
        </view>
        <view class="timer-pill">
          <text class="timer-dot">●</text>
          <text class="timer-val">{{ formatClock(elapsed) }}</text>
        </view>
      </view>

      <!-- 训练主屏 -->
      <block v-if="phase === 'train'">
        <!-- 动图演示 -->
        <view class="gif-wrap">
          <image v-if="curAction.mediaUrl && curAction.mediaType !== 2" :src="fullUrl(curAction.mediaUrl)" mode="aspectFit" class="gif-img" />
          <video v-else-if="curAction.mediaUrl && curAction.mediaType === 2" :src="fullUrl(curAction.mediaUrl)" class="gif-img" autoplay loop muted />
          <view v-else class="gif-fallback">
            <text class="gif-fb-emoji">{{ catEmoji(course.category) }}</text>
          </view>
        </view>

        <!-- 标题区：左 动作名+副信息 ｜ 右 动作要领按钮 -->
        <view class="action-info">
          <view class="ai-left">
            <text class="ai-name">{{ curAction.name }}</text>
            <text class="ai-sub">{{ subInfo }}</text>
          </view>
          <view class="tips-btn" @click="showTips = true">
            <text class="tips-ico">💡</text>
            <text class="tips-txt">动作要领</text>
            <text class="tips-arrow">▾</text>
          </view>
        </view>

        <!-- 中央倒计时圈（已完成/总数） -->
        <view class="ring-wrap" @click="tapOnce">
          <canvas canvas-id="trainRing" class="ring-canvas" />
          <view class="ring-center">
            <text class="ring-num">{{ repsDone }} / {{ curReps }}</text>
            <text class="ring-label">已完成 / 总数</text>
          </view>
        </view>

        <!-- 本组进度点 -->
        <view class="dots-row">
          <text class="dots-label">本组进度</text>
          <view class="dots">
            <view v-for="i in curReps" :key="i" class="dot" :class="{ on: i <= repsDone }" />
          </view>
          <text class="dots-num">{{ repsDone }}/{{ curReps }}</text>
        </view>

        <!-- 下一动作预告 -->
        <view class="next-card" v-if="nextPreview">
          <text class="next-cap">下一个</text>
          <view class="next-row">
            <view class="next-avatar">{{ (nextPreview.name || '动')[0] }}</view>
            <view class="next-info">
              <text class="next-name">{{ nextPreview.name }}</text>
              <text class="next-meta">{{ nextPreview.sets }} 组 · {{ nextPreview.reps }} 次</text>
            </view>
            <text class="next-chev">›</text>
          </view>
        </view>

        <!-- 三按钮：左右圆形图标 + 中间暂停胶囊 -->
        <view class="tri-bar">
          <view class="tri-circle" @click="prevSet">
            <text class="tri-circle-ico">|‹</text>
          </view>
          <view class="tri-main" @click="togglePause">
            <text class="tri-main-ico">{{ paused ? '▶' : '❚❚' }}</text>
            <text class="tri-main-txt">{{ paused ? '继续' : '暂停' }}</text>
          </view>
          <view class="tri-circle" @click="nextSet">
            <text class="tri-circle-ico">›|</text>
          </view>
        </view>
      </block>

      <!-- 组间休息 -->
      <block v-else>
        <view class="rest-wrap">
          <view class="ring-wrap rest-ring">
            <canvas canvas-id="restRing" class="ring-canvas" />
            <view class="ring-center">
              <text class="ring-num rest-num">{{ restLeft }}</text>
              <text class="ring-label">秒</text>
            </view>
          </view>
          <text class="rest-title">休息一下，准备下一组</text>
          <text class="rest-sub">下一个动作</text>
          <view class="next-card rest-next" v-if="nextPreview">
            <view class="next-row">
              <view class="next-avatar">{{ (nextPreview.name || '动')[0] }}</view>
              <view class="next-info">
                <text class="next-name">{{ nextPreview.name }}</text>
                <text class="next-meta">{{ nextPreview.sets }} 组 · {{ nextPreview.reps }} 次</text>
              </view>
              <text class="next-chev">›</text>
            </view>
          </view>
          <view class="rest-ops">
            <view class="rest-op" @click="skipRest">
              <text class="rest-op-ico">⏭</text>
              <text class="rest-op-txt">跳过</text>
            </view>
            <view class="rest-op" @click="addRest">
              <text class="rest-op-ico">＋</text>
              <text class="rest-op-txt">+30 秒</text>
            </view>
          </view>
        </view>
      </block>
    </view>

    <!-- ============ 完成总结 ============ -->
    <view v-else class="done-page">
      <view class="done-badge">
        <text class="done-check">✓</text>
      </view>
      <text class="done-title">恭喜完成训练！</text>
      <text class="done-sub">{{ course.name }}</text>

      <view class="stat-row">
        <view class="stat-item">
          <text class="stat-val">{{ stats.duration }}</text>
          <text class="stat-label">时长(分钟)</text>
        </view>
        <view class="stat-item">
          <text class="stat-val">{{ stats.calorie }}</text>
          <text class="stat-label">消耗(千卡)</text>
        </view>
        <view class="stat-item">
          <text class="stat-val">{{ stats.checkIn ? '✓' : '—' }}</text>
          <text class="stat-label">今日打卡</text>
        </view>
      </view>

      <view class="done-progress">
        <text class="dp-ico">🎓</text>
        <text class="dp-txt">学习进度：已完成</text>
      </view>

      <button class="back-btn" @click="backToCourse">返回课程</button>
    </view>

    <!-- ============ 动作要领弹窗（4 步掌握要点） ============ -->
    <view v-if="showTips" class="mask" @click="showTips = false">
      <view class="sheet" @click.stop>
        <view class="sheet-head">
          <view class="sheet-head-text">
            <text class="sheet-title">{{ curAction.name }}</text>
            <text class="sheet-subtitle">动作要领 · {{ stepList.length }} 步掌握要点</text>
          </view>
          <view class="sheet-close" @click="showTips = false"><text>✕</text></view>
        </view>
        <scroll-view scroll-y class="sheet-body">
          <view class="step-item" v-for="s in stepList" :key="s.no">
            <view class="step-no">{{ s.no }}</view>
            <view class="step-content">
              <text class="step-desc">{{ s.text }}</text>
            </view>
          </view>
        </scroll-view>
        <button class="sheet-btn" @click="showTips = false">知道了</button>
      </view>
    </view>

    <!-- ============ 退出确认弹窗 ============ -->
    <view v-if="showExit" class="mask" @click="showExit = false">
      <view class="dialog" @click.stop>
        <view class="dialog-icon">!</view>
        <text class="dialog-title">要退出本次训练吗？</text>
        <text class="dialog-desc">退出后本次训练进度将不会保存</text>
        <view class="dialog-ops">
          <view class="dialog-btn ghost" @click="showExit = false">继续训练</view>
          <view class="dialog-btn danger" @click="confirmExit">确认退出</view>
        </view>
      </view>
    </view>
  </view>
</template>

<script>
import { get, post, put } from '../../utils/request'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      id: null,
      loaded: false,
      course: {},
      actions: [],
      actIdx: 0,
      setIdx: 0,
      repsDone: 0,
      phase: 'train', // train | rest | done
      paused: false,
      restLeft: 60,
      restTotal: 60,
      elapsed: 0,
      recordId: null,
      showTips: false,
      showExit: false,
      stats: { duration: 0, calorie: 0, checkIn: false },
      mainTimer: null,
      restTimer: null
    }
  },
  computed: {
    curAction() {
      return this.actions[this.actIdx] || {}
    },
    curSets() {
      return this.curAction.sets || 1
    },
    curReps() {
      return this.curAction.reps || 0
    },
    repsLeft() {
      return Math.max(0, this.curReps - this.repsDone)
    },
    subInfo() {
      const w = this.curAction.weight
      const weightPart = w ? `${w}kg × ` : ''
      return `第 ${this.setIdx + 1} / ${this.curSets} 组 · ${weightPart}${this.curReps} 次`
    },
    stepList() {
      const raw = this.curAction.steps || ''
      return raw.split('\n')
        .map(l => l.trim())
        .filter(l => l)
        .map((l, i) => ({ no: i + 1, text: l.replace(/^\d+[\.、\):：]\s*/, '') }))
    },
    nextPreview() {
      // 当前动作还有下一组 → 预览同动作下一组；否则预览下一动作
      if (this.setIdx + 1 < this.curSets) {
        return { name: this.curAction.name, sets: this.curSets, reps: this.curReps }
      }
      const next = this.actions[this.actIdx + 1]
      if (next) {
        return { name: next.name, sets: next.sets || 1, reps: next.reps || 0 }
      }
      return null
    }
  },
  onLoad(options) {
    this.id = options.id
    this.load()
  },
  onUnload() {
    this.clearTimers()
  },
  methods: {
    fullUrl,
    catEmoji(cat) {
      const map = {
        '增肌': '💪', '减脂': '🔥', '塑形': '🌸',
        '瑜伽': '🧘', 'HIIT': '⚡', '康复拉伸': '🌿'
      }
      return map[cat] || '🏋️'
    },
    async load() {
      try {
        const data = await get('/course/' + this.id)
        this.course = data.course || {}
        this.actions = data.actions || []
        if (!this.actions.length) {
          uni.showToast({ title: '该课程暂无动作', icon: 'none' })
          setTimeout(() => uni.navigateBack(), 900)
          return
        }
        this.loaded = true
        this.$nextTick(() => {
          this.drawRing('trainRing', 0)
          this.startSession()
        })
      } catch (e) {
        setTimeout(() => uni.navigateBack(), 900)
      }
    },
    async startSession() {
      this.startMainTimer()
      try {
        const rec = await post('/training/start', { planId: Number(this.id) })
        this.recordId = rec.id
      } catch (e) { /* 未登录等情况不阻断跟练 */ }
    },
    // ===== 计时 =====
    startMainTimer() {
      this.clearMainTimer()
      this.mainTimer = setInterval(() => {
        if (!this.paused) this.elapsed++
      }, 1000)
    },
    clearMainTimer() {
      if (this.mainTimer) { clearInterval(this.mainTimer); this.mainTimer = null }
    },
    clearRestTimer() {
      if (this.restTimer) { clearInterval(this.restTimer); this.restTimer = null }
    },
    clearTimers() {
      this.clearMainTimer()
      this.clearRestTimer()
    },
    formatClock(sec) {
      const m = Math.floor(sec / 60)
      const s = sec % 60
      return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    },
    // ===== 圆环 =====
    drawRing(canvasId, progress) {
      const query = uni.createSelectorQuery().in(this)
      query.select('#' + canvasId).boundingClientRect(rect => {
        if (!rect || !rect.width) return
        const ctx = uni.createCanvasContext(canvasId, this)
        const c = rect.width / 2
        const r = c - 9
        // 底环
        ctx.beginPath()
        ctx.arc(c, c, r, 0, Math.PI * 2)
        ctx.setStrokeStyle('#E8FBF2')
        ctx.setLineWidth(11)
        ctx.setLineCap('round')
        ctx.stroke()
        // 进度环
        const start = -Math.PI / 2
        const end = start + Math.PI * 2 * Math.max(0, Math.min(1, progress))
        ctx.beginPath()
        ctx.arc(c, c, r, start, end)
        ctx.setStrokeStyle('#1FCB8A')
        ctx.setLineWidth(11)
        ctx.setLineCap('round')
        ctx.stroke()
        ctx.draw()
      }).exec()
    },
    // ===== 跟练交互 =====
    tapOnce() {
      if (this.paused) return
      if (this.repsDone >= this.curReps) {
        uni.showToast({ title: '本组已完成，点下一组', icon: 'none' })
        return
      }
      this.repsDone++
      this.drawRing('trainRing', this.curReps ? this.repsDone / this.curReps : 0)
    },
    prevSet() {
      this.clearRestTimer()
      if (this.setIdx > 0) {
        this.setIdx--
      } else if (this.actIdx > 0) {
        this.actIdx--
        this.setIdx = (this.actions[this.actIdx].sets || 1) - 1
      } else {
        uni.showToast({ title: '已经是第一个动作', icon: 'none' })
        return
      }
      this.repsDone = 0
      this.phase = 'train'
      this.$nextTick(() => this.drawRing('trainRing', 0))
    },
    nextSet() {
      if (this.paused) return
      this.completeSet()
    },
    completeSet() {
      // 当前组完成 → 进入休息（休息结束再推进到下一组）
      this.phase = 'rest'
      this.restLeft = this.restTotal
      this.startRestTimer()
      this.$nextTick(() => this.drawRing('restRing', 1))
    },
    startRestTimer() {
      this.clearRestTimer()
      this.restTimer = setInterval(() => {
        if (this.paused) return
        this.restLeft--
        if (this.restLeft <= 0) {
          this.restLeft = 0
          this.clearRestTimer()
          this.advance()
        } else {
          this.drawRing('restRing', this.restLeft / this.restTotal)
        }
      }, 1000)
    },
    skipRest() {
      this.clearRestTimer()
      this.advance()
    },
    addRest() {
      this.restLeft += 30
      this.drawRing('restRing', this.restLeft / this.restTotal)
    },
    advance() {
      this.repsDone = 0
      if (this.setIdx + 1 < this.curSets) {
        this.setIdx++
      } else if (this.actIdx + 1 < this.actions.length) {
        this.actIdx++
        this.setIdx = 0
      } else {
        this.finish()
        return
      }
      this.phase = 'train'
      this.$nextTick(() => this.drawRing('trainRing', 0))
    },
    togglePause() {
      this.paused = !this.paused
    },
    // ===== 完成 =====
    async finish() {
      this.clearTimers()
      this.phase = 'done'
      // 构造逐组明细（每个动作每组 done=1）
      const sets = []
      this.actions.forEach(a => {
        const cnt = a.sets || 1
        for (let s = 1; s <= cnt; s++) {
          sets.push({ actionId: a.id, setNo: s, reps: a.reps || 0, done: 1 })
        }
      })
      const duration = Math.max(1, Math.round(this.elapsed / 60))
      let checkIn = false
      let calorie = 0
      try {
        const data = await post('/training/complete', { recordId: this.recordId, duration, sets })
        checkIn = !!(data && data.checkIn)
        calorie = (data && data.record && data.record.calorie) || 0
      } catch (e) { /* 忽略，仍展示总结 */ }
      // 学习进度标记完成
      try {
        await put('/course/' + this.id + '/progress', { status: 2 })
      } catch (e) { /* 忽略 */ }
      this.stats = { duration, calorie, checkIn }
    },
    // ===== 退出 =====
    askExit() {
      if (this.phase === 'done') { this.backToCourse(); return }
      this.paused = true
      this.showExit = true
    },
    confirmExit() {
      this.clearTimers()
      uni.navigateBack()
    },
    backToCourse() {
      uni.navigateBack()
    }
  }
}
</script>

<style>
.page {
  min-height: 100vh;
  background: #FFFFFF;
}

/* ===== 顶部栏 ===== */
.top-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  padding-top: calc(env(safe-area-inset-top) + 16rpx);
  background: #FFFFFF;
  border-bottom: 1rpx solid #F0F2F5;
}
.tb-btn {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #F4F6F8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.tb-icon {
  font-size: 32rpx;
  color: #14181F;
  font-weight: 600;
}
.tb-mid {
  flex: 1;
  display: flex;
  flex-direction: column;
  margin-left: 20rpx;
  min-width: 0;
}
.tb-title {
  font-size: 30rpx;
  font-weight: 700;
  color: #14181F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tb-sub {
  font-size: 22rpx;
  color: #8A94A6;
  margin-top: 4rpx;
}
.rest-sub {
  color: #1FCB8A;
  font-weight: 600;
}
.timer-pill {
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
  background: #F4F6F8;
  flex-shrink: 0;
}
.timer-dot {
  color: #1FCB8A;
  font-size: 16rpx;
}
.timer-val {
  font-size: 26rpx;
  font-weight: 700;
  color: #14181F;
  font-variant-numeric: tabular-nums;
}

/* ===== 动图 ===== */
.gif-wrap {
  margin: 24rpx 24rpx 0;
  height: 420rpx;
  border-radius: 24rpx;
  background: #F4F6F8;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.gif-img {
  width: 100%;
  height: 100%;
}
.gif-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
}
.gif-fb-emoji {
  font-size: 140rpx;
  opacity: 0.7;
}

/* ===== 标题区（动作名+副信息 ｜ 动作要领按钮） ===== */
.action-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 28rpx 32rpx 0;
}
.ai-left {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.ai-name {
  font-size: 44rpx;
  font-weight: 700;
  color: #14181F;
}
.ai-sub {
  font-size: 24rpx;
  color: #8A94A6;
  margin-top: 8rpx;
}

/* ===== 倒计时圈 ===== */
.ring-wrap {
  position: relative;
  width: 340rpx;
  height: 340rpx;
  margin: 40rpx auto 0;
}
.ring-canvas {
  width: 340rpx;
  height: 340rpx;
}
.ring-center {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.ring-num {
  font-size: 88rpx;
  font-weight: 800;
  color: #14181F;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.ring-label {
  font-size: 24rpx;
  color: #8A94A6;
  margin-top: 14rpx;
}
.rest-num {
  color: #1FCB8A;
}

/* ===== 动作要领按钮（标题右侧小胶囊） ===== */
.tips-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8rpx;
  padding: 14rpx 24rpx;
  border-radius: 999rpx;
  background: #F4F6F8;
}
.tips-ico {
  font-size: 24rpx;
}
.tips-txt {
  font-size: 26rpx;
  font-weight: 600;
  color: #14181F;
}
.tips-arrow {
  font-size: 22rpx;
  color: #9CA3AF;
}

/* ===== 本组进度点 ===== */
.dots-row {
  display: flex;
  align-items: center;
  margin: 28rpx 32rpx 0;
}
.dots-label {
  font-size: 24rpx;
  color: #4B5563;
  flex-shrink: 0;
  margin-right: 16rpx;
}
.dots {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx;
}
.dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  background: #E5E9EF;
}
.dot.on {
  background: #1FCB8A;
}
.dots-num {
  font-size: 24rpx;
  font-weight: 700;
  color: #1FCB8A;
  margin-left: 16rpx;
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}

/* ===== 三按钮（左右圆形图标 + 中间暂停胶囊） ===== */
.tri-bar {
  display: flex;
  align-items: center;
  gap: 24rpx;
  margin: 40rpx 32rpx 0;
}
.tri-circle {
  width: 104rpx;
  height: 104rpx;
  border-radius: 50%;
  background: #F4F6F8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.tri-circle-ico {
  font-size: 34rpx;
  color: #14181F;
  font-weight: 700;
}
.tri-main {
  flex: 1;
  height: 104rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #1FCB8A, #0FA56C);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  box-shadow: 0 8rpx 24rpx rgba(31, 203, 138, 0.35);
}
.tri-main-ico {
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
}
.tri-main-txt {
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
}

/* ===== 下一动作预告 ===== */
.next-card {
  margin: 40rpx 32rpx 0;
  background: #F4F6F8;
  border-radius: 20rpx;
  padding: 20rpx;
}
.next-cap {
  font-size: 22rpx;
  color: #8A94A6;
  display: block;
  margin-bottom: 12rpx;
}
.next-row {
  display: flex;
  align-items: center;
}
.next-avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 16rpx;
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  color: #0FA56C;
  font-size: 32rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.next-info {
  flex: 1;
  margin-left: 20rpx;
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.next-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #14181F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.next-meta {
  font-size: 24rpx;
  color: #8A94A6;
  margin-top: 6rpx;
}
.next-chev {
  font-size: 36rpx;
  color: #B5BCC7;
}

/* ===== 组间休息 ===== */
.rest-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 60rpx;
}
.rest-ring {
  margin: 0 auto;
}
.rest-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #14181F;
  margin-top: 40rpx;
}
.rest-sub {
  font-size: 24rpx;
  color: #8A94A6;
  margin-top: 16rpx;
}
.rest-next {
  width: 600rpx;
  margin-top: 32rpx;
}
.rest-ops {
  display: flex;
  gap: 24rpx;
  margin-top: 48rpx;
}
.rest-op {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 160rpx;
  height: 120rpx;
  border-radius: 24rpx;
  background: #F4F6F8;
  gap: 6rpx;
}
.rest-op-ico {
  font-size: 36rpx;
  color: #14181F;
  font-weight: 700;
}
.rest-op-txt {
  font-size: 24rpx;
  color: #4B5563;
}

/* ===== 完成总结 ===== */
.done-page {
  min-height: 100vh;
  background: #FFFFFF;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 120rpx 40rpx 0;
  padding-top: calc(env(safe-area-inset-top) + 120rpx);
}
.done-badge {
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #1FCB8A, #0FA56C);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 12rpx 32rpx rgba(31, 203, 138, 0.4);
}
.done-check {
  color: #fff;
  font-size: 88rpx;
  font-weight: 800;
}
.done-title {
  font-size: 44rpx;
  font-weight: 800;
  color: #14181F;
  margin-top: 40rpx;
}
.done-sub {
  font-size: 28rpx;
  color: #8A94A6;
  margin-top: 12rpx;
}
.stat-row {
  display: flex;
  width: 100%;
  margin-top: 64rpx;
  background: #F4F6F8;
  border-radius: 24rpx;
  padding: 32rpx 0;
}
.stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.stat-val {
  font-size: 44rpx;
  font-weight: 800;
  color: #14181F;
}
.stat-label {
  font-size: 24rpx;
  color: #8A94A6;
}
.done-progress {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-top: 40rpx;
  padding: 24rpx 36rpx;
  border-radius: 999rpx;
  background: #ECFDF5;
}
.dp-ico {
  font-size: 32rpx;
}
.dp-txt {
  font-size: 28rpx;
  font-weight: 600;
  color: #0FA56C;
}
.back-btn {
  width: 100%;
  margin-top: 80rpx;
  height: 96rpx;
  line-height: 96rpx;
  background: linear-gradient(135deg, #1FCB8A, #0FA56C);
  color: #fff;
  font-size: 32rpx;
  font-weight: 700;
  letter-spacing: 2rpx;
  border-radius: 999rpx;
  border: none;
  box-shadow: 0 8rpx 24rpx rgba(31, 203, 138, 0.35);
}
.back-btn::after {
  border: none;
}

/* ===== 弹窗通用 ===== */
.mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 动作要领弹窗 */
.sheet {
  width: 620rpx;
  max-height: 76vh;
  background: #fff;
  border-radius: 28rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.sheet-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx 32rpx;
  border-bottom: 1rpx solid #F0F2F5;
}
.sheet-head-text {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
}
.sheet-title {
  font-size: 32rpx;
  font-weight: 700;
  color: #14181F;
}
.sheet-subtitle {
  font-size: 22rpx;
  color: #8A94A6;
  margin-top: 6rpx;
}
.sheet-close {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #F4F6F8;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8A94A6;
  font-size: 28rpx;
  flex-shrink: 0;
}
.sheet-body {
  flex: 1;
  padding: 20rpx 32rpx;
  max-height: 52vh;
}
.step-item {
  display: flex;
  gap: 20rpx;
  padding: 24rpx 20rpx;
  background: #F4F6F8;
  border-radius: 16rpx;
  margin-bottom: 16rpx;
}
.step-item:last-child {
  margin-bottom: 0;
}
.step-no {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  background: #1FCB8A;
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.step-content {
  flex: 1;
  min-width: 0;
}
.step-desc {
  font-size: 26rpx;
  color: #4B5563;
  line-height: 1.6;
}
.sheet-btn {
  margin: 20rpx 32rpx 28rpx;
  height: 88rpx;
  line-height: 88rpx;
  background: linear-gradient(135deg, #1FCB8A, #0FA56C);
  color: #fff;
  font-size: 30rpx;
  font-weight: 700;
  border-radius: 999rpx;
  border: none;
}
.sheet-btn::after {
  border: none;
}

/* 退出确认弹窗 */
.dialog {
  width: 560rpx;
  background: #fff;
  border-radius: 28rpx;
  padding: 48rpx 36rpx 36rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.dialog-icon {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: #FFF1F0;
  color: #EF4444;
  font-size: 56rpx;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
}
.dialog-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #14181F;
  margin-top: 28rpx;
}
.dialog-desc {
  font-size: 26rpx;
  color: #8A94A6;
  margin-top: 12rpx;
}
.dialog-ops {
  display: flex;
  gap: 20rpx;
  width: 100%;
  margin-top: 40rpx;
}
.dialog-btn {
  flex: 1;
  height: 84rpx;
  line-height: 84rpx;
  text-align: center;
  border-radius: 999rpx;
  font-size: 28rpx;
  font-weight: 600;
}
.dialog-btn.ghost {
  background: #F4F6F8;
  color: #4B5563;
}
.dialog-btn.danger {
  background: #EF4444;
  color: #fff;
}
</style>
