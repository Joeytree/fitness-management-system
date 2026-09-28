<template>
  <view class="page-pad">
    <!-- 基础信息 -->
    <view class="card">
      <view class="form-row">
        <text class="label">课程名称 *</text>
        <input class="input" v-model="form.name" placeholder="如：居家零器械全身训练" />
      </view>
      <view class="form-row">
        <text class="label">分类</text>
        <picker :range="cats" @change="onCat">
          <view class="picker-value">{{ form.category || '塑形' }} ▾</view>
        </picker>
      </view>
      <view class="form-row">
        <text class="label">时长(分钟)</text>
        <input class="input" v-model="form.duration" type="number" placeholder="如：30" />
      </view>
      <view class="form-row col">
        <text class="label">课程简介</text>
        <textarea class="textarea" v-model="form.intro" placeholder="介绍课程内容与适合人群" />
      </view>
    </view>

    <!-- 封面 -->
    <view class="card">
      <view class="form-row col">
        <text class="label">课程封面（可选）</text>
        <view v-if="form.cover" class="cover-preview" @click="removeCover">
          <image class="cover-img" :src="fullUrl(form.cover)" mode="widthFix" />
          <view class="cover-del">点击移除</view>
        </view>
        <view v-else class="cover-btn" @click="chooseCover">🖼 上传封面</view>
      </view>
    </view>

    <!-- 动作编排 -->
    <view class="card">
      <view class="arrange-head">
        <text class="arrange-title">动作编排</text>
        <text class="add-btn" @click="openPicker">＋ 添加动作</text>
      </view>
      <view v-if="!actionItems.length" class="arrange-empty">尚未添加动作，点击上方按钮从动作库选择</view>
      <view v-for="(item, idx) in actionItems" :key="item.actionId" class="arrange-item">
        <view class="arrange-top">
          <view class="arrange-info">
            <text class="arrange-name">{{ item.name }}</text>
            <text v-if="item.status === 3" class="mini-tag tag-3">已驳回</text>
            <text v-else-if="item.status === 4" class="mini-tag tag-4">仅自用</text>
            <text v-else-if="item.status === 1" class="mini-tag tag-1">待审核</text>
          </view>
          <view class="arrange-ops">
            <text class="op" @click="moveItem(idx, -1)">↑</text>
            <text class="op" @click="moveItem(idx, 1)">↓</text>
            <text class="op del" @click="removeItem(idx)">✕</text>
          </view>
        </view>
        <view class="arrange-params">
          <view class="param">
            <text class="param-label">组数</text>
            <input class="param-input" v-model="item.sets" type="number" placeholder="3" />
          </view>
          <view class="param">
            <text class="param-label">次数</text>
            <input class="param-input" v-model="item.reps" type="number" placeholder="12" />
          </view>
          <view class="param">
            <text class="param-label">重量kg</text>
            <input class="param-input" v-model="item.weight" type="number" placeholder="徒手" />
          </view>
        </view>
      </view>
    </view>

    <!-- 可见性二选一 -->
    <view class="vis-card">
      <text class="vis-tip">提交方式（决定内容的可见范围）</text>
      <view class="vis-row">
        <view class="vis-btn ghost" @click="submit('private')">
          <text class="vis-title">仅自用</text>
          <text class="vis-sub">仅自己可见，永不公开</text>
        </view>
        <view class="vis-btn primary" @click="submit('public')">
          <text class="vis-title">申请公开</text>
          <text class="vis-sub">提交审核，通过后全站可见</text>
        </view>
      </view>
    </view>

    <!-- 动作选择器弹层 -->
    <view v-if="pickerVisible" class="picker-mask" @click="closePicker">
      <view class="picker-sheet" @click.stop>
        <view class="picker-head">
          <input
            class="picker-search"
            :value="keyword"
            placeholder="搜索动作名称（服务端实时搜索）"
            confirm-type="search"
            @input="onSearchInput"
            @confirm="onSearchConfirm"
          />
          <text class="picker-close" @click="closePicker">关闭</text>
        </view>
        <scroll-view
          scroll-y
          class="picker-list"
          :lower-threshold="80"
          @scrolltolower="loadMore"
        >
          <view v-for="a in pickerActions" :key="a.id" class="picker-item" @click="pick(a)">
            <view class="picker-info">
              <text class="picker-name">{{ a.name }}</text>
              <text class="picker-sub">{{ a.part || '' }} · {{ a.equipment || '徒手' }}</text>
            </view>
            <text v-if="a.status === 3" class="mini-tag tag-3">已驳回</text>
            <text v-else-if="a.status === 4" class="mini-tag tag-4">仅自用</text>
            <text v-else-if="a.status === 1" class="mini-tag tag-1">待审核</text>
            <text v-else class="mini-tag tag-2">已公开</text>
          </view>
          <view v-if="pickerLoading" class="arrange-empty">加载中…</view>
          <view v-else-if="!pickerActions.length" class="arrange-empty">无匹配动作</view>
          <view v-else-if="!pickerHasMore" class="arrange-empty">已加载全部</view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { get, post, put } from '../../utils/request'
