<template>
  <div class="community-page">
    <!-- 下拉刷新指示器 -->
    <div class="pull-refresh-indicator" :style="{ transform: `translateY(${pullDistance}px)`, transition: isRefreshing ? 'none' : 'transform 0.3s ease' }">
      <div v-if="pullDistance > 0 || isRefreshing" class="pull-indicator-inner">
        <div class="pull-spinner" :class="{ spinning: isRefreshing }"></div>
        <span class="pull-text">{{ isRefreshing ? '刷新中...' : canTrigger ? '松手刷新' : '下拉刷新' }}</span>
      </div>
    </div>
    <div class="page-header">
      <h1 class="page-title">社区</h1>
      <button class="write-btn" @click="goToCreatePost">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        写帖子
      </button>
    </div>

    <div class="top-tabs">
      <button :class="{ active: tab === 'latest' }" @click="tab = 'latest'">最新</button>
      <button :class="{ active: tab === 'hot' }" @click="tab = 'hot'">热门</button>
    </div>

    <div class="content-area">
      <div v-if="loading" class="loading-skeletons">
        <div class="skeleton" v-for="i in 5" :key="i"></div>
      </div>

      <div v-else-if="error" class="error-state">
        <div class="error-icon">⚠️</div>
        <p>{{ error }}</p>
        <button @click="loadFeed">重试</button>
      </div>

      <div v-else-if="items.length === 0" class="empty-state">
        <div class="empty-icon">🌿</div>
        <p class="empty-title">这里还空空如也</p>
        <p class="empty-desc">快来发布第一帖，和大家分享你的校园生活吧！</p>
      </div>

      <div v-else class="feed-list">
        <PostCard
          v-for="item in items"
          :key="'post-' + item.id"
          :post="item"
          @click="goToPost(item)"
          @like-toggled="(data) => handleLikeToggle(item, data)"
        />
        <div ref="scrollTrigger" class="scroll-trigger"></div>
      </div>

      <div v-if="loadingMore" class="load-more">
        <div class="dot-bounce">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
      </div>
    </div>

    <button class="fab" @click="goToCreatePost">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
        <line x1="12" y1="5" x2="12" y2="19"></line>
        <line x1="5" y1="12" x2="19" y2="12"></line>
      </svg>
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { postApi } from '../services/api'
import PostCard from '../components/PostCard.vue'
import { usePullRefresh } from '../use/usePullRefresh'

const router = useRouter()
const tab = ref('latest')
const items = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const error = ref(null)
const page = ref(1)
const hasMore = ref(true)
const scrollTrigger = ref(null)
let observer = null

onMounted(() => {
  loadFeed()
  setupScrollObserver()
})

onUnmounted(() => {
  if (observer) observer.disconnect()
})

watch(tab, () => {
  page.value = 1
  items.value = []
  loadFeed()
})

async function loadFeed() {
  loading.value = true
  error.value = null
  try {
    const sortBy = tab.value === 'hot' ? 'hot' : 'time_desc'
    const res = await postApi.getPosts({ page: page.value, size: 10, sortBy })
    if (res.code === 200) {
      const list = res.data.list || []
      items.value = list.map(p => ({ ...p, itemType: 'POST', postTypeText: p.postTypeText }))
      hasMore.value = list.length >= 10
    }
  } catch {
    error.value = '加载失败，请检查网络连接'
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  page.value++
  try {
    const sortBy = tab.value === 'hot' ? 'hot' : 'time_desc'
    const res = await postApi.getPosts({ page: page.value, size: 10, sortBy })
    if (res.code === 200) {
      const list = res.data.list || []
      items.value = [...items.value, ...list.map(p => ({ ...p, itemType: 'POST' }))]
      hasMore.value = list.length >= 10
    }
  } catch {
    page.value--
  } finally {
    loadingMore.value = false
  }
}

function setupScrollObserver() {
  observer = new IntersectionObserver(
    (entries) => {
      const entry = entries[0]
      if (entry.isIntersecting && hasMore.value && !loadingMore.value && !loading.value) {
        loadMore()
      }
    },
    { rootMargin: '200px', threshold: 0.1 }
  )
  watch(scrollTrigger, (el) => {
    if (el) observer.observe(el)
  })
}

// 下拉刷新
const { isRefreshing, pullDistance, canTrigger } = usePullRefresh(async () => {
  page.value = 1
  hasMore.value = true
  await loadFeed()
})

/* 活动类型帖子跳转到活动详情页，其他类型跳转到帖子详情页 */
function handleLikeToggle(item, data) {
  item.isLiked = data.isLiked
  item.likeCount = data.count
}

function goToPost(post) {
  if (post.postType === 'ACTIVITY') {
    router.push(`/activities/${post.id}`)
  } else {
    router.push(`/community/posts/${post.id}`)
  }
}
function goToCreatePost() { router.push('/community/posts/create') }
</script>

<style scoped>
.community-page {
  position: relative;
  min-height: 100vh;
  background: var(--color-bg-page);
  padding-bottom: calc(var(--tabbar-height) + var(--space-4));
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

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-4) 0;
}

