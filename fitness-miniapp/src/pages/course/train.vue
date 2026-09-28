<template>
  <view class="page" v-if="loaded">
    <!-- ============ 跟练中 / 休息中 ============ -->
    <view v-if="phase !== 'done'" class="train-page">
      <!-- 顶部栏 — paddingTop 用动态 statusBarH 替代 env() -->
      <view class="top-bar" :style="{ paddingTop: statusBarH + 'px' }">
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

        <!-- 中央圆圈 — 单击切暂停/继续 -->
        <!-- 注意：mp-weixin 必须同时给 canvas-id 和 id，否则 createSelectorQuery().select('#xxx') 选不到 canvas 元素，boundingClientRect 返回 width=0，圆圈永远画不出来 -->
        <view class="ring-wrap" :class="{ paused: paused }" @click="togglePause">
          <canvas canvas-id="trainRing" id="trainRing" class="ring-canvas" />
          <view class="ring-center">
            <text class="ring-num" v-if="!paused">{{ repsDone }} / {{ curReps }}</text>
            <text class="ring-num paused-ico" v-else>⏸</text>
            <text class="ring-label" v-if="!paused">点击圆圈暂停</text>
            <text class="ring-label" v-else>已暂停 · 点击继续</text>
          </view>
        </view>

        <!-- 实时倒计时：距离下次自动 +1 还有多少秒 -->
        <view class="countdown-row" v-if="!paused">
          <text class="cd-ico">⏱</text>
          <text class="cd-val">{{ repCountdown.toFixed(1) }}s</text>
          <text class="cd-suf">后自动 +1</text>
        </view>

        <!-- 本组进度点 -->
        <view class="dots-row">
          <text class="dots-label">本组进度</text>
          <view class="dots">
            <view v-for="i in curReps" :key="i" class="dot" :class="{ on: i <= repsDone }" />
          </view>
          <text class="dots-num">{{ repsDone }}/{{ curReps }}</text>
        </view>

        <!-- 主操作区：主报数 + 切换动作（点击弹本课动作选择器） -->
        <view class="action-btns">
          <view class="a-btn" @click="showIntervalPicker = true">
            <text class="a-btn-ico">⏲</text>
            <text class="a-btn-txt">主报数 {{ repInterval }} 秒</text>
          </view>
          <view class="a-btn a-btn-primary" @click="pickAction">
            <text class="a-btn-ico">⏭</text>
            <text class="a-btn-txt">切换动作</text>
          </view>
        </view>

        <!-- 下一动作预告 — 可点击跳到该动作 -->
        <view class="next-card" v-if="nextPreview" @click="jumpToNext">
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

        <!-- 底部三按钮：上一组 | 暂停 | 下一组 -->
        <view class="bottom-bar">
          <view class="bb-side" @click="goPrevSet">
            <text class="bb-ico">|‹</text>
          </view>
          <view class="bb-main" @click="togglePause">
            <text class="bb-ico-main">{{ paused ? '▶' : '‖' }}</text>
            <text class="bb-txt">{{ paused ? '继续' : '暂停' }}</text>
          </view>
          <view class="bb-side" @click="goNextSet">
            <text class="bb-ico">›|</text>
          </view>
        </view>
      </block>

      <!-- 组间休息 -->
      <block v-else>
        <view class="rest-wrap">
          <view class="ring-wrap rest-ring">
            <canvas canvas-id="restRing" id="restRing" class="ring-canvas" />
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
    <view v-else class="done-page" :style="{ paddingTop: (statusBarH + 120) + 'px' }">
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

    <!-- ============ 动作要领弹窗 ============ -->
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

    <!-- ============ 主报数间隔选择弹窗 ============ -->
    <view v-if="showIntervalPicker" class="mask" @click="showIntervalPicker = false">
      <view class="interval-sheet" @click.stop>
        <text class="interval-title">设置自动报数间隔</text>
        <text class="interval-sub">每隔指定时间自动 +1，无需手动点击</text>
        <view class="interval-grid">
          <view
            v-for="s in intervalOptions"
            :key="s"
            class="interval-cell"
            :class="{ on: s === repInterval }"
            @click="changeInterval(s)"
          >
            <text class="ic-num">{{ s }}</text>
            <text class="ic-unit">秒/次</text>
          </view>
        </view>
        <button class="sheet-btn" @click="showIntervalPicker = false">完成</button>
      </view>
    </view>

    <!-- ============ 切换动作弹窗：本节课全部动作可选 ============ -->
    <view v-if="showActionPicker" class="mask" @click="showActionPicker = false">
      <view class="picker-sheet" @click.stop>
        <view class="sheet-head">
          <view class="sheet-head-text">
            <text class="sheet-title">切换动作</text>
            <text class="sheet-subtitle">{{ course.name }} · 共 {{ actions.length }} 个动作</text>
          </view>
          <view class="sheet-close" @click="showActionPicker = false"><text>✕</text></view>
        </view>
        <scroll-view scroll-y class="sheet-body picker-body">
          <view
            v-for="(a, idx) in actions"
            :key="a.id || idx"
            class="pick-row"
            :class="{ current: idx === actIdx }"
            @click="jumpToAction(idx)"
          >
            <view class="pick-no">{{ idx + 1 }}</view>
            <view class="pick-info">
              <text class="pick-name">{{ a.name }}</text>
              <text class="pick-meta">{{ a.sets || 1 }} 组 × {{ a.reps || 0 }} 次<text v-if="a.weight"> · {{ a.weight }}kg</text></text>
            </view>
            <text class="pick-tag" v-if="idx === actIdx">当前</text>
            <text class="pick-chev" v-else>›</text>
          </view>
        </scroll-view>
        <view class="sheet-btn-wrap">
          <button class="sheet-btn" @click="showActionPicker = false">取消</button>
        </view>
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
    // mp-weixin 不支持 env(safe-area-inset-top)，必须用 statusBarHeight
    let sbh = 0
    try { sbh = uni.getSystemInfoSync().statusBarHeight || 0 } catch (e) {}
    return {
      id: null,
      statusBarH: sbh,
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
      showIntervalPicker: false,   // 主报数间隔 sheet
      showActionPicker: false,     // 切换动作 sheet（本节课全部动作）
      repInterval: 1.8,            // 自动 +1 间隔（秒）
      repCountdown: 1.8,           // 距下次 +1 倒计时
      stats: { duration: 0, calorie: 0, checkIn: false },
      mainTimer: null,
      restTimer: null,
      repTimer: null               // 自动 +1 计时器
    }
  },
  computed: {
    // 可选的报数间隔档位
    intervalOptions() {
      return [0.6, 1.0, 1.4, 1.8, 2.5, 3.5, 5.0]
    },
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
        return { name: this.curAction.name, sets: this.curSets, reps: this.curReps, sameAction: true }
      }
      const next = this.actions[this.actIdx + 1]
      if (next) {
        return { name: next.name, sets: next.sets || 1, reps: next.reps || 0, sameAction: false }
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
        // setTimeout 替代 $nextTick：$nextTick 在 mp-weixin 中可能在 canvas layout 前回调，rect.width=0
        setTimeout(() => {
          this.drawRing('trainRing', 0)
          this.startSession()
        }, 100)
      } catch (e) {
        setTimeout(() => uni.navigateBack(), 900)
      }
    },
    async startSession() {
      this.startMainTimer()
      this.startRepTimer()
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
    clearRepTimer() {
      if (this.repTimer) { clearInterval(this.repTimer); this.repTimer = null }
    },
    clearTimers() {
      this.clearMainTimer()
      this.clearRestTimer()
      this.clearRepTimer()
    },
    formatClock(sec) {
      const m = Math.floor(sec / 60)
      const s = sec % 60
      return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    },
    // ===== 圆环 =====
// mp-weixin canvas-id 不等同于 id 属性，selector 偶尔拿不到 rect.width；做 12 次重试确保圆圈画出来
    drawRing(canvasId, progress, retry = 0) {
      const query = uni.createSelectorQuery().in(this)
      query.select('#' + canvasId).boundingClientRect(rect => {
        if (!rect || !rect.width) {
          if (retry < 12) setTimeout(() => this.drawRing(canvasId, progress, retry + 1), 80)
          return
        }
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
    // ===== 自动报数（核心：每 repInterval 秒 repsDone +1） =====
    startRepTimer() {
      this.clearRepTimer()
      this.repCountdown = this.repInterval
      this.repTimer = setInterval(() => {
        if (this.paused || this.phase !== 'train') return
        // 0.1s 精度倒数
        this.repCountdown = Math.max(0, +(this.repCountdown - 0.1).toFixed(1))
        if (this.repCountdown <= 0) {
          this.autoCount()
          this.repCountdown = this.repInterval
        }
      }, 100)
    },
    autoCount() {
      if (this.repsDone >= this.curReps) return
      this.repsDone++
      this.drawRing('trainRing', this.curReps ? this.repsDone / this.curReps : 0)
      // 本组完成 → 仅在最后一 +1 时给一次震动反馈（不再每次都震）
      if (this.repsDone >= this.curReps) {
        try { uni.vibrateShort({ type: 'medium' }) } catch (e) {}
        this.clearRepTimer()
        // 短延迟让用户感知"最后一个 +1"已落定
        setTimeout(() => this.completeSet(), 400)
      }
    },
    changeInterval(s) {
      this.repInterval = s
      this.repCountdown = s
      // 间隔变化时立即重启 tick
      if (this.phase === 'train' && !this.paused) {
        this.startRepTimer()
      }
    },
    // ===== 切换动作（弹本节课全部动作可选项） =====
    pickAction() {
      this.showActionPicker = true
    },
    jumpToAction(idx) {
      if (idx === this.actIdx) {
        // 当前动作 — 仅关闭弹窗
        this.showActionPicker = false
        return
      }
      this.showActionPicker = false
      this.clearRepTimer()
      this.clearRestTimer()
      this.actIdx = idx
      this.setIdx = 0
      this.repsDone = 0
      this.phase = 'train'
      this.repCountdown = this.repInterval
      setTimeout(() => this.drawRing('trainRing', 0), 100)
      this.startRepTimer()
      uni.showToast({ title: `已切换到 ${this.actions[idx].name}`, icon: 'none', duration: 900 })
    },
    jumpToNext() {
      // 「下一个」卡片：跳到当前组的下一组 或 下一动作的第一个组
      if (this.setIdx + 1 < this.curSets) {
        this.setIdx++
        this.repsDone = 0
        this.phase = 'train'
        this.repCountdown = this.repInterval
        setTimeout(() => this.drawRing('trainRing', 0), 100)
        this.startRepTimer()
      } else if (this.actIdx + 1 < this.actions.length) {
        this.actIdx++
        this.setIdx = 0
        this.repsDone = 0
        this.phase = 'train'
        this.repCountdown = this.repInterval
        setTimeout(() => this.drawRing('trainRing', 0), 100)
        this.startRepTimer()
      } else {
        this.finish()
      }
    },
    // ===== 底部三按钮：上一组 / 下一组 =====
    goPrevSet() {
      if (this.phase !== 'train') return
      this.clearRepTimer()
      if (this.setIdx > 0) {
        this.setIdx--
      } else if (this.actIdx > 0) {
        this.actIdx--
        this.setIdx = (this.actions[this.actIdx].sets || 1) - 1
      } else {
        uni.showToast({ title: '已经是第一组', icon: 'none' })
        this.startRepTimer()
        return
      }
      this.repsDone = 0
      this.repCountdown = this.repInterval
      setTimeout(() => this.drawRing('trainRing', 0), 100)
      this.startRepTimer()
    },
    goNextSet() {
      // 与完整本组流程对齐：标记完成 → 短暂休息 → 进入下一组
      if (this.phase !== 'train') return
      this.clearRepTimer()
      this.completeSet()
    },
    togglePause() {
      this.paused = !this.paused
      if (this.paused) {
        this.clearRepTimer()
      } else if (this.phase === 'train') {
        this.startRepTimer()
      }
    },
    completeSet() {
      this.clearRepTimer()
      this.phase = 'rest'
      this.restLeft = this.restTotal
      this.startRestTimer()
      setTimeout(() => this.drawRing('restRing', 1), 100)
    },
    startRestTimer() {
      this.clearRestTimer()
      this.restTimer = setInterval(() => {
        if (this.paused) return
        this.restLeft--
        if (this.restLeft <= 0) {
          this.restLeft = 0
          this.clearRestTimer()
          this.advance() // 休息结束自动跳下一组/下一动作
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
      this.repCountdown = this.repInterval
      setTimeout(() => this.drawRing('trainRing', 0), 100)
      this.startRepTimer()
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
      this.clearRepTimer()
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
  padding-bottom: env(safe-area-inset-bottom);
}

/* ===== 顶部栏 ===== */
.top-bar {
  display: flex;
  align-items: center;
  /* 右侧 200rpx 给微信原生胶囊（⋯⊙）让位，避免计时器与之重叠 */
  padding: 16rpx 200rpx 16rpx 24rpx;
  /* padding-top 由 inline style 动态绑定 statusBarH（mp-weixin 不支持 env） */
  padding-top: 0;
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
  margin: 32rpx auto 0;
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
.paused-ico {
  font-size: 96rpx;
  color: #B5BCC7;
}
.ring-label {
  font-size: 22rpx;
  color: #8A94A6;
  margin-top: 14rpx;
}
.rest-num {
  color: #1FCB8A;
}
.ring-wrap.paused .ring-canvas {
  opacity: 0.4;
}

/* ===== 实时倒计时（下次 +1 还有多少秒） ===== */
.countdown-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8rpx;
  margin-top: 18rpx;
  font-size: 24rpx;
  color: #4B5563;
}
.cd-ico {
  font-size: 24rpx;
}
.cd-val {
  font-size: 30rpx;
  font-weight: 700;
  color: #1FCB8A;
  font-variant-numeric: tabular-nums;
  letter-spacing: 1rpx;
}
.cd-suf {
  color: #8A94A6;
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
  margin: 24rpx 32rpx 0;
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

/* ===== 主操作区（主报数 + 切换动作） ===== */
.action-btns {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin: 32rpx 32rpx 0;
}
.a-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  height: 88rpx;
  border-radius: 999rpx;
  background: #F4F6F8;
  font-size: 26rpx;
  font-weight: 600;
  color: #14181F;
}
.a-btn-primary {
  background: linear-gradient(135deg, #1FCB8A, #0FA56C);
  color: #fff;
  box-shadow: 0 6rpx 18rpx rgba(31, 203, 138, 0.32);
}
.a-btn-ico {
  font-size: 28rpx;
  line-height: 1;
}
.a-btn-txt {
  font-size: 28rpx;
  font-weight: 600;
  line-height: 1;
}

/* ===== 下一动作预告 ===== */
.next-card {
  margin: 28rpx 32rpx 0;
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

/* ===== 底部三按钮 ===== */
.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  /* iOS 安全区：env 在 mp-weixin 不可靠，固定底部 24rpx 兜底 */
  padding: 20rpx 32rpx calc(20rpx + env(safe-area-inset-bottom));
  padding-bottom: calc(20rpx + 24rpx);
  background: #FFFFFF;
  display: flex;
  align-items: center;
  gap: 20rpx;
  box-shadow: 0 -4rpx 20rpx rgba(20, 24, 31, 0.04);
  z-index: 10;
}
.bb-side {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: #F4F6F8;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.bb-ico {
  font-size: 36rpx;
  font-weight: 800;
  color: #14181F;
  line-height: 1;
}
.bb-main {
  flex: 1;
  height: 96rpx;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #1FCB8A, #0FA56C);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  box-shadow: 0 6rpx 18rpx rgba(31, 203, 138, 0.32);
}
.bb-ico-main {
  font-size: 32rpx;
  font-weight: 800;
  color: #FFFFFF;
  line-height: 1;
}
.bb-txt {
  font-size: 32rpx;
  font-weight: 700;
  color: #FFFFFF;
  line-height: 1;
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
  /* padding-top 由 inline style 动态绑定 statusBarH */
  padding-top: 120rpx;
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
.sheet-btn-wrap {
  padding: 20rpx 32rpx 28rpx;
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
  width: 100%;
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

/* ===== 主报数间隔选择弹窗 ===== */
.interval-sheet {
  width: 600rpx;
  background: #fff;
  border-radius: 28rpx;
  padding: 40rpx 32rpx 24rpx;
  display: flex;
  flex-direction: column;
}
.interval-title {
  font-size: 34rpx;
  font-weight: 700;
  color: #14181F;
  text-align: center;
}
.interval-sub {
  font-size: 24rpx;
  color: #8A94A6;
  text-align: center;
  margin-top: 12rpx;
}
.interval-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16rpx;
  margin: 32rpx 0 8rpx;
}
.interval-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 130rpx;
  border-radius: 20rpx;
  background: #F4F6F8;
  transition: all 0.15s;
}
.interval-cell.on {
  background: linear-gradient(135deg, #1FCB8A, #0FA56C);
  box-shadow: 0 4rpx 12rpx rgba(31, 203, 138, 0.3);
}
.ic-num {
  font-size: 36rpx;
  font-weight: 800;
  color: #14181F;
  font-variant-numeric: tabular-nums;
}
.ic-unit {
  font-size: 20rpx;
  color: #8A94A6;
  margin-top: 4rpx;
}
.interval-cell.on .ic-num {
  color: #fff;
}
.interval-cell.on .ic-unit {
  color: rgba(255, 255, 255, 0.85);
}

/* ===== 切换动作弹窗 ===== */
.picker-sheet {
  width: 620rpx;
  max-height: 80vh;
  background: #fff;
  border-radius: 28rpx;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.picker-body {
  max-height: 56vh;
  padding: 16rpx 24rpx 24rpx;
}
.pick-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 20rpx;
  border-radius: 20rpx;
  background: #F4F6F8;
  margin-bottom: 16rpx;
}
.pick-row.current {
  background: linear-gradient(135deg, #ECFDF5, #D1FAE5);
  border: 2rpx solid #1FCB8A;
}
.pick-no {
  width: 64rpx;
  height: 64rpx;
  border-radius: 50%;
  background: #FFFFFF;
  color: #14181F;
  font-size: 28rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.pick-row.current .pick-no {
  background: #1FCB8A;
  color: #FFFFFF;
}
.pick-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.pick-name {
  font-size: 28rpx;
  font-weight: 600;
  color: #14181F;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pick-meta {
  font-size: 22rpx;
  color: #8A94A6;
  margin-top: 6rpx;
}
.pick-tag {
  font-size: 22rpx;
  font-weight: 600;
  color: #0FA56C;
  background: #FFFFFF;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  flex-shrink: 0;
}
.pick-chev {
  font-size: 40rpx;
  color: #B5BCC7;
  flex-shrink: 0;
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
