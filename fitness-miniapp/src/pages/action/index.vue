<template>
  <view class="lib-page">
    <!-- 顶部搜索框（钉死不动） -->
    <view class="search-bar">
      <input
        class="search-input"
        v-model="keyword"
        placeholder="搜索动作"
        confirm-type="search"
        @confirm="doSearch"
      />
    </view>

    <view class="lib-body">
      <!-- 左侧：垂直部位筛选 -->
      <scroll-view scroll-y class="side">
        <view
          v-for="p in parts"
          :key="p"
          class="side-item"
          :class="{ active: part === p }"
          @click="selectPart(p)"
        >{{ p }}</view>
      </scroll-view>

      <!-- 右侧：难度/器械筛选 + 两列动作网格 -->
      <scroll-view scroll-y class="main" @scrolltolower="loadMore">
        <view class="filter-row">
          <scroll-view scroll-x class="filter-scroll">
            <view
              v-for="d in diffs"
              :key="d.label"
              class="chip"
              :class="{ on: difficulty === d.value }"
              @click="selectDifficulty(d.value)"
            >{{ d.label }}</view>
          </scroll-view>
        </view>
        <view class="filter-row">
          <scroll-view scroll-x class="filter-scroll">
            <view
              v-for="e in equipments"
              :key="e"
              class="chip"
              :class="{ on: equipment === e }"
              @click="selectEquipment(e)"
            >{{ e || '全部' }}</view>
          </scroll-view>
        </view>

        <view class="cols">
          <view class="col">
            <view v-for="a in colA" :key="a.id" class="cell" @click="goDetail(a)">
              <view class="cell-img">
                <image
                  v-if="a.mediaUrl && a.mediaType !== 2"
                  class="img"
                  :src="fullUrl(a.mediaUrl)"
                  mode="widthFix"
                  lazy-load
                />
                <view v-else class="img-holder"><text class="holder-ico">💪</text></view>
              </view>
              <view class="cell-name">{{ a.name }}</view>
            </view>
          </view>
          <view class="col">
            <view v-for="a in colB" :key="a.id" class="cell" @click="goDetail(a)">
              <view class="cell-img">
                <image
                  v-if="a.mediaUrl && a.mediaType !== 2"
                  class="img"
                  :src="fullUrl(a.mediaUrl)"
                  mode="widthFix"
                  lazy-load
                />
                <view v-else class="img-holder"><text class="holder-ico">💪</text></view>
              </view>
              <view class="cell-name">{{ a.name }}</view>
            </view>
          </view>
        </view>
        <view v-if="!list.length && !loading" class="empty muted">没有符合条件的动作</view>
        <view v-if="loading" class="empty muted">加载中…</view>
        <view v-else-if="finished && list.length" class="empty muted">没有更多了</view>
      </scroll-view>
    </view>
  </view>
</template>

<script>
import { get } from '../../utils/request'
import { PARTS, diffText } from '../../utils/index'
import { fullUrl } from '../../utils/config'

const EQUIPMENTS = ['无', '哑铃', '杠铃', '绳索', '固定器械', '弹力带', '史密斯机', '壶铃', '负重', '瑞士球', '拉伸', '药球', '泡沫轴', '有氧器械', '健腹轮']
const PAGE_SIZE = 30

