<template>
  <div class="dashboard-page">
    <!-- 顶部栏 -->
    <header class="dashboard-header">
      <button class="back-btn" @click="router.back()" aria-label="返回">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="9,18 15,12 9,6"/>
        </svg>
      </button>
      <h1 class="header-title">经营看板</h1>
      <button class="refresh-btn" @click="loadStats" aria-label="刷新">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="23,4 23,10 17,10"/>
          <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
        </svg>
      </button>
    </header>

    <!-- KPI 统计卡 -->
    <section class="kpi-grid" v-if="stats">
      <el-card shadow="never" class="kpi-card">
        <span class="kpi-label">累计发布</span>
        <span class="kpi-value">{{ stats.summary.published }}</span>
        <span class="kpi-trend faint">在售 {{ stats.summary.onSale }}</span>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <span class="kpi-label">已售出</span>
        <span class="kpi-value">{{ stats.summary.sold }}</span>
        <span class="kpi-trend faint">件商品</span>
      </el-card>
      <el-card shadow="never" class="kpi-card">
        <span class="kpi-label">累计浏览</span>
        <span class="kpi-value">{{ stats.summary.totalViews }}</span>
        <span class="kpi-trend faint">获赞 {{ stats.summary.totalLikes }}</span>
      </el-card>
      <el-card shadow="never" class="kpi-card highlight">
        <span class="kpi-label">成交总额</span>
        <span class="kpi-value">¥{{ stats.summary.gmv }}</span>
        <span class="kpi-trend positive">二手好物持续流转</span>
      </el-card>
    </section>
    <section class="kpi-grid" v-else>
      <el-card v-for="i in 4" :key="i" shadow="never" class="kpi-card skeleton-card"></el-card>
    </section>

    <!-- 近30天趋势 -->
    <el-card shadow="never" class="chart-card">
      <div class="chart-header">
        <h2 class="chart-title">近 30 天浏览 / 收藏趋势</h2>
        <span class="chart-hint">点击日期可看当日明细</span>
      </div>
      <div v-if="selectedDay" class="day-detail">
        选中 <b>{{ selectedDay.date }}</b>：浏览量 <b>{{ selectedDay.views }}</b> ·
        收藏 <b>{{ selectedDay.likes }}</b> · 成交 <b>{{ selectedDay.sales }}</b> 件
      </div>
      <div ref="trendRef" class="chart-body"></div>
    </el-card>

    <div class="chart-row">
      <!-- 类目分布 -->
      <el-card shadow="never" class="chart-card">
        <div class="chart-header">
          <h2 class="chart-title">商品类目分布</h2>
          <span class="chart-hint">按商品数量</span>
        </div>
        <div ref="categoryRef" class="chart-body"></div>
      </el-card>

      <!-- 浏览量 Top 商品 -->
      <el-card shadow="never" class="chart-card">
        <div class="chart-header">
          <h2 class="chart-title">浏览量 Top 商品</h2>
          <span class="chart-hint">横向条形图</span>
        </div>
        <div ref="topRef" class="chart-body"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  TitleComponent, TooltipComponent, GridComponent, LegendComponent, DataZoomComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { dashboardApi } from '../services/api'

echarts.use([
  BarChart, LineChart, PieChart,
  TitleComponent, TooltipComponent, GridComponent, LegendComponent, DataZoomComponent,
  CanvasRenderer
])

const router = useRouter()

interface TrendPoint { date: string; views: number; likes: number; sales: number }
interface StatsData {
  summary: { published: number; onSale: number; sold: number; totalViews: number; totalLikes: number; gmv: number }
  trend: TrendPoint[]
  categories: { name: string; count: number; gmv: number }[]
  topProducts: { name: string; price: number; views: number; likes: number }[]
}

const stats = ref<StatsData | null>(null)
const selectedDay = ref<TrendPoint | null>(null)

const trendRef = ref<HTMLDivElement | null>(null)
const categoryRef = ref<HTMLDivElement | null>(null)
const topRef = ref<HTMLDivElement | null>(null)

let trendChart: echarts.ECharts | null = null
let categoryChart: echarts.ECharts | null = null
let topChart: echarts.ECharts | null = null

const PALETTE = ['#10b981', '#f59e0b', '#0ea5e9', '#8b5cf6', '#f43f5e', '#14b8a6']

async function loadStats() {
  try {
    const res = await dashboardApi.getStats()
    if (res.code === 200 && res.data) {
      stats.value = res.data
      await nextTick()
      renderCharts(res.data)
    }
  } catch (e) {
    console.error('加载看板数据失败:', e)
  }
}

function renderCharts(data: StatsData) {
  renderTrend(data)
  renderCategory(data)
  renderTop(data)
}

// 折线图：浏览 + 收藏，标记成交
function renderTrend(data: StatsData) {
  if (!trendRef.value) return
  trendChart?.dispose()
  trendChart = echarts.init(trendRef.value)
  const dates = data.trend.map(p => p.date)
  trendChart.setOption({
    color: PALETTE,
    tooltip: { trigger: 'axis' },
    legend: { data: ['浏览量', '收藏数'], top: 0, textStyle: { color: '#6b7280' } },
    grid: { left: 8, right: 12, top: 32, bottom: 8, containLabel: true },
    dataZoom: [{ type: 'inside', start: 0, end: 100 }],
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#d1d5db' } }, axisLabel: { color: '#9ca3af' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f3f4f6' } }, axisLabel: { color: '#9ca3af' } },
    series: [
      { name: '浏览量', type: 'line', smooth: true, symbol: 'circle', symbolSize: 6,
        lineStyle: { width: 3, color: '#10b981' }, itemStyle: { color: '#10b981' },
        areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [{ offset: 0, color: 'rgba(16,185,129,0.25)' }, { offset: 1, color: 'rgba(16,185,129,0)' }] } },
        data: data.trend.map(p => p.views) },
      { name: '收藏数', type: 'line', smooth: true, symbol: 'circle', symbolSize: 5,
        lineStyle: { width: 2, color: '#f59e0b' }, itemStyle: { color: '#f59e0b' },
        data: data.trend.map(p => p.likes) }
    ]
  })
  trendChart.on('click', (params) => {
    const idx = params.dataIndex
    if (idx != null && data.trend[idx]) {
      selectedDay.value = data.trend[idx]
    }
  })
}

