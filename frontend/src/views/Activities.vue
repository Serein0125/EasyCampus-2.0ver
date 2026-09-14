<template>
  <div class="activities-page">
    <!-- 下拉刷新指示器 -->
    <div class="pull-refresh-indicator" :style="{ transform: `translateY(${pullDistance}px)`, transition: isRefreshing ? 'none' : 'transform 0.3s ease' }">
      <div v-if="pullDistance > 0 || isRefreshing" class="pull-indicator-inner">
        <div class="pull-spinner" :class="{ spinning: isRefreshing }"></div>
        <span class="pull-text">{{ isRefreshing ? '刷新中...' : canTrigger ? '松手刷新' : '下拉刷新' }}</span>
      </div>
    </div>

    <header class="page-header">
      <h1 class="page-title">校园活动</h1>
    </header>

    <div class="activity-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.value"
        class="tab-btn"
        :class="{ active: activeTab === tab.value }"
        @click="switchTab(tab.value)"
      >
        {{ tab.label }}
      </button>
    </div>

    <main class="activity-list">
      <div v-if="loading" class="loading-state">
        <div v-for="i in 4" :key="i" class="skeleton-card">
          <div class="skeleton-banner"></div>
          <div class="skeleton-body">
            <div class="skeleton-line short"></div>
            <div class="skeleton-line medium"></div>
            <div class="skeleton-line long"></div>
          </div>
        </div>
      </div>

      <div v-else-if="error" class="error-state">
        <div class="error-icon">😔</div>
        <p class="error-text">{{ error }}</p>
        <button @click="loadActivities()" class="retry-btn">点击重试</button>
      </div>

      <div v-else-if="activities.length === 0" class="empty-state">
        <div class="empty-icon">📅</div>
        <h3 class="empty-title">{{ emptyTitle }}</h3>
        <p class="empty-desc">{{ emptyDesc }}</p>
      </div>

      <div v-else class="activity-grid">
        <ActivityCard
          v-for="activity in activities"
          :key="activity.id"
          :activity="activity"
          @click="goToDetail(activity.id)"
        />
      </div>

      <div v-if="!loading && hasMore && activities.length > 0" class="load-more">
        <button @click="loadMore" :disabled="loadingMore" class="load-more-btn">
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>

      <div v-if="!hasMore && activities.length > 0" class="no-more">
        <span class="no-more-line"></span>
        <span class="no-more-text">已经到底啦~</span>
        <span class="no-more-line"></span>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi } from '../services/api'
import ActivityCard from '../components/ActivityCard.vue'
import { usePullRefresh } from '../use/usePullRefresh'

const router = useRouter()

const tabs = [
  { label: '即将开始', value: 'upcoming' },
  { label: '进行中', value: 'ongoing' },
  { label: '已结束', value: 'past' }
]

const activeTab = ref('upcoming')
const activities = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const error = ref(null)
const page = ref(1)
const hasMore = ref(true)
const pageSize = 10

const emptyTitle = computed(() => {
  const map = { upcoming: '暂无即将开始的活动', ongoing: '当前没有进行中的活动', past: '暂无已结束的活动' }
  return map[activeTab.value] || '暂无活动'
})

const emptyDesc = computed(() => {
  const map = { upcoming: '敬请期待更多精彩活动', ongoing: '活动结束后会出现在这里', past: '' }
  return map[activeTab.value] || ''
})

onMounted(() => {
  loadActivities()
})

watch(activeTab, () => {
  page.value = 1
  hasMore.value = true
  activities.value = []
  loadActivities()
})

