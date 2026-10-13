<template>
  <view class="chat-page">
    <scroll-view scroll-y class="msg-list" :scroll-into-view="scrollTo" scroll-with-animation>
      <view v-if="!messages.length && !waiting" class="empty-tip">
        <text class="empty-emoji">🤖</text>
        <text class="empty-text">我是你的 AI 私教\n描述训练目标、器械和时间，我来为你定制课程</text>
      </view>

      <view v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
        <view v-if="m.role === 'assistant'" class="avatar ai">🤖</view>
        <view class="bubble" :class="m.role">
          <text v-if="m.content" class="bubble-text">{{ m.content }}</text>
          <view v-if="m.type === 'course' && m.data && m.data.course" class="course-card">
            <view class="cc-head">
              <text class="cc-name">{{ m.data.course.name }}</text>
              <text class="cc-badge">{{ m.data.course.duration }}分钟 · 约{{ m.data.course.calorie }}千卡</text>
            </view>
            <view class="cc-list">
              <view v-for="a in m.data.course.actions" :key="a.actionId" class="cc-item">
                <text class="cc-role">{{ roleIcon(a.role) }}</text>
                <text class="cc-aname">{{ a.name }}</text>
                <text class="cc-sets">{{ a.sets }}×{{ a.reps }}</text>
              </view>
            </view>
            <view class="cc-btn" @click="startTrain(m.data.courseId)">开始训练</view>
          </view>
          <view v-if="m.type === 'plan' && m.data" class="course-card">
            <view class="cc-head">
              <text class="cc-name">{{ m.data.name }}</text>
              <text class="cc-badge">共 {{ (m.data.days || []).length }} 次课程</text>
            </view>
            <view class="cc-list">
              <view v-for="d in (m.data.days || []).slice(0, 8)" :key="d.dayNo" class="cc-item">
                <text class="cc-role">📅</text>
                <text class="cc-aname">第{{ d.dayNo }}天 · {{ d.courseName }}</text>
              </view>
              <text v-if="(m.data.days || []).length > 8" class="cc-more">… 共 {{ m.data.days.length }} 天</text>
            </view>
            <view class="cc-btn" @click="goPlan(m.data.planId)">查看计划</view>
          </view>
        </view>
        <view v-if="m.role === 'user'" class="avatar user">{{ avatarText }}</view>
      </view>

      <view v-if="waiting" class="msg-row assistant">
        <view class="avatar ai">🤖</view>
        <view class="bubble assistant"><text class="typing">正在思考…</text></view>
      </view>
      <view id="msg-bottom" style="height: 24rpx;"></view>
    </scroll-view>

    <view v-if="!messages.length && !waiting" class="quick">
      <text class="quick-title">试试这样问</text>
      <view v-for="q in quickQuestions" :key="q" class="quick-item" @click="send(q)">{{ q }}</view>
    </view>

    <view class="input-bar">
      <input
        class="input"
        v-model="inputText"
        placeholder="如：我只有哑铃，想练胸 30 分钟"
        confirm-type="send"
        @confirm="send()"
      />
      <button class="send-btn" :disabled="!inputText.trim() || waiting" @click="send()">发送</button>
    </view>
  </view>
</template>

<script>
import config from '../../utils/config'
import { getUserInfo } from '../../utils/index'

export default {
  data() {
    return {
      messages: [],
      inputText: '',
      waiting: false,
      socketConnected: false,
      scrollTo: '',
      avatarText: '我',
      quickQuestions: [
        '我只有哑铃，想练胸 30 分钟',
        '帮我安排一次减脂训练',
        '每天要吃多少蛋白质？'
      ]
    }
  },
  onLoad() {
    const user = getUserInfo()
    this.avatarText = (user.nickname || '我').slice(0, 1)
    this.connect()
    // WebSocket 心跳：每 30 秒发 ping 保活
    this.heartbeatTimer = setInterval(() => {
      if (this.socketConnected) {
        uni.sendSocketMessage({ data: JSON.stringify({ type: 'ping' }) })
      }
    }, 30000)
  },
  onUnload() {
    if (this.heartbeatTimer) clearInterval(this.heartbeatTimer)
    uni.closeSocket({})
  },
  methods: {
    connect() {
      const token = uni.getStorageSync('token')
      if (!token) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        setTimeout(() => uni.navigateTo({ url: '/pages/login/index' }), 600)
        return
      }
      uni.connectSocket({ url: config.WS_URL + '?token=' + token, complete: () => {} })
      uni.onSocketOpen(() => { this.socketConnected = true })
      uni.onSocketMessage((res) => this.onMessage(res.data))
      uni.onSocketClose(() => { this.socketConnected = false })
      uni.onSocketError(() => { this.socketConnected = false })
    },
    send(text) {
      const content = (text || this.inputText || '').trim()
      if (!content || this.waiting) return
      if (!this.socketConnected) {
        uni.showToast({ title: '连接中，请稍候', icon: 'none' })
        return
      }
      this.pushMsg('user', content, 'text')
      this.inputText = ''
      this.waiting = true
      this.pushMsg('assistant', '', 'text')
      this.scrollBottom()
      uni.sendSocketMessage({ data: JSON.stringify({ type: 'message', content }) })
    },
    onMessage(data) {
      let msg
      try { msg = JSON.parse(data) } catch (e) { return }
      if (msg.type === 'chunk') {
        this.appendChunk(msg.content || '')
        this.scrollBottom()
      } else if (msg.type === 'done') {
        this.finish(msg.data || {})
      } else if (msg.type === 'error') {
        this.waiting = false
        this.replaceLastAssistant(msg.content || '服务出错了，请重试', 'text', null)
        this.scrollBottom()
      }
    },
    appendChunk(text) {
      const msgs = this.messages
      for (let i = msgs.length - 1; i >= 0; i--) {
        if (msgs[i].role === 'assistant') { msgs[i].content += text; break }
      }
    },
    finish(data) {
      this.waiting = false
      const d = data.data
      if (data.type === 'course' && d && d.course) {
        this.replaceLastAssistant(data.content || '', 'course', d)
      } else if (data.type === 'plan' && d) {
        this.replaceLastAssistant(data.content || '', 'plan', d)
      } else {
        this.replaceLastAssistant(data.content || '', data.type || 'text', null)
      }
      this.scrollBottom()
    },
    replaceLastAssistant(content, type, data) {
      const msgs = this.messages
      for (let i = msgs.length - 1; i >= 0; i--) {
        if (msgs[i].role === 'assistant') {
          msgs[i].content = content
          msgs[i].type = type
          msgs[i].data = data
          break
        }
      }
    },
    pushMsg(role, content, type) {
      this.messages.push({ role, content, type })
    },
    startTrain(courseId) {
      uni.navigateTo({ url: '/pages/course/train?id=' + courseId })
    },
    goPlan(planId) {
      uni.navigateTo({ url: '/pages/plan/detail?id=' + planId })
    },
    scrollBottom() {
      this.$nextTick(() => { this.scrollTo = 'msg-bottom' })
    },
    roleIcon(role) {
      return { '热身': '🔥', '主项': '💪', '辅助': '🏋️', '收尾': '🧘' }[role] || '•'
    }
  }
}
</script>