// 饼图：类目分布
function renderCategory(data: StatsData) {
  if (!categoryRef.value) return
  categoryChart?.dispose()
  categoryChart = echarts.init(categoryRef.value)
  categoryChart.setOption({
    color: PALETTE,
    tooltip: { trigger: 'item', formatter: '{b}: {c} 件 ({d}%)' },
    legend: { bottom: 0, textStyle: { color: '#6b7280', fontSize: 11 } },
    series: [{
      name: '类目分布', type: 'pie', radius: ['45%', '68%'],
      center: ['50%', '45%'], avoidLabelOverlap: true,
      itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontWeight: 'bold', fontSize: 13 } },
      data: data.categories.map(c => ({ name: c.name, value: c.count }))
    }]
  })
}

// 横向条形图：Top 商品浏览量
function renderTop(data: StatsData) {
  if (!topRef.value) return
  topChart?.dispose()
  topChart = echarts.init(topRef.value)
  const items = [...data.topProducts].reverse()
  topChart.setOption({
    color: ['#10b981'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' },
      formatter: (params: any) => {
        const p = params[0]
        const item = items[0] && data.topProducts[data.topProducts.length - 1 - p.dataIndex]
        return item
          ? `${item.name}<br/>价格 ¥${item.price} · 浏览 ${item.views} · 收藏 ${item.likes}`
          : ''
      } },
    grid: { left: 8, right: 28, top: 8, bottom: 8, containLabel: true },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#f3f4f6' } }, axisLabel: { color: '#9ca3af' } },
    yAxis: { type: 'category', data: items.map(i => i.name), axisLine: { show: false },
      axisTick: { show: false }, axisLabel: { color: '#374151', fontSize: 12, width: 80, overflow: 'truncate' } },
    series: [{
      name: '浏览量', type: 'bar', data: items.map(i => i.views),
      barWidth: 14, itemStyle: { borderRadius: [0, 7, 7, 0] },
      label: { show: true, position: 'right', color: '#6b7280', fontSize: 11 }
    }]
  })
}

function handleResize() {
  trendChart?.resize()
  categoryChart?.resize()
  topChart?.resize()
}

onMounted(() => {
  loadStats()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  categoryChart?.dispose()
  topChart?.dispose()
})
</script>

<style scoped>
.dashboard-page {
  min-height: 100vh;
  background-color: var(--color-bg-page);
  padding: var(--space-4);
  padding-bottom: var(--space-12);
  max-width: 760px;
  margin: 0 auto;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.back-btn, .refresh-btn {
  width: 40px; height: 40px;
  display: flex; align-items: center; justify-content: center;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-full);
  color: var(--color-text-primary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}
.back-btn:hover, .refresh-btn:hover { background: var(--color-primary-50); }
.back-btn:active, .refresh-btn:active { transform: scale(0.94); }

.header-title {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}
@media (min-width: 640px) { .kpi-grid { grid-template-columns: repeat(4, 1fr); } }

.kpi-card {
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
}
.kpi-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding: var(--space-4) var(--space-4);
}
.kpi-card.highlight {
  background: linear-gradient(135deg, var(--color-primary-600), var(--color-primary-700));
  border-color: transparent;
  color: #fff;
}

.kpi-label { font-size: var(--text-xs); color: var(--color-text-tertiary); }
.kpi-card.highlight .kpi-label { color: rgba(255,255,255,0.8); }
.kpi-value { font-size: var(--text-2xl); font-weight: var(--font-extrabold); color: var(--color-primary-600); line-height: 1.1; }
.kpi-card.highlight .kpi-value { color: #fff; }
.kpi-trend { font-size: var(--text-xs); color: var(--color-text-tertiary); }
.kpi-trend.positive { color: rgba(255,255,255,0.85); }

.skeleton-card { height: 96px; }
.skeleton-card :deep(.el-card__body) { padding: 0; }

.chart-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-4);
}
@media (min-width: 720px) { .chart-row { grid-template-columns: 1fr 1fr; } }

.chart-card {
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-card);
  margin-bottom: var(--space-4);
}
.chart-card :deep(.el-card__body) { padding: var(--space-4); }
.chart-row .chart-card { margin-bottom: 0; }

.chart-header {
  display: flex; align-items: baseline; justify-content: space-between;
  margin-bottom: var(--space-2);
}
.chart-title { font-size: var(--text-sm); font-weight: var(--font-bold); color: var(--color-text-primary); margin: 0; }
.chart-hint { font-size: var(--text-xs); color: var(--color-text-tertiary); }

.day-detail {
  font-size: var(--text-xs); color: var(--color-text-secondary);
  background: var(--color-primary-50);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-2);
}
.day-detail b { color: var(--color-primary-600); }

.chart-body { width: 100%; height: 260px; }
</style>