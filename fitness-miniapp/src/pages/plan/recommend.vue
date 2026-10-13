<template>
  <view class="page-pad">
    <!-- 筛选行：目标 / 水平 / 训练频率（内联下拉） -->
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

    <!-- 卡片列表：封面图 → 名称 → 标签行（目标 / 水平 / 每周几练） -->
    <view class="list">
      <view v-for="p in filtered" :key="p.id" class="plan-card" @click="goDetail(p)">
        <view class="p-cover">
          <image v-if="p.cover" class="p-cover-img" :src="fullUrl(p.cover)" mode="aspectFill" lazy-load />
          <text v-else class="p-cover-ico">🎯</text>
        </view>
        <view class="p-name">{{ p.name }}</view>
        <view class="p-tags">
          <text class="pill">{{ goalText(p.goal) }}</text>
          <text class="pill">{{ levelText(p.level) }}</text>
          <text class="muted">{{ p.cycle || '' }}</text>
        </view>
      </view>
      <view v-if="loaded && !filtered.length" class="empty muted">暂无符合条件的计划</view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { goalText, levelText } from '../../utils/index'
import { fullUrl } from '../../utils/config'

export default {
  data() {
    return {
      templates: [],
      loaded: false,
      // 筛选状态：null 表示不限
      goal: null,
      level: null,
      cycle: null,
      openFilter: null
    }
  },
  computed: {
    // 模板量小（当前 8 条），一次拉全后本地过滤，切换筛选即时响应
    filtered() {
      return this.templates.filter(p =>
        (!this.goal || p.goal === this.goal) &&
        (!this.level || p.level === this.level) &&
        (!this.cycle || p.cycle === this.cycle)
      )
    },
    // 频率选项从数据动态提取去重，避免写死
    cycleOptions() {
      const set = []
      this.templates.forEach(p => {
        if (p.cycle && !set.includes(p.cycle)) set.push(p.cycle)
      })
      return set.sort()
    },
    filterConfigs() {
      return [
        {
          key: 'goal',
          label: this.goal ? this.goalText(this.goal) : '目标',
          isActive: !!this.goal,
          current: this.goal,
          options: [
            { label: '全部目标', value: null },
            { label: '增肌', value: 1 },
            { label: '减脂', value: 2 },
            { label: '塑形', value: 3 },
            { label: '保持', value: 4 }
          ]
        },
        {
          key: 'level',
          label: this.level ? this.levelText(this.level) : '水平',
          isActive: !!this.level,
          current: this.level,
          options: [
            { label: '全部水平', value: null },
            { label: '新手', value: 1 },
            { label: '初级', value: 2 },
            { label: '进阶', value: 3 }
          ]
        },
        {
          key: 'cycle',
          label: this.cycle || '训练频率',
          isActive: !!this.cycle,
          current: this.cycle,
          options: [
            { label: '全部频率', value: null },
            ...this.cycleOptions.map(c => ({ label: c, value: c }))
          ]
        }
      ]
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    goalText,
    levelText,
    fullUrl,
    async load() {
      try {
        this.templates = await get('/plan/templates') || []
      } catch (e) { /* 忽略 */ }
      this.loaded = true
    },
    toggleDropdown(key) {
      this.openFilter = this.openFilter === key ? null : key
    },
    selectFilter(key, value) {
      if (key === 'goal') this.goal = value
      else if (key === 'level') this.level = value
      else if (key === 'cycle') this.cycle = value
      this.openFilter = null
    },
    goDetail(p) {
      uni.navigateTo({ url: '/pages/plan/detail?id=' + p.id })
    }
  }
}
</script>

<style>
.filter-row {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 20rpx;
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
.plan-card {
  background: #fff;
  border-radius: 28rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 6rpx 20rpx rgba(16, 185, 129, 0.06);
}
.p-cover {
  height: 320rpx;
  border-radius: 16rpx;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #a7f3d0, #34d399);
}
.p-cover-img {
  width: 100%;
  height: 100%;
}
.p-cover-ico {
  font-size: 100rpx;
}
.p-name {
  font-size: 32rpx;
  font-weight: 700;
  margin-top: 20rpx;
}
.p-tags {
  margin-top: 16rpx;
  display: flex;
  align-items: center;
  gap: 14rpx;
  flex-wrap: wrap;
}
.empty {
  text-align: center;
  padding: 100rpx 0;
}
</style>
