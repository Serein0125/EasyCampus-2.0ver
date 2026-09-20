<template>
  <div class="search-page">
    <div class="search-header">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="22" height="22">
          <path d="M15 18l-6-6 6-6"/>
        </svg>
      </button>
      <div class="search-input-box" :class="{ focused: inputFocused }">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="16" height="16">
          <circle cx="11" cy="11" r="8"/>
          <path d="M21 21l-4.35-4.35"/>
        </svg>
        <input
          ref="searchInput"
          v-model="keyword"
          type="text"
          placeholder="搜索帖子、商品、用户..."
          @keyup.enter="doSearch"
          @focus="inputFocused = true"
          @blur="inputFocused = false"
        />
        <button v-if="keyword" class="clear-btn" @click="clearKeyword">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <circle cx="12" cy="12" r="10"/>
            <path d="M15 9l-6 6M9 9l6 6"/>
          </svg>
        </button>
      </div>
      <button class="search-btn" @click="doSearch">搜索</button>
    </div>

    <div v-if="hasSearched" class="filter-tabs">
      <el-tabs :model-value="activeType" @tab-change="switchType">
        <el-tab-pane
          v-for="tab in filterTabs"
          :key="tab.key"
          :name="tab.key"
        >
          <template #label>
            {{ tab.label }}
            <span v-if="tab.count > 0" class="tab-count">{{ tab.count }}</span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <div v-if="hasSearched && !isEmpty && showSortBar" class="sort-bar">
      <span class="sort-label">排序：</span>
      <button
        v-for="opt in currentSortOptions"
        :key="opt.value"
        class="sort-btn"
        :class="{ active: currentSort === opt.value }"
        @click="changeSort(opt.value)"
      >
        {{ opt.label }}
      </button>
    </div>

    <div class="search-body">
      <div v-if="loading" class="skeleton-grid">
        <div v-for="i in 8" :key="'sk-' + i" class="skeleton-card">
          <div class="skeleton-image skeleton"></div>
          <div class="skeleton-body">
            <div class="skeleton-title skeleton"></div>
            <div class="skeleton-text skeleton"></div>
            <div class="skeleton-text short skeleton"></div>
          </div>
        </div>
      </div>

      <div v-else-if="!hasSearched" class="initial-state">
        <el-card v-if="searchHistory.length > 0" class="history-section" shadow="never" :body-style="{ padding: '16px' }">
          <div class="section-label">
            <span class="section-label-text">搜索历史</span>
            <button class="clear-history-btn" @click="clearHistory">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
                <polyline points="3 6 5 6 21 6"/>
                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
              </svg>
              清空
            </button>
          </div>
          <div class="history-tags">
            <button
              v-for="(item, idx) in searchHistory"
              :key="'hist-' + idx"
              class="history-tag"
              @click="searchFromHistory(item)"
            >
              {{ item }}
            </button>
          </div>
        </el-card>

        <el-card class="hot-section" shadow="never" :body-style="{ padding: '16px' }">
          <div class="section-label">
            <span class="section-label-text">🔥 热门搜索</span>
          </div>
          <div class="hot-tags">
            <button
              v-for="(item, idx) in hotSearchList"
              :key="'hot-' + idx"
              class="hot-tag"
              :class="{ 'hot-tag--top': idx < 3 }"
              @click="searchFromHistory(item)"
            >
              <span class="hot-rank" :class="{ 'hot-rank--top': idx < 3 }">{{ idx + 1 }}</span>
              {{ item }}
            </button>
          </div>
        </el-card>

        <el-card class="tag-section" shadow="never" :body-style="{ padding: '16px' }">
          <div class="section-label">
            <span class="section-label-text"># 热门标签</span>
          </div>
          <div class="tag-cloud">
            <button
              v-for="tag in hotTags"
              :key="tag"
              class="tag-cloud-item"
              @click="searchFromHistory(tag)"
            >
              {{ tag }}
            </button>
          </div>
        </el-card>

        <div class="initial-hint">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="48" height="48">
            <circle cx="11" cy="11" r="8"/>
            <path d="M21 21l-4.35-4.35"/>
          </svg>
          <p class="initial-text">输入关键词搜索帖子、商品和用户</p>
        </div>
      </div>

      <el-empty v-else-if="isEmpty" :description="'未找到与“' + searchKeyword + '”相关的内容'">
        <p class="empty-sub">试试其他关键词吧</p>
      </el-empty>

      <template v-else>
        <section v-if="activeType === 'all' || activeType === 'user'" class="result-section">
          <div class="section-header" v-if="users.length > 0">
            <h3 class="section-title">相关用户</h3>
            <span class="section-count">{{ result.userTotal }}个结果</span>
            <button v-if="activeType === 'all' && result.userTotal > users.length" class="view-all-btn" @click="switchType('user')">查看全部</button>
          </div>
          <div class="user-list">
            <el-card
              v-for="user in users"
              :key="'user-' + user.id"
              class="user-item"
              shadow="hover"
              :body-style="{ padding: '0px' }"
              @click="goToUser(user.id)"
            >
              <div class="user-item-inner">
                <img :src="user.avatar || defaultAvatar" class="user-avatar" loading="lazy" @error="onAvatarError" />
                <div class="user-info">
                  <div class="user-name-row">
                    <span class="user-nickname" v-html="highlightText(user.nickname || user.username, searchKeyword)"></span>
                    <span v-if="user.matchedField === 'nickname'" class="match-badge">昵称匹配</span>
                    <span v-else class="match-badge">用户名匹配</span>
                  </div>
                  <div class="user-meta">
                    <span v-if="user.school">{{ user.school }}</span>
                    <span v-if="user.major">{{ user.major }}</span>
                  </div>
                  <p v-if="user.bio" class="user-bio" v-html="highlightText(user.bio, searchKeyword)"></p>
                </div>
              </div>
            </el-card>
          </div>
        </section>

        <section v-if="activeType === 'all' || activeType === 'post'" class="result-section">
          <div class="section-header" v-if="posts.length > 0">
            <h3 class="section-title">相关帖子</h3>
            <span class="section-count">{{ result.postTotal }}个结果</span>
            <button v-if="activeType === 'all' && result.postTotal > posts.length" class="view-all-btn" @click="switchType('post')">查看全部</button>
          </div>
          <div class="card-grid">
            <PostCard
              v-for="post in normalizedPosts"
              :key="'post-' + post.id"
              :post="post"
              @click="goToPost(post)"
              @like-toggled="(data) => handleLikeToggle(post, data)"
            />
          </div>
          <div v-if="activeType === 'post' && loadingMore" class="loading-more">
            <div class="loading-spinner small"></div>
            <span>加载中...</span>
          </div>
          <div v-if="activeType === 'post' && !hasMorePosts && posts.length > 0" class="no-more">
            <span>没有更多了</span>
          </div>
        </section>

        <section v-if="activeType === 'all' || activeType === 'product'" class="result-section">
          <div class="section-header" v-if="products.length > 0">
            <h3 class="section-title">相关商品</h3>
            <span class="section-count">{{ result.productTotal }}个结果</span>
            <button v-if="activeType === 'all' && result.productTotal > products.length" class="view-all-btn" @click="switchType('product')">查看全部</button>
          </div>
          <div class="card-grid">
            <ProductCard
              v-for="product in normalizedProducts"
              :key="'product-' + product.id"
              :product="product"
              @click="goToProduct(product.id)"
            />
          </div>
          <div v-if="activeType === 'product' && loadingMore" class="loading-more">
            <div class="loading-spinner small"></div>
            <span>加载中...</span>
          </div>
          <div v-if="activeType === 'product' && !hasMoreProducts && products.length > 0" class="no-more">
            <span>没有更多了</span>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchApi } from '../services/api'
import PostCard from '../components/PostCard.vue'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><circle cx="20" cy="20" r="20" fill="#eee"/><circle cx="20" cy="15" r="8" fill="#ccc"/><ellipse cx="20" cy="35" rx="12" ry="8" fill="#ccc"/></svg>')

const keyword = ref('')
const searchKeyword = ref('')
const hasSearched = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const activeType = ref('all')
const searchInput = ref(null)
const inputFocused = ref(false)

const HISTORY_KEY = 'search_history'
const MAX_HISTORY = 15

const searchHistory = ref([])

const hotSearchList = ref([
  '二手教材', '自行车', '考研资料', '耳机', '台灯',
  '显示器', '键盘', '宿舍好物', '运动鞋', '充电宝'
])

const hotTags = ref([
  '#美食', '#考研', '#二手', '#宿舍生活', '#运动',
  '#学习笔记', '#摄影', '#课程', '#志愿活动', '#校园',
  '#数码', '#教材', '#社团招新', '#风景', '#探店'
])

const result = ref({
  posts: [],
  postTotal: 0,
  products: [],
  productTotal: 0,
  users: [],
  userTotal: 0,
  keyword: '',
  page: 1,
  size: 20
})

const currentPage = ref(1)
const pageSize = 20
const currentSort = ref('')

const sortOptions = {
  product: [
    { value: 'time_desc', label: '最新' },
    { value: 'price_asc', label: '价格↑' },
    { value: 'price_desc', label: '价格↓' }
  ],
  post: [
    { value: 'time_desc', label: '最新' },
    { value: 'hot', label: '最热' }
  ]
}

const showSortBar = computed(() => activeType.value === 'post' || activeType.value === 'product')
const currentSortOptions = computed(() => sortOptions[activeType.value] || [])

const posts = computed(() => result.value.posts || [])
const products = computed(() => result.value.products || [])
const users = computed(() => result.value.users || [])

const normalizedPosts = computed(() =>
  posts.value.map(p => ({
    ...p,
    isLiked: p.isLiked || false,
    tags: p.tags || '',
    coverImage: p.coverImage || ''
  }))
)

const normalizedProducts = computed(() =>
  products.value.map(p => ({
    ...p,
    title: p.name || p.title || '',
    userId: p.sellerId || p.userId,
    userName: p.sellerName || p.userName || '',
    userAvatar: p.sellerAvatar || p.userAvatar || '',
    isLiked: p.isLiked || false,
    condition: p.condition || '',
    tags: p.tags || ''
  }))
)

const filterTabs = computed(() => [
  { key: 'all', label: '全部', count: result.value.postTotal + result.value.productTotal + result.value.userTotal },
  { key: 'post', label: '帖子', count: result.value.postTotal },
  { key: 'product', label: '商品', count: result.value.productTotal },
  { key: 'user', label: '用户', count: result.value.userTotal }
])

const isEmpty = computed(() => {
  return posts.value.length === 0 && products.value.length === 0 && users.value.length === 0
})

const hasMorePosts = computed(() => {
  return posts.value.length < result.value.postTotal
})

const hasMoreProducts = computed(() => {
  return products.value.length < result.value.productTotal
})

function loadHistory() {
  try {
    const raw = localStorage.getItem(HISTORY_KEY)
    searchHistory.value = raw ? JSON.parse(raw) : []
  } catch {
    searchHistory.value = []
  }
}

function saveHistory(kw) {
  const list = searchHistory.value.filter(item => item !== kw)
  list.unshift(kw)
  if (list.length > MAX_HISTORY) list.length = MAX_HISTORY
  searchHistory.value = list
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list))
}

function clearHistory() {
  searchHistory.value = []
  localStorage.removeItem(HISTORY_KEY)
}

function searchFromHistory(kw) {
  keyword.value = kw
  doSearch()
}

onMounted(() => {
  loadHistory()
  /* 同时支持 keyword 和 q 两种 URL 参数格式 */
  const queryKeyword = String(route.query.keyword || route.query.q || '')
  if (queryKeyword) {
    keyword.value = queryKeyword
    doSearch()
  } else {
    searchInput.value?.focus()
  }
})

/* 同时监听 keyword 和 q 参数变化 */
watch(() => route.query.keyword || route.query.q, (newKw) => {
  if (newKw && newKw !== searchKeyword.value) {
    keyword.value = String(newKw || '')
    doSearch()
  }
})

async function doSearch() {
  const kw = keyword.value.trim()
  if (!kw) return

  searchKeyword.value = kw
  hasSearched.value = true
  loading.value = true
  currentPage.value = 1
  activeType.value = 'all'
  saveHistory(kw)

  try {
    const params = { keyword: kw, page: 1, size: pageSize, sortBy: currentSort.value || undefined }
    const res = await searchApi.search(params)
    if (res.code === 200) {
      result.value = res.data
    }
  } catch (err) {
    console.error('搜索失败:', err)
  } finally {
    loading.value = false
  }
}

async function switchType(type) {
  if (activeType.value === type) return
  activeType.value = type
  currentSort.value = '' // 切换类型时重置排序
  currentPage.value = 1

  loading.value = true
  try {
    const params = { keyword: searchKeyword.value, page: 1, size: pageSize, type, sortBy: currentSort.value || undefined }
    const res = await searchApi.search(params)
    if (res.code === 200) {
      result.value = res.data
    }
  } catch (err) {
    console.error('搜索失败:', err)
  } finally {
    loading.value = false
  }
}

function clearKeyword() {
  keyword.value = ''
  searchInput.value?.focus()
}

async function changeSort(sortValue) {
  currentSort.value = sortValue
  loading.value = true
  try {
    const params = { keyword: searchKeyword.value, page: 1, size: pageSize, type: activeType.value === 'all' ? undefined : activeType.value, sortBy: sortValue || undefined }
    const res = await searchApi.search(params)
    if (res.code === 200) {
      result.value = res.data
    }
  } catch (err) {
    console.error('排序失败:', err)
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.back()
}

function goToUser(userId) {
  if (userId) router.push(`/users/${userId}`)
}

function handleLikeToggle(post, data) {
  post.isLiked = data.isLiked
  post.likeCount = data.count
}

function goToPost(post) {
  if (post.postType === 'ACTIVITY') {
    router.push(`/activities/${post.id}`)
  } else {
    router.push(`/community/posts/${post.id}`)
  }
}

function goToProduct(productId) {
  router.push(`/products/${productId}`)
}

function highlightText(text, kw) {
  if (!text || !kw) return text || ''
  const escaped = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}

function onAvatarError(e) {
  e.target.src = defaultAvatar
}
</script>

<style scoped>
.search-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f0fdf4);
}