import { COURSE_CATS } from '../../utils/index'
import { fullUrl } from '../../utils/config'
import { uploadFile } from '../../utils/upload'

export default {
  data() {
    return {
      cats: COURSE_CATS,
      id: null,
      form: { name: '', category: '塑形', duration: 30, intro: '', cover: '' },
      actionItems: [],
      // 选择器（服务端实时搜索 + 分页加载）
      pickerVisible: false,
      pickerActions: [],
      pickerLoading: false,
      pickerHasMore: false,
      pickerPage: 1,
      pickerPageSize: 30,
      keyword: '',
      searchTimer: null
    }
  },
  onLoad(options) {
    this.id = options.id
    if (this.id) {
      this.load()
    }
  },
  onUnload() {
    if (this.searchTimer) {
      clearTimeout(this.searchTimer)
      this.searchTimer = null
    }
  },
  methods: {
    fullUrl,
    onCat(e) {
      this.form.category = this.cats[Number(e.detail.value)]
    },
    async load() {
      try {
        const d = await get('/course/' + this.id)
        const c = d && d.course ? d.course : d
        if (c) {
          this.form = {
            name: c.name || '',
            category: c.category || '塑形',
            duration: c.duration || 30,
            intro: c.intro || '',
            cover: c.cover || ''
          }
        }
        // 已有编排
        const acts = (d && d.actions) || []
        this.actionItems = acts.map(a => ({
          actionId: a.id,
          name: a.name,
          part: a.part,
          status: 2,
          sets: a.sets,
          reps: a.reps,
          weight: a.weight
        }))
      } catch (e) { /* 已提示 */ }
    },
    async chooseCover() {
      uni.chooseImage({
        count: 1,
        success: async (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0]
          if (!path) return
          try {
            this.form.cover = await uploadFile(path)
          } catch (e) { /* 已提示 */ }
        }
      })
    },
    removeCover() {
      this.form.cover = ''
    },
    async openPicker() {
      this.keyword = ''
      this.pickerVisible = true
      this.pickerPage = 1
      this.pickerActions = []
      this.pickerHasMore = false
      await this.loadActions(true)
    },
    closePicker() {
      this.pickerVisible = false
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
    },
    /** 输入即搜（300ms 防抖，走后端 keyword） */
    onSearchInput(e) {
      this.keyword = e.detail.value
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => {
        this.searchTimer = null
        this.loadActions(true)
      }, 300)
    },
    /** 键盘「搜索」立即触发，不等防抖 */
    onSearchConfirm() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
      this.loadActions(true)
    },
    /** 拉取动作（reset=true 重置列表，false 追加下一页） */
    async loadActions(reset) {
      if (this.pickerLoading) return
      if (!reset && !this.pickerHasMore) return
      this.pickerLoading = true
      const page = reset ? 1 : this.pickerPage
      try {
        const params = { includeMine: true, page, pageSize: this.pickerPageSize }
        const k = this.keyword.trim()
        if (k) params.keyword = k
        const r = await get('/action', params)
        const list = (r && r.list) || []
        const total = (r && r.total) || 0
        this.pickerActions = reset ? list : this.pickerActions.concat(list)
        this.pickerPage = page
        this.pickerHasMore = this.pickerActions.length < total
      } catch (e) {
        if (reset) {
          this.pickerActions = []
        }
        this.pickerHasMore = false
      } finally {
        this.pickerLoading = false
      }
    },
    /** 触底加载下一页 */
    loadMore() {
      if (this.pickerLoading || !this.pickerHasMore) return
      this.pickerPage += 1
      this.loadActions(false)
    },
    pick(a) {
      if (this.actionItems.some(x => x.actionId === a.id)) {
        uni.showToast({ title: '该动作已添加', icon: 'none' })
        return
      }
      this.actionItems.push({
        actionId: a.id,
        name: a.name,
        part: a.part,
        status: a.status,
        sets: 3,
        reps: 12,
        weight: ''
      })
    },
    removeItem(idx) {
      this.actionItems.splice(idx, 1)
    },
    moveItem(idx, dir) {
      const to = idx + dir
      if (to < 0 || to >= this.actionItems.length) return
      const arr = this.actionItems
      const tmp = arr[idx]
      arr[idx] = arr[to]
      arr[to] = tmp
      this.actionItems = arr.slice()
    },
    async submit(visibility) {
      if (!this.form.name) {
        uni.showToast({ title: '课程名称为必填', icon: 'none' })
        return
      }
      // 公开课约束校验（前端兜底，后端也会校验）
      if (visibility === 'public') {
        const bad = this.actionItems.find(a => a.status === 3 || a.status === 4)
        if (bad) {
          uni.showModal({
            title: '无法申请公开',
            content: '动作「' + bad.name + '」为仅自用/已驳回状态，申请公开的课程不能包含它。请移除该动作，或改用「仅自用」提交。',
            showCancel: false
          })
          return
        }
      }
      const actionItems = this.actionItems.map((a, i) => ({
        actionId: a.actionId,
        sets: a.sets ? Number(a.sets) : 3,
        reps: a.reps ? Number(a.reps) : 12,
        weight: a.weight !== '' && a.weight != null ? Number(a.weight) : null
      }))
      const payload = {
        name: this.form.name,
        category: this.form.category,
        intro: this.form.intro,
        cover: this.form.cover,
        duration: Number(this.form.duration) || 30,
        difficulty: 1,
        calorie: 150,
        visibility,
        actionItems
      }
      try {
        if (this.id) {
          await put('/course/ugc/' + this.id, payload)
        } else {
          await post('/course/ugc', payload)
        }
        const tip = visibility === 'private' ? '已保存为仅自用' : '已提交，等待审核'
        uni.showToast({ title: tip, icon: 'success' })
        setTimeout(() => uni.navigateBack(), 800)
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
  margin-bottom: 24rpx;
}
.form-row {
  display: flex;
  align-items: center;
  padding: 26rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}
.form-row.col {
  flex-direction: column;
  align-items: flex-start;
  gap: 16rpx;
}
.label {
  width: 180rpx;
  font-size: 28rpx;
  color: #6b7280;
  flex: none;
}
.input {
  flex: 1;
  font-size: 28rpx;
}
.picker-value {
  font-size: 28rpx;
  color: #1f2937;
}
.textarea {
  width: 100%;
  min-height: 140rpx;
  font-size: 28rpx;
}
.cover-preview {
  position: relative;
  width: 100%;
}
.cover-img {
  width: 100%;
  border-radius: 16rpx;
}
.cover-del {
  position: absolute;
  right: 16rpx;
  bottom: 16rpx;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
}
.cover-btn {
  width: 100%;
  text-align: center;
  padding: 40rpx 0;
  border: 2rpx dashed #d1d5db;
  border-radius: 16rpx;
  font-size: 28rpx;
  color: #6b7280;
}
.arrange-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
}
.arrange-title {
  font-size: 28rpx;
  color: #6b7280;
}
.add-btn {
  font-size: 28rpx;
  color: #10b981;
}
.arrange-empty {
  padding: 40rpx 0;
  text-align: center;
  color: #9ca3af;
  font-size: 26rpx;
}
.arrange-item {
  padding: 20rpx 0;
  border-top: 1rpx solid #f3f4f6;
}
.arrange-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.arrange-info {
  display: flex;
  align-items: center;
  gap: 12rpx;
  flex: 1;
}
.arrange-name {
  font-size: 30rpx;
  font-weight: 600;
  color: #1f2937;
}
.arrange-ops {
  display: flex;
  gap: 24rpx;
}
.op {
  font-size: 30rpx;
  color: #10b981;
}
.op.del {
  color: #ef4444;
}
.arrange-params {
  display: flex;
  gap: 20rpx;
  margin-top: 16rpx;
}
.param {
  flex: 1;
  display: flex;
  align-items: center;
  background: #f9fafb;
  border-radius: 12rpx;
  padding: 12rpx 16rpx;
}
.param-label {
  font-size: 24rpx;
  color: #9ca3af;
  margin-right: 8rpx;
}
.param-input {
  flex: 1;
  font-size: 26rpx;
}
.mini-tag {
  font-size: 20rpx;
  padding: 2rpx 12rpx;
  border-radius: 999rpx;
}
.tag-1 { background: #fff7e6; color: #d97706; }
.tag-2 { background: #ecfdf5; color: #059669; }
.tag-3 { background: #fef2f2; color: #dc2626; }
.tag-4 { background: #f3f4f6; color: #6b7280; }
.vis-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;
}
.vis-tip {
  font-size: 26rpx;
  color: #6b7280;
  display: block;
  margin-bottom: 20rpx;
}
.vis-row {
  display: flex;
  gap: 20rpx;
}
.vis-btn {
  flex: 1;
  border-radius: 20rpx;
  padding: 28rpx 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.vis-btn.ghost {
  background: #f3f4f6;
}
.vis-btn.primary {
  background: linear-gradient(135deg, #34d399, #10b981);
}
.vis-title {
  font-size: 30rpx;
  font-weight: 600;
}
.vis-btn.ghost .vis-title { color: #4b5563; }
.vis-btn.primary .vis-title { color: #fff; }
.vis-sub { font-size: 22rpx; }
.vis-btn.ghost .vis-sub { color: #9ca3af; }
.vis-btn.primary .vis-sub { color: rgba(255, 255, 255, 0.9); }

/* 选择器弹层 */
.picker-mask {
  position: fixed;
  left: 0;
  top: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 90;
}
.picker-sheet {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  border-radius: 32rpx 32rpx 0 0;
  padding: 28rpx;
  max-height: 70vh;
  display: flex;
  flex-direction: column;
}
.picker-head {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 20rpx;
}
.picker-search {
  flex: 1;
  background: #f3f4f6;
  border-radius: 999rpx;
  padding: 14rpx 24rpx;
  font-size: 28rpx;
}
.picker-close {
  font-size: 28rpx;
  color: #6b7280;
}
.picker-list {
  height: 56vh;
}
.picker-item {
  display: flex;
  align-items: center;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f3f4f6;
}
.picker-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.picker-name {
  font-size: 30rpx;
  color: #1f2937;
}
.picker-sub {
  font-size: 24rpx;
  color: #9ca3af;
  margin-top: 4rpx;
}
</style>