<style>
page {
  height: 100%;
  background: #f5f6f8;
}
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.msg-list {
  flex: 1;
  height: 0;
  padding: 24rpx 24rpx 0;
  box-sizing: border-box;
}
.empty-tip {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16rpx;
  padding: 80rpx 40rpx;
}
.empty-emoji { font-size: 96rpx; }
.empty-text {
  font-size: 26rpx;
  color: #9ca3af;
  text-align: center;
  line-height: 1.6;
  white-space: pre-line;
}
.msg-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 28rpx;
}
.msg-row.user { flex-direction: row-reverse; }
.avatar {
  width: 72rpx;
  height: 72rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 40rpx;
  flex-shrink: 0;
}
.avatar.ai { background: #ecfdf5; }
.avatar.user {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-size: 28rpx;
}
.bubble {
  max-width: 72%;
  margin: 0 16rpx;
  padding: 20rpx 24rpx;
  border-radius: 20rpx;
  font-size: 28rpx;
  line-height: 1.6;
}
.bubble.assistant {
  background: #fff;
  color: #1f2937;
  border-top-left-radius: 6rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.06);
}
.bubble.user {
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  border-top-right-radius: 6rpx;
}
.bubble-text { word-break: break-all; white-space: pre-wrap; }
.typing { color: #9ca3af; }
.course-card {
  background: #f8fafc;
  border: 1rpx solid #e5e8ef;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-top: 4rpx;
}
.cc-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.cc-name { font-size: 28rpx; font-weight: 700; color: #111827; }
.cc-badge { font-size: 20rpx; color: #10b981; background: #ecfdf5; padding: 4rpx 12rpx; border-radius: 999rpx; }
.cc-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 10rpx 0;
  border-bottom: 1rpx solid #f1f3f5;
}
.cc-item:last-child { border-bottom: none; }
.cc-role { width: 44rpx; text-align: center; font-size: 24rpx; flex-shrink: 0; }
.cc-aname { flex: 1; font-size: 26rpx; color: #374151; }
.cc-sets { font-size: 24rpx; color: #10b981; font-weight: 600; }
.cc-more { font-size: 22rpx; color: #9ca3af; padding: 8rpx 0; }
.cc-btn {
  margin-top: 16rpx;
  height: 64rpx;
  line-height: 64rpx;
  text-align: center;
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-size: 26rpx;
  border-radius: 999rpx;
}
.quick {
  padding: 0 24rpx 20rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  align-items: center;
}
.quick-title { font-size: 24rpx; color: #9ca3af; width: 100%; }
.quick-item {
  font-size: 26rpx;
  color: #059669;
  background: #ecfdf5;
  border: 1rpx solid #a7f3d0;
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
}
.input-bar {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 16rpx 24rpx 30rpx;
  background: #fff;
  box-shadow: 0 -4rpx 16rpx rgba(16, 185, 129, 0.06);
}
.input {
  flex: 1;
  height: 76rpx;
  background: #f5f6f8;
  border-radius: 999rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
}
.send-btn {
  flex-shrink: 0;
  width: 132rpx;
  height: 76rpx;
  line-height: 76rpx;
  background: linear-gradient(135deg, #34d399, #10b981);
  color: #fff;
  font-size: 28rpx;
  border-radius: 999rpx;
  padding: 0;
  margin: 0;
}
.send-btn[disabled] { opacity: 0.5; color: #fff; background: #cbd5d1; }
</style>
