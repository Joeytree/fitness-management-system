<template>
  <div>
    <div class="stat-cards">
      <div class="stat-card">
        <div class="ico" style="background:linear-gradient(135deg,#4d8bff,#2f6bff)">👥</div>
        <div>
          <div class="num">{{ data.userCount ?? 0 }}</div>
          <div class="lbl">平台注册用户</div>
          <div class="trend" style="color:#12b886">累计注册用户总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="ico" style="background:linear-gradient(135deg,#33d69f,#12b886)">⚡</div>
        <div>
          <div class="num">{{ data.todayCheckIn ?? 0 }}</div>
          <div class="lbl">今日训练打卡</div>
          <div class="trend" style="color:#12b886">今日有效打卡次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="ico" style="background:linear-gradient(135deg,#ffa94d,#f76707)">🏋️</div>
        <div>
          <div class="num">{{ data.courseCount ?? 0 }}</div>
          <div class="lbl">在架课程</div>
          <div class="trend" style="color:#f76707">◆ 待审核 {{ data.pendingCount ?? 0 }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="ico" style="background:linear-gradient(135deg,#9775fa,#7950f2)">📅</div>
        <div>
          <div class="num">{{ data.checkInRate ?? 0 }}%</div>
          <div class="lbl">今日训练打卡率</div>
          <div class="trend" style="color:#12b886">打卡 / 注册用户</div>
        </div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="chart-box">
        <div class="chart-title">近 12 个月注册用户增长</div>
        <div ref="growChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">课程热度 Top6（按浏览量）</div>
        <div ref="heatChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">课程分类占比</div>
        <div ref="shareChart" class="chart"></div>
      </div>
      <div class="chart-box">
        <div class="chart-title">近 12 个月打卡趋势</div>
        <div ref="checkinChart" class="chart"></div>
      </div>
    </div>

    <div style="margin-top:14px" class="page-card">
      <div class="chart-title" style="margin-bottom:10px">平台运营概览</div>
      <el-table :data="overview" border style="width:100%">
        <el-table-column prop="item" label="指标" width="220" />
        <el-table-column prop="value" label="数值" width="200" />
        <el-table-column prop="note" label="说明" />
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import request from '../api/request'

const data = ref({})
const charts = {}
const growChart = ref(null)
const heatChart = ref(null)
const shareChart = ref(null)
const checkinChart = ref(null)

const overview = ref([])

const COLORS = ['#2f6bff', '#12b886', '#f76707', '#7950f2', '#e64980', '#1098ad', '#f59f00']

function monthLabels(n = 12) {
  const now = new Date()
  const labels = []
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    labels.push((d.getMonth() + 1) + '月')
  }
  return labels
}

async function load() {
  data.value = await request.get('/admin/dashboard')
  overview.value = [
    { item: '平台注册用户', value: (data.value.userCount ?? 0) + ' 人', note: '累计注册用户总数' },
    { item: '在架课程', value: (data.value.courseCount ?? 0) + ' 门', note: '已发布状态课程（移动端可见）' },
    { item: '今日训练打卡', value: (data.value.todayCheckIn ?? 0) + ' 次', note: '今日有效打卡记录' },
    { item: '今日打卡率', value: (data.value.checkInRate ?? 0) + '%', note: '打卡人数 / 注册用户数' },
    { item: '待审核内容', value: (data.value.pendingCount ?? 0) + ' 条', note: '动态 / 评论 / UGC 动作与课程' }
  ]
  await nextTick()
  renderAll()
}

function renderAll() {
  renderGrow()
  renderHeat()
  renderShare()
  renderCheckin()
}

function renderGrow() {
  if (!growChart.value) return
  if (!charts.grow) charts.grow = echarts.init(growChart.value)
  const months = monthLabels(12)
  const growth = data.value.userGrowth || []
  charts.grow.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 10, right: 10, top: 32, bottom: 0, containLabel: true },
    xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#e5e8ef' } }, axisLabel: { color: '#909399' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f2f7' } }, axisLabel: { color: '#909399' } },
    series: [{
      name: '注册用户', type: 'line', smooth: true, data: growth, symbolSize: 5,
      lineStyle: { width: 3, color: '#2f6bff' }, itemStyle: { color: '#2f6bff' }, areaStyle: { opacity: .08, color: '#2f6bff' }
    }]
  })
}

function renderHeat() {
  if (!heatChart.value) return
  if (!charts.heat) charts.heat = echarts.init(heatChart.value)
  const rows = (data.value.hotCourses || []).slice(0, 6).reverse()
  charts.heat.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 10, right: 40, top: 10, bottom: 0, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f2f7' } }, axisLabel: { color: '#909399' } },
    yAxis: { type: 'category', data: rows.map(r => r.name), axisLine: { show: false }, axisLabel: { color: '#4a4f58' } },
    series: [{
      type: 'bar', data: rows.map(r => r.viewCount), barWidth: 14,
      itemStyle: { borderRadius: [0, 7, 7, 0], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#4d8bff' }, { offset: 1, color: '#2f6bff' }]) },
      label: { show: true, position: 'right', color: '#909399', fontSize: 11 }
    }]
  })
}

function renderShare() {
  if (!shareChart.value) return
  if (!charts.share) charts.share = echarts.init(shareChart.value)
  const rows = data.value.categoryShare || []
  charts.share.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} 门 ({d}%)' },
    legend: { bottom: 0, itemWidth: 12, itemHeight: 12, textStyle: { color: '#606266', fontSize: 12 } },
    color: COLORS,
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '45%'],
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: rows.map(r => ({ name: r.name, value: r.value }))
    }]
  })
}

function renderCheckin() {
  if (!checkinChart.value) return
  if (!charts.checkin) charts.checkin = echarts.init(checkinChart.value)
  const months = monthLabels(12)
  charts.checkin.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}<br>打卡次数：{c}' },
    grid: { left: 10, right: 10, top: 24, bottom: 0, containLabel: true },
    xAxis: { type: 'category', data: months, axisLine: { lineStyle: { color: '#e5e8ef' } }, axisLabel: { color: '#909399' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f2f7' } }, axisLabel: { color: '#909399' } },
    series: [{
      name: '打卡次数', type: 'line', smooth: true, data: data.value.checkinTrend || [],
      lineStyle: { width: 3, color: '#7950f2' }, itemStyle: { color: '#7950f2' }, symbolSize: 5,
      areaStyle: { opacity: .1, color: '#7950f2' }
    }]
  })
}

function onResize() {
  Object.values(charts).forEach(c => c && c.resize())
}

onMounted(load)
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  Object.values(charts).forEach(c => c && c.dispose())
})
window.addEventListener('resize', onResize)
</script>
