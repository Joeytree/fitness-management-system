<template>
  <view class="course-page">
    <!-- 搜索框（钉死顶部） -->
    <view class="search-bar">
      <text class="search-ico">🔍</text>
      <input
        class="search-input"
        v-model="keyword"
        placeholder="搜索课程"
        confirm-type="search"
        @confirm="reload"
      />
    </view>

    <!-- 主体：左侧分类栏 + 右侧沉浸式卡片列表 -->
    <view class="body">
      <!-- 左侧竖向分类栏 -->
      <scroll-view scroll-y class="rail">
        <view
          v-for="c in cats"
          :key="c"
          class="rail-item"
          :class="{ active: category === c }"
          @click="selectCat(c)"
        >{{ c }}</view>
      </scroll-view>

      <!-- 右侧：筛选行 + 卡片列表 -->
      <view class="right">
        <!-- 筛选行（难度 / 部位 / 时长，内联下拉，靠右） -->
        <view class="filter-row">
          <view v-for="f in filterConfigs" :key="f.key" class="chip-wrap" @click="toggleDropdown(f.key)">
            <view class="filter-chip" :class="{ on: f.isActive, open: openFilter === f.key }">
              {{ f.label }}<text class="arrow">▾</text>
            </view>
            <view v-if="openFilter === f.key" class="dropdown" @click.stop>
              <view
                v-for="opt in f.options"
                :key="opt.label"
                class="dropdown-item"
                :class="{ selected: f.current === opt.value }"
                @click="selectFilter(f.key, opt.value)"
              >{{ opt.label }}</view>
            </view>
          </view>
          <!-- 点击下拉外部关闭：透明全屏遮罩（z-index 介于卡片与 chip 之间） -->
          <view v-if="openFilter" class="dropdown-mask" @click="openFilter = null" />
        </view>

        <!-- 卡片列表 -->
        <scroll-view scroll-y class="card-scroll">
          <view v-for="c in list" :key="c.id" class="course-card" @click="goDetail(c)">
            <view class="cover" :style="{ background: coverGrad(c.category) }">
              <image v-if="c.cover" class="cover-img" :src="fullUrl(c.cover)" mode="aspectFill" lazy-load />
              <view class="cover-shade" />
              <text v-if="!c.cover" class="cover-emoji">{{ coverEmoji(c.category) }}</text>
              <view class="cover-info">
                <view class="cover-title">{{ c.name }}</view>
                <view class="cover-meta">
                  <text>{{ diffText(c.difficulty) }}</text>
                  <text class="dot">·</text>
                  <text>{{ c.duration || 0 }} 分钟</text>
                  <text class="spacer" />
                  <text class="stat">👀 {{ c.viewCount || 0 }}</text>
                  <text class="stat">♥ {{ c.favoriteCount || 0 }}</text>
                </view>
              </view>
            </view>
          </view>
          <view v-if="!list.length" class="empty muted">暂无课程</view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { COURSE_CATS, PARTS, diffText } from '../../utils/index'
import { fullUrl } from '../../utils/config'

// 分类 → 渐变背景 + 占位 emoji（无封面图时兜底）
const CAT_STYLE = {
  '增肌': { grad: 'linear-gradient(135deg,#3b82f6,#1d4ed8)', emoji: '🏋️' },
  '减脂': { grad: 'linear-gradient(135deg,#f97316,#ea580c)', emoji: '🔥' },
  '塑形': { grad: 'linear-gradient(135deg,#a855f7,#7e22ce)', emoji: '✨' },
  '瑜伽': { grad: 'linear-gradient(135deg,#06b6d4,#0891b2)', emoji: '🧘' },
  'HIIT': { grad: 'linear-gradient(135deg,#ef4444,#dc2626)', emoji: '⚡' },
  '康复拉伸': { grad: 'linear-gradient(135deg,#10b981,#059669)', emoji: '🤸' }
}
const FALLBACK = { grad: 'linear-gradient(135deg,#34d399,#10b981)', emoji: '💪' }

// 时长筛选档位：index → {min,max}（min/max 缺省表示不限）
const DURATION_BUCKETS = [
  null,
  { max: 15 },
  { min: 15, max: 30 },
  { min: 30 }
]