export default {
  data() {
    return {
      parts: ['全部', ...PARTS],
      part: '全部',
      diffs: [
        { value: null, label: '难度' },
        { value: 1, label: '入门' },
        { value: 2, label: '进阶' },
        { value: 3, label: '高级' }
      ],
      difficulty: null,
      equipments: [''].concat(EQUIPMENTS),
      equipment: '',
      keyword: '',
      list: [],
      page: 1,
      loading: false,
      finished: false
    }
  },
  computed: {
    // 手动分列：奇偶交替放进左右两列（scroll-view 内 flex-wrap 子项宽度不可靠，分列后各列 flex:1 恒为半宽）
    colA() {
      return this.list.filter((_, i) => i % 2 === 0)
    },
    colB() {
      return this.list.filter((_, i) => i % 2 === 1)
    }
  },
  onShow() {
    this.reload()
  },
  methods: {
    diffText,
    fullUrl,
    selectPart(p) {
      this.part = p
      this.reload()
    },
    selectDifficulty(v) {
      this.difficulty = v
      this.reload()
    },
    selectEquipment(e) {
      this.equipment = e
      this.reload()
    },
    doSearch() {
      this.reload()
    },
    reset() {
      this.page = 1
      this.finished = false
      this.list = []
    },
    async reload() {
      this.reset()
      await this.load()
    },
    async load() {
      if (this.loading || this.finished) return
      this.loading = true
      try {
        const data = await get('/action', {
          part: this.part === '全部' ? undefined : this.part,
          difficulty: this.difficulty || undefined,
          equipment: this.equipment || undefined,
          keyword: this.keyword || undefined,
          page: this.page,
          pageSize: PAGE_SIZE
        })
        const rows = data.list || []
        this.list = this.list.concat(rows)
        if (rows.length < PAGE_SIZE) {
          this.finished = true
        } else {
          this.page += 1
        }
      } catch (e) { /* 忽略 */ }
      this.loading = false
    },
    loadMore() {
      this.load()
    },
    goDetail(a) {
      uni.navigateTo({ url: '/pages/action/detail?id=' + a.id })
    }
  }
}
</script>

<style>
page {
  height: 100%;
}
.lib-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f8;
}
/* 搜索框：钉死在顶部 */
.search-bar {
  flex-shrink: 0;
  padding: 16rpx 24rpx;
  background: #f5f6f8;
}
.search-input {
  background: #fff;
  border-radius: 999rpx;
  padding: 16rpx 32rpx;
  font-size: 28rpx;
}
.lib-body {
  flex: 1;
  display: flex;
  overflow: hidden;
}
/* 左侧部位竖栏 */
.side {
  width: 150rpx;
  height: 100%;
  flex-shrink: 0;
  background: #f5f6f8;
}
.side-item {
  position: relative;
  padding: 30rpx 0;
  text-align: center;
  font-size: 26rpx;
  color: #6b7280;
}
.side-item.active {
  background: #fff;
  color: #111827;
  font-weight: 700;
  border-radius: 24rpx 0 0 24rpx;
}
.side-item.active::before {
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
/* 右侧内容区 */
.main {
  flex: 1;
  height: 100%;
  background: #fff;
  border-radius: 24rpx 0 0 0;
  padding-bottom: 40rpx;
  box-sizing: border-box;
}
.filter-row {
  padding: 16rpx 20rpx 0;
}
.filter-scroll {
  white-space: nowrap;
}
.chip {
  display: inline-block;
  padding: 10rpx 30rpx;
  margin-right: 14rpx;
  border-radius: 999rpx;
  background: #f3f4f6;
  font-size: 24rpx;
  color: #4b5563;
}
.chip.on {
  background: #10b981;
  color: #fff;
  font-weight: 600;
}
/* 双列：列宽用固定 rpx 锁死（彻底规避 scroll-view 内 flex:1 / % 被图片回退撑开的陷阱）。
   内容区 750 - 150(左栏) = 600rpx，两列 270 + 间隙 16 + 边距 40 = 596 ≤ 600。 */
.cols {
  display: flex;
  align-items: flex-start;
  padding: 16rpx 20rpx 0;
  box-sizing: border-box;
}
.col {
  width: 270rpx;
  min-width: 270rpx;
  max-width: 270rpx;
  display: flex;
  flex-direction: column;
}
.col + .col {
  margin-left: 16rpx;
}
.cell {
  width: 100%;
  background: #fff;
  border: 2rpx solid #f3f4f6;
  border-radius: 16rpx;
  overflow: hidden;
  box-sizing: border-box;
  margin-bottom: 16rpx;
}
.cell-img {
  width: 100%;
  background: #fff;
  overflow: hidden;
}
.cell-img .img {
  display: block;
  width: 100%;
}
.img-holder {
  width: 100%;
  height: 240rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
}
.holder-ico {
  font-size: 64rpx;
}
.cell-name {
  padding: 14rpx 12rpx 18rpx;
  font-size: 26rpx;
  font-weight: 600;
  text-align: center;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}
.empty {
  text-align: center;
  padding: 80rpx 0;
  font-size: 26rpx;
}
</style>