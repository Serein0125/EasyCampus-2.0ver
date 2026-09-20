<template>
  <div class="community-page">
    <div class="page-header">
      <h1 class="page-title">社区</h1>
      <el-button type="primary" round class="write-btn" @click="goToCreatePost">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
        写帖子
      </el-button>
    </div>

    <div class="top-tabs">
      <el-radio-group v-model="tab" class="feed-tabs">
        <el-radio-button value="latest">最新</el-radio-button>
        <el-radio-button value="hot">热门</el-radio-button>
      </el-radio-group>
    </div>

    <div class="content-area">
      <div v-if="loading" class="loading-skeletons">
        <div class="skeleton" v-for="i in 5" :key="i"></div>
      </div>

      <el-empty v-else-if="error" :description="error">
        <el-button type="primary" round @click="loadFeed">重试</el-button>
      </el-empty>

      <el-empty v-else-if="items.length === 0">
        <template #description>
          <p class="empty-title">这里还空空如也</p>
          <p class="empty-desc">快来发布第一帖，和大家分享你的校园生活吧！</p>
        </template>
      </el-empty>

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

    <el-button class="fab" type="primary" circle @click="goToCreatePost">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
        <line x1="12" y1="5" x2="12" y2="19"></line>
        <line x1="5" y1="12" x2="19" y2="12"></line>
      </svg>
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { postApi } from '../services/api'
import PostCard from '../components/PostCard.vue'

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

// 下拉刷新已移除（H5 移动端特化）
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

.top-tabs {
  display: flex;
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border-light);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
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
  margin: 0;
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

@media (min-width: 769px) {
  .feed-list {
    column-count: 3;
    column-gap: 8px;
  }

  .top-tabs {
    padding: var(--space-4) var(--space-6);
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

<style>
/* el-card / el-button 根节点由 EP 渲染，scoped 无法命中，用全局类兜底 */
.community-page .write-btn.el-button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  border: none;
  font-weight: 600;
}

.community-page .fab.el-button {
  position: fixed;
  bottom: var(--space-5);
  right: var(--space-5);
  width: 56px;
  height: 56px;
  font-size: 22px;
  z-index: var(--z-overlay);
  box-shadow: var(--shadow-green-lg);
}

/* 最新/热门 胶囊 tab：覆盖 el-radio-button 默认边框为胶囊样式 */
.community-page .feed-tabs.el-radio-group {
  display: inline-flex;
  padding: 3px;
  background: var(--color-bg-tertiary, #f3f4f6);
  border-radius: var(--radius-full);
}

.community-page .feed-tabs .el-radio-button {
  --el-radio-button-checked-bg-color: var(--color-primary-500, #10b981);
  --el-radio-button-checked-border-color: transparent;
  --el-radio-button-checked-text-color: #fff;
}

.community-page .feed-tabs .el-radio-button__inner {
  border: none;
  border-radius: var(--radius-full);
  background: transparent;
  box-shadow: none;
  color: var(--color-text-secondary, #6b7280);
  font-weight: 500;
  padding: 6px 22px;
}

.community-page .feed-tabs .el-radio-button.is-active .el-radio-button__inner {
  background: var(--gradient-primary, linear-gradient(135deg, #10b981, #34d399));
  color: var(--color-text-inverse, #fff);
  box-shadow: var(--shadow-green);
}
</style>