export default {
  data() {
    return {
      cats: ['全部', ...COURSE_CATS],
      category: '全部',
      keyword: '',
      list: [],
      // 筛选状态
      difficulty: null,
      part: null,
      durationOptions: ['全部时长', '15分钟内', '15-30分钟', '30分钟以上'],
      durationIndex: 0,
      // 当前展开的下拉：'diff' / 'part' / 'dur' / null
      openFilter: null
    }
  },
  computed: {
    durationLabel() {
      return this.durationIndex ? this.durationOptions[this.durationIndex] : ''
    },
    durationRange() {
      return DURATION_BUCKETS[this.durationIndex] || null
    },
    // 三个筛选的配置（含选项 + 当前值），用于渲染下拉菜单
    filterConfigs() {
      return [
        {
          key: 'diff',
          label: this.difficulty ? this.diffText(this.difficulty) : '难度',
          isActive: !!this.difficulty,
          current: this.difficulty,
          options: [
            { label: '全部难度', value: null },
            { label: '入门', value: 1 },
            { label: '进阶', value: 2 },
            { label: '高级', value: 3 }
          ]
        },
        {
          key: 'part',
          label: this.part || '部位',
          isActive: !!this.part,
          current: this.part,
          options: [
            { label: '全部部位', value: null },
            ...PARTS.map(p => ({ label: p, value: p }))
          ]
        },
        {
          key: 'dur',
          label: this.durationIndex ? this.durationOptions[this.durationIndex] : '时长',
          isActive: !!this.durationIndex,
          current: this.durationIndex,
          options: [
            { label: '全部时长', value: 0 },
            { label: '15分钟内', value: 1 },
            { label: '15-30分钟', value: 2 },
            { label: '30分钟以上', value: 3 }
          ]
        }
      ]
    }
  },
  onShow() {
    if (this.$mp && this.getTabBar && this.getTabBar()) {
      this.getTabBar().init(1)
    }
    this.reload()
  },
  methods: {
    diffText,
    fullUrl,
    coverGrad(cat) {
      return (CAT_STYLE[cat] || FALLBACK).grad
    },
    coverEmoji(cat) {
      return (CAT_STYLE[cat] || FALLBACK).emoji
    },
    selectCat(c) {
      this.category = c
      this.reload()
    },
    toggleDropdown(key) {
      this.openFilter = this.openFilter === key ? null : key
    },
    selectFilter(key, value) {
      if (key === 'diff') this.difficulty = value
      else if (key === 'part') this.part = value
      else if (key === 'dur') this.durationIndex = value
      this.openFilter = null
      this.reload()
    },
    async reload() {
      const r = this.durationRange || {}
      try {
        const data = await get('/course', {
          category: this.category === '全部' ? undefined : this.category,
          part: this.part || undefined,
          difficulty: this.difficulty || undefined,
          minDuration: r.min,
          maxDuration: r.max,
          keyword: this.keyword || undefined,
          page: 1,
          pageSize: 100
        })
        this.list = data.list || []
      } catch (e) { /* 忽略 */ }
    },
    goDetail(c) {
      uni.navigateTo({ url: '/pages/course/detail?id=' + c.id })
    }
  }
}
</script>

<style>
page {
  height: 100%;
}
.course-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f8;
}
/* 搜索框：钉死顶部 */
.search-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: #fff;
  border-radius: 999rpx;
  margin: 16rpx 24rpx;
  padding: 16rpx 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(16, 185, 129, 0.05);
}
.search-ico {
  font-size: 28rpx;
  color: #9ca3af;
}
.search-input {
  flex: 1;
  font-size: 28rpx;
}
/* 主体 */
.body {
  flex: 1;
  display: flex;
  overflow: hidden;
}
/* 左侧竖向分类栏 */
.rail {
  width: 176rpx;
  height: 100%;
  flex-shrink: 0;
  background: #f5f6f8;
}
.rail-item {
  position: relative;
  padding: 32rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #6b7280;
}
.rail-item.active {
  background: #fff;
  color: #111827;
  font-weight: 700;
  border-radius: 24rpx 0 0 24rpx;
}
.rail-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 8rpx;
  height: 36rpx;
  border-radius: 0 8rpx 8rpx 0;
  background: #10b981;
}
/* 右侧区域 */
.right {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 24rpx 0 0 0;
  overflow: hidden;
}
/* 筛选行（靠右） */
.filter-row {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 20rpx 8rpx;
  position: relative;
  z-index: 50;
}
.chip-wrap {
  position: relative;
  z-index: 60;
}
.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  padding: 10rpx 22rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  font-size: 24rpx;
  color: #4b5563;
}
.filter-chip.on {
  background: #ecfdf5;
  color: #059669;
  font-weight: 600;
}
.arrow {
  font-size: 20rpx;
  color: #9ca3af;
  transition: transform .15s;
}
.filter-chip.on .arrow {
  color: #10b981;
}
.filter-chip.open .arrow {
  transform: rotate(180deg);
}
/* 内联下拉面板 */
.dropdown {
  position: absolute;
  top: calc(100% + 10rpx);
  right: 0;
  z-index: 70;
  min-width: 220rpx;
  background: #fff;
  border-radius: 16rpx;
  box-shadow: 0 10rpx 28rpx rgba(0, 0, 0, 0.12);
  padding: 8rpx 0;
}
.dropdown-item {
  padding: 18rpx 28rpx;
  font-size: 26rpx;
  color: #1f2937;
  white-space: nowrap;
}
.dropdown-item.selected {
  color: #10b981;
  font-weight: 600;
}
/* 全屏透明遮罩：点击下拉以外的区域关闭（z-index 低于 chip-wrap，不会拦截 chip 点击） */
.dropdown-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 40;
}
/* 卡片列表（底部留白给悬浮 tabBar） */
.card-scroll {
  flex: 1;
  height: 0;
  padding: 8rpx 20rpx 180rpx;
  box-sizing: border-box;
}
.course-card {
  border-radius: 24rpx;
  overflow: hidden;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.08);
  margin-bottom: 20rpx;
}
.cover {
  position: relative;
  height: 320rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.cover-img {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
}
.cover-shade {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.05) 40%, rgba(0, 0, 0, 0.6) 100%);
  z-index: 1;
}
.cover-emoji {
  position: relative;
  z-index: 1;
  font-size: 120rpx;
  filter: drop-shadow(0 8rpx 16rpx rgba(0, 0, 0, 0.3));
}
/* 沉浸式标题两层（浮于大图底部） */
.cover-info {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 2;
  padding: 0 24rpx 20rpx;
  color: #fff;
}
.cover-title {
  font-size: 34rpx;
  font-weight: 700;
  line-height: 1.35;
  text-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.4);
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.cover-meta {
  margin-top: 12rpx;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8rpx;
  font-size: 24rpx;
  opacity: 0.95;
  text-shadow: 0 1rpx 4rpx rgba(0, 0, 0, 0.4);
}
.cover-meta .dot {
  opacity: 0.7;
}
.spacer {
  flex: 1;
}
.stat {
  display: inline-flex;
  align-items: center;
  gap: 4rpx;
}
.empty {
  text-align: center;
  padding: 80rpx 0;
  font-size: 26rpx;
}
</style>