.search-header {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky, 200);
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
  background: var(--color-bg-primary, #ffffff);
  box-shadow: var(--shadow-sm, 0 1px 3px rgba(0,0,0,0.06));
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  color: var(--color-text-primary, #111827);
  cursor: pointer;
  flex-shrink: 0;
  border-radius: var(--radius-full, 9999px);
  transition: background var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.back-btn:hover {
  background: var(--color-primary-50, #ecfdf5);
  color: var(--color-primary-600, #059669);
}

.back-btn:active {
  background: var(--color-primary-100, #d1fae5);
}

.search-input-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-1_5, 0.375rem);
  background: var(--color-gray-100, #f3f4f6);
  border: 2px solid transparent;
  border-radius: var(--radius-full, 9999px);
  padding: var(--space-2, 0.5rem) var(--space-3, 0.75rem);
  min-width: 0;
  transition: all var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.search-input-box.focused {
  background: var(--color-bg-primary, #ffffff);
  border-color: var(--color-primary-400, #34d399);
  box-shadow: 0 0 0 4px rgba(16, 185, 129, 0.15), var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
}

.search-icon {
  color: var(--color-text-tertiary, #9ca3af);
  flex-shrink: 0;
  transition: color var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.search-input-box.focused .search-icon {
  color: var(--color-primary-500, #10b981);
}

.search-input-box input {
  flex: 1;
  border: none;
  background: none;
  font-size: var(--text-base, 0.9375rem);
  color: var(--color-text-primary, #111827);
  outline: none;
  min-width: 0;
  font-family: var(--font-sans, inherit);
}

.search-input-box input::placeholder {
  color: var(--color-text-tertiary, #9ca3af);
}

.clear-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  background: var(--color-gray-300, #d1d5db);
  color: var(--color-text-inverse, #ffffff);
  border-radius: var(--radius-full, 9999px);
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
  transition: background var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.clear-btn:hover {
  background: var(--color-gray-400, #9ca3af);
}

.search-btn {
  flex-shrink: 0;
  padding: var(--space-2, 0.5rem) var(--space-4, 1rem);
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  color: var(--color-text-inverse, #ffffff);
  border: none;
  border-radius: var(--radius-full, 9999px);
  font-size: var(--text-sm, 0.8125rem);
  font-weight: var(--font-semibold, 600);
  cursor: pointer;
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
  transition: all var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.search-btn:hover {
  box-shadow: var(--shadow-green-lg, 0 8px 25px rgba(16,185,129,0.3));
  transform: translateY(-1px);
}

.search-btn:active {
  transform: translateY(0) scale(0.97);
}

.filter-tabs {
  background: var(--color-bg-primary, #ffffff);
  border-bottom: 1px solid var(--color-border-light, #e5e7eb);
  padding: 0 var(--space-3, 0.75rem);
}

.filter-tabs :deep(.el-tabs__header) {
  margin: 0;
}

.tab-count {
  font-size: var(--text-xs, 0.6875rem);
  padding: 1px var(--space-1_5, 0.375rem);
  border-radius: var(--radius-full, 9999px);
  background: var(--color-gray-100, #f3f4f6);
  color: var(--color-text-tertiary, #9ca3af);
  font-weight: var(--font-normal, 400);
  transition: all var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.sort-bar {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: var(--space-2, 0.5rem) var(--space-4, 1rem);
  background: var(--color-bg-primary, #ffffff);
  border-bottom: 1px solid var(--color-border-light, #e5e7eb);
}

.sort-label {
  font-size: 12px;
  color: var(--color-text-tertiary, #9ca3af);
  flex-shrink: 0;
}

.sort-btn {
  padding: 4px 12px;
  font-size: 12px;
  font-weight: var(--font-medium, 500);
  color: var(--color-text-secondary, #4b5563);
  background: var(--color-gray-50, #f9fafb);
  border: 1px solid var(--color-border-light, #e5e7eb);
  border-radius: var(--radius-full, 9999px);
  cursor: pointer;
  transition: all var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
  white-space: nowrap;
}

.sort-btn:hover:not(.active) {
  border-color: var(--color-primary-300, #6ee7b7);
  color: var(--color-primary-600, #059669);
}

.sort-btn.active {
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  color: var(--color-text-inverse, #ffffff);
  border-color: transparent;
  font-weight: var(--font-semibold, 600);
}

.search-body {
  padding: var(--space-3, 0.75rem) var(--space-4, 1rem);
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3, 0.75rem);
}

.skeleton-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-xl, 20px);
  overflow: hidden;
  box-shadow: var(--shadow-card, 0 2px 8px rgba(0,0,0,0.04));
}

.skeleton-image {
  width: 100%;
  aspect-ratio: 4 / 3;
}

.skeleton-body {
  padding: var(--space-3, 0.75rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.skeleton-title {
  height: 16px;
  width: 70%;
}

.skeleton-text {
  height: 12px;
  width: 100%;
}

.skeleton-text.short {
  width: 50%;
}

.skeleton {
  background: linear-gradient(
    90deg,
    var(--color-gray-100, #f3f4f6) 25%,
    var(--color-primary-50, #ecfdf5) 37%,
    var(--color-gray-200, #e5e7eb) 63%,
    var(--color-gray-100, #f3f4f6) 75%
  );
  background-size: 400% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
  border-radius: var(--radius-md, 10px);
}

@keyframes shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.initial-state {
  display: flex;
  flex-direction: column;
  gap: var(--space-6, 1.5rem);
}

.section-label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3, 0.75rem);
}

.section-label-text {
  font-size: var(--text-sm, 0.8125rem);
  font-weight: var(--font-semibold, 600);
  color: var(--color-text-primary, #111827);
}

.clear-history-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1, 0.25rem);
  font-size: var(--text-xs, 0.6875rem);
  color: var(--color-text-tertiary, #9ca3af);
  border: none;
  background: none;
  cursor: pointer;
  padding: var(--space-1, 0.25rem) var(--space-2, 0.5rem);
  border-radius: var(--radius-md, 10px);
  transition: all var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.clear-history-btn:hover {
  color: var(--color-primary-600, #059669);
  background: var(--color-primary-50, #ecfdf5);
}

.history-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2, 0.5rem);
}

.history-tag {
  padding: var(--space-1_5, 0.375rem) var(--space-3, 0.75rem);
  background: var(--color-gray-100, #f3f4f6);
  color: var(--color-text-secondary, #4b5563);
  border: none;
  border-radius: var(--radius-full, 9999px);
  font-size: var(--text-xs, 0.6875rem);
  cursor: pointer;
  transition: all var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.history-tag:hover {
  background: var(--color-primary-50, #ecfdf5);
  color: var(--color-primary-700, #047857);
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
}

.history-tag:active {
  transform: scale(0.95);
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2, 0.5rem);
}

.hot-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1_5, 0.375rem);
  padding: var(--space-1_5, 0.375rem) var(--space-3, 0.75rem);
  background: var(--color-gray-50, #f9fafb);
  color: var(--color-text-secondary, #4b5563);
  border: 1px solid var(--color-border-light, #e5e7eb);
  border-radius: var(--radius-full, 9999px);
  font-size: var(--text-xs, 0.6875rem);
  cursor: pointer;
  transition: all var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.hot-tag:hover {
  background: var(--color-primary-50, #ecfdf5);
  border-color: var(--color-primary-200, #a7f3d0);
  color: var(--color-primary-700, #047857);
}

.hot-tag--top {
  background: var(--color-primary-50, #ecfdf5);
  border-color: var(--color-primary-200, #a7f3d0);
  color: var(--color-primary-700, #047857);
  font-weight: var(--font-semibold, 600);
}

.hot-tag--top:hover {
  background: var(--color-primary-100, #d1fae5);
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
}

.hot-rank {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: var(--radius-xs, 4px);
  background: var(--color-gray-200, #e5e7eb);
  color: var(--color-text-tertiary, #9ca3af);
  font-size: 10px;
  font-weight: var(--font-bold, 700);
  line-height: 1;
  flex-shrink: 0;
}

.hot-rank--top {
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  color: var(--color-text-inverse, #ffffff);
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.3);
}

.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2, 0.5rem);
}

.tag-cloud-item {
  font-size: var(--text-sm, 0.8125rem);
  color: var(--color-primary-600, #059669);
  background: none;
  border: none;
  cursor: pointer;
  padding: var(--space-1, 0.25rem) 0;
  white-space: nowrap;
  transition: color var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.tag-cloud-item:hover {
  color: var(--color-primary-800, #065f46);
  text-decoration: underline;
}

.initial-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-10, 2.5rem) var(--space-4, 1rem);
  text-align: center;
  color: var(--color-primary-200, #a7f3d0);
}

.initial-text {
  font-size: var(--text-sm, 0.8125rem);
  color: var(--color-text-tertiary, #9ca3af);
  margin-top: var(--space-3, 0.75rem);
}

.empty-sub {
  margin: 0;
  font-size: var(--text-sm, 0.8125rem);
  color: var(--color-text-tertiary, #9ca3af);
}

.result-section {
  margin-bottom: var(--space-6, 1.5rem);
}

.section-header {
  display: flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  margin-bottom: var(--space-3, 0.75rem);
}

.section-title {
  font-size: var(--text-lg, 1.0625rem);
  font-weight: var(--font-bold, 700);
  color: var(--color-text-primary, #111827);
  margin: 0;
}

.section-count {
  font-size: var(--text-xs, 0.6875rem);
  color: var(--color-text-tertiary, #9ca3af);
}

.view-all-btn {
  margin-left: auto;
  font-size: var(--text-xs, 0.6875rem);
  color: var(--color-primary-600, #059669);
  border: none;
  background: none;
  cursor: pointer;
  font-weight: var(--font-semibold, 600);
  padding: var(--space-1, 0.25rem) var(--space-2, 0.5rem);
  border-radius: var(--radius-full, 9999px);
  transition: all var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.view-all-btn:hover {
  background: var(--color-primary-50, #ecfdf5);
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.user-item-inner {
  display: flex;
  align-items: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-3, 0.75rem);
  cursor: pointer;
}

.user-item .user-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-full, 9999px);
  object-fit: cover;
  background: var(--color-primary-50, #ecfdf5);
  flex-shrink: 0;
  border: 2px solid var(--color-primary-100, #d1fae5);
}

.user-info {
  flex: 1;
  min-width: 0;
}

.user-name-row {
  display: flex;
  align-items: center;
  gap: var(--space-1_5, 0.375rem);
  margin-bottom: var(--space-0_5, 0.125rem);
}

.user-nickname {
  font-size: var(--text-base, 0.9375rem);
  font-weight: var(--font-semibold, 600);
  color: var(--color-text-primary, #111827);
}

.match-badge {
  font-size: 10px;
  padding: 1px var(--space-1_5, 0.375rem);
  border-radius: var(--radius-sm, 6px);
  background: var(--color-primary-50, #ecfdf5);
  color: var(--color-primary-600, #059669);
  font-weight: var(--font-medium, 500);
}

.user-meta {
  display: flex;
  align-items: center;
  gap: var(--space-1_5, 0.375rem);
  font-size: var(--text-xs, 0.6875rem);
  color: var(--color-text-tertiary, #9ca3af);
  margin-bottom: var(--space-0_5, 0.125rem);
}

.user-meta span + span::before {
  content: '·';
  margin-right: var(--space-1_5, 0.375rem);
  color: var(--color-gray-300, #d1d5db);
}

.user-bio {
  font-size: var(--text-xs, 0.6875rem);
  color: var(--color-text-tertiary, #9ca3af);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 小红书风格瀑布流布局：使用 column-count 实现不等高卡片自然排列 */
.card-grid {
  column-count: 2;
  column-gap: 8px;
}

/* 瀑布流子项需要防止卡片在列中间断裂 */
.card-grid > * {
  break-inside: avoid;
  margin-bottom: 8px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--color-gray-200, #e5e7eb);
  border-top-color: var(--color-primary-500, #10b981);
  border-radius: var(--radius-full, 9999px);
  animation: spin 0.8s linear infinite;
  margin-bottom: var(--space-3, 0.75rem);
}

.loading-spinner.small {
  width: 18px;
  height: 18px;
  border-width: 2px;
  margin-bottom: 0;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2, 0.5rem);
  padding: var(--space-4, 1rem) 0;
  font-size: var(--text-sm, 0.8125rem);
  color: var(--color-text-tertiary, #9ca3af);
}

.no-more {
  text-align: center;
  padding: var(--space-4, 1rem) 0;
  font-size: var(--text-sm, 0.8125rem);
  color: var(--color-gray-300, #d1d5db);
}

:deep(.search-highlight) {
  background: #fef08a;
  color: inherit;
  padding: 0 2px;
  border-radius: var(--radius-xs, 4px);
  font-weight: var(--font-semibold, 600);
}

@media (min-width: 769px) {
  .search-page {
    max-width: var(--container-xl, 1200px);
    margin: 0 auto;
    box-shadow: var(--shadow-xl, 0 20px 25px -5px rgba(0,0,0,0.08));
    border-radius: var(--radius-2xl, 28px);
    overflow: hidden;
    margin-top: var(--space-4, 1rem);
    margin-bottom: var(--space-4, 1rem);
    min-height: calc(100vh - var(--space-8, 2rem));
  }

  .search-header {
    padding: var(--space-3, 0.75rem) var(--space-6, 1.5rem);
  }

  .search-body {
    padding: var(--space-4, 1rem) var(--space-6, 1.5rem);
  }

  .skeleton-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  /* 平板3列瀑布流 */
  .card-grid {
    column-count: 3;
    column-gap: 8px;
  }

  .history-tags,
  .hot-tags {
    gap: var(--space-2_5, 0.625rem);
  }

  .user-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-3, 0.75rem);
  }
}

@media (min-width: 1025px) {
  .search-header {
    padding: var(--space-4, 1rem) var(--space-8, 2rem);
  }

  .search-body {
    padding: var(--space-6, 1.5rem) var(--space-8, 2rem);
  }

  /* 桌面4列瀑布流 */
  .card-grid {
    column-count: 4;
    column-gap: 8px;
  }
}
</style>