.page-title {
  font-size: 22px;
  font-weight: 800;
  color: #1a1a2e;
  margin: 0;
}

.write-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: #fff;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(16,185,129,0.25);
  transition: all 0.2s ease;
}

.write-btn:active { transform: scale(0.95); }

.top-tabs {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border-light);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.top-tabs button {
  padding: var(--space-2) var(--space-5);
  border: none;
  background: var(--color-bg-tertiary);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: all var(--duration-normal) var(--ease-out);
  line-height: var(--leading-tight);
}

.top-tabs button:hover:not(.active) {
  background: var(--color-primary-50);
  color: var(--color-primary-600);
}

.top-tabs button.active {
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  font-weight: var(--font-semibold);
  box-shadow: var(--shadow-green);
}

.content-area {
  padding: var(--space-4);
  max-width: var(--container-xl);
  margin: 0 auto;
}

.loading-skeletons {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.loading-skeletons .skeleton {
  height: 120px;
  border-radius: var(--radius-xl);
  background: linear-gradient(
    90deg,
    var(--color-primary-50) 25%,
    var(--color-primary-100) 37%,
    var(--color-primary-50) 63%
  );
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

@keyframes shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) var(--space-4);
  text-align: center;
}

.error-icon {
  font-size: 48px;
  margin-bottom: var(--space-4);
}

.error-state p {
  color: var(--color-text-secondary);
  font-size: var(--text-base);
  margin-bottom: var(--space-5);
}

.error-state button {
  padding: var(--space-2_5) var(--space-8);
  border: none;
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-full);
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  box-shadow: var(--shadow-green);
  transition: all var(--duration-normal) var(--ease-out);
}

.error-state button:hover {
  box-shadow: var(--shadow-green-lg);
  transform: translateY(-1px);
}

.error-state button:active {
  transform: scale(0.97);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) var(--space-4);
  text-align: center;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: var(--space-4);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.empty-title {
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);
}

.empty-desc {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  max-width: 280px;
  line-height: var(--leading-relaxed);
}

.feed-list {
  /* 小红书风格瀑布流布局 */
  column-count: 2;
  column-gap: 8px;
}

.scroll-trigger {
  height: 1px;
  width: 100%;
}

.load-more {
  display: flex;
  justify-content: center;
  padding: var(--space-6) 0;
}

.dot-bounce {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.dot-bounce .dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary-500);
  animation: dotBounce 1.4s ease-in-out infinite both;
}

.dot-bounce .dot:nth-child(1) {
  animation-delay: 0s;
}

.dot-bounce .dot:nth-child(2) {
  animation-delay: 0.16s;
}

.dot-bounce .dot:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes dotBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.fab {
  position: fixed;
  bottom: calc(var(--tabbar-height) + var(--space-5));
  right: var(--space-5);
  width: 56px;
  height: 56px;
  border-radius: var(--radius-full);
  border: none;
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  cursor: pointer;
  box-shadow: var(--shadow-green-lg);
  z-index: var(--z-overlay);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--duration-normal) var(--ease-spring);
}

.fab:hover {
  transform: scale(1.08) rotate(90deg);
  box-shadow: 0 8px 30px rgba(16, 185, 129, 0.4);
}

.fab:active {
  transform: scale(0.95);
}

@media (min-width: 769px) {
  .feed-list {
    column-count: 3;
    column-gap: 8px;
  }

  .top-tabs {
    padding: var(--space-4) var(--space-6);
    gap: var(--space-4);
  }

  .top-tabs button {
    padding: var(--space-2_5) var(--space-6);
    font-size: var(--text-base);
  }

  .content-area {
    padding: var(--space-5) var(--space-6);
  }
}

@media (min-width: 1025px) {
  .feed-list {
    column-count: 4;
    column-gap: 8px;
  }
}
</style>