async function loadActivities(isLoadMore = false) {
  try {
    error.value = null
    if (isLoadMore) {
      loadingMore.value = true
    } else {
      loading.value = true
    }

    const params = {
      page: page.value,
      size: pageSize,
      status: activeTab.value
    }

    const response = await activityApi.getActivities(params)

    if (response.code === 200) {
      const data = response.data || {}
      const newItems = data.list || data.records || data.items || []

      if (isLoadMore) {
        activities.value = [...activities.value, ...newItems]
      } else {
        activities.value = newItems
      }

      const total = data.total || 0
      hasMore.value = activities.value.length < total
    } else {
      throw new Error(response.message || '加载活动失败')
    }
  } catch {
    error.value = '加载活动失败，请检查网络连接'
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

function loadMore() {
  if (!hasMore.value || loadingMore.value) return
  page.value++
  loadActivities(true)
}

// 下拉刷新：重新加载活动数据
const { isRefreshing, pullDistance, canTrigger } = usePullRefresh(async () => {
  page.value = 1
  hasMore.value = true
  await loadActivities()
})

function switchTab(tab) {
  activeTab.value = tab
}

function goToDetail(activityId) {
  router.push(`/activities/${activityId}`)
}
</script>

<style scoped>
.activities-page {
  min-height: 100vh;
  background-color: var(--color-bg-page);
  position: relative;
}

.page-header {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  background: var(--color-bg-primary);
  padding: 0 var(--space-4);
  border-bottom: 1px solid var(--color-border-light);
  display: flex;
  align-items: center;
  height: 48px;
}

.page-title {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
}

.activity-tabs {
  display: flex;
  gap: 0;
  padding: 0 var(--space-4);
  background: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border-light);
}

.tab-btn {
  flex: 1;
  padding: var(--space-3) 0;
  border: none;
  background: none;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  position: relative;
  transition: all var(--duration-normal) var(--ease-out);
  font-family: var(--font-sans);
}

.tab-btn:hover {
  color: var(--color-primary-500);
}

.tab-btn.active {
  color: var(--color-primary-600);
  font-weight: var(--font-bold);
}

.tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: var(--gradient-primary);
  border-radius: var(--radius-full);
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  padding: var(--space-5) var(--space-6);
  max-width: var(--container-xl, 1200px);
  margin: 0 auto;
}

@media (max-width: 1024px) {
  .activity-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-3);
    padding: var(--space-4);
  }
}

@media (max-width: 640px) {
  .activity-grid {
    grid-template-columns: 1fr;
    padding: var(--space-4);
  }
}

.loading-state {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  padding: var(--space-5) var(--space-6);
  max-width: var(--container-xl, 1200px);
  margin: 0 auto;
}

@media (max-width: 1024px) {
  .loading-state { grid-template-columns: repeat(2, 1fr); padding: var(--space-4); }
}
@media (max-width: 640px) {
  .loading-state { grid-template-columns: 1fr; padding: var(--space-4); }
}

.skeleton-card {
  background: var(--color-bg-primary);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.skeleton-banner {
  width: 100%;
  aspect-ratio: 4 / 3;
  background: linear-gradient(90deg, var(--color-primary-50) 25%, var(--color-primary-100) 50%, var(--color-primary-50) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-body {
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2_5);
}

.skeleton-line {
  height: 16px;
  background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-gray-200) 50%, var(--color-gray-100) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-xs);
}

.skeleton-line.short { width: 50%; }
.skeleton-line.medium { width: 75%; }
.skeleton-line.long { width: 90%; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.error-state {
  text-align: center;
  padding: var(--space-16) var(--space-5);
}

.error-icon {
  font-size: 64px;
  margin-bottom: var(--space-4);
  opacity: 0.8;
}

.error-text {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-6);
}

.retry-btn {
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  padding: var(--space-3) var(--space-8);
  border-radius: var(--radius-xl);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  border: none;
  cursor: pointer;
  box-shadow: var(--shadow-green);
  transition: all var(--duration-normal) var(--ease-out);
  font-family: var(--font-sans);
}

.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-green-lg);
}

.retry-btn:active {
  transform: translateY(0) scale(0.98);
}

.empty-state {
  text-align: center;
  padding: var(--space-20) var(--space-5);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: var(--space-4);
  opacity: 0.6;
}

.empty-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
}

.empty-desc {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
}

.load-more {
  text-align: center;
  padding: var(--space-2) 0 var(--space-6);
}

.load-more-btn {
  background: var(--color-primary-50);
  color: var(--color-primary-600);
  padding: var(--space-2_5) var(--space-8);
  border-radius: var(--radius-xl);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  border: 1.5px solid var(--color-primary-200);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
  font-family: var(--font-sans);
}

.load-more-btn:hover:not(:disabled) {
  background: var(--color-primary-100);
  border-color: var(--color-primary-400);
  transform: translateY(-1px);
  box-shadow: var(--shadow-green);
}

.load-more-btn:active:not(:disabled) {
  transform: translateY(0);
}

.load-more-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-2) 0 var(--space-8);
  color: var(--color-text-tertiary);
  font-size: var(--text-xs);
}

.no-more-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, var(--color-border-light), transparent);
}

/* 下拉刷新指示器 */
.pull-refresh-indicator {
  position: absolute;
  top: -40px;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 40px;
  z-index: 5;
}
.pull-indicator-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-tertiary, #9ca3af);
  font-size: 13px;
}
.pull-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid var(--color-border-light, #e5e7eb);
  border-top-color: var(--color-primary-500, #10b981);
  border-radius: 50%;
}
.pull-spinner.spinning {
  animation: pullRefreshSpin 0.8s linear infinite;
}
@keyframes pullRefreshSpin {
  to { transform: rotate(360deg); }
}
</style>
