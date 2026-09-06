<template>
  <div class="products-page">
    <!-- 下拉刷新指示器 -->
    <div class="pull-refresh-indicator" :style="{ transform: `translateY(${pullDistance}px)`, transition: isRefreshing ? 'none' : 'transform 0.3s ease' }">
      <div v-if="pullDistance > 0 || isRefreshing" class="pull-indicator-inner">
        <div class="pull-spinner" :class="{ spinning: isRefreshing }"></div>
        <span class="pull-text">{{ isRefreshing ? '刷新中...' : canTrigger ? '松手刷新' : '下拉刷新' }}</span>
      </div>
    </div>
    <header class="mobile-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <div class="search-bar" :class="{ focused: searchFocused }">
        <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <circle cx="11" cy="11" r="8"/>
          <path d="M21 21l-4.35-4.35"/>
        </svg>
        <input
          type="text"
          v-model="searchKeyword"
          class="search-input"
          placeholder="搜索好物..."
          @keyup.enter="handleSearch"
          @focus="searchFocused = true"
          @blur="searchFocused = false"
        />
        <button v-if="searchKeyword" @click="searchKeyword = ''; handleSearch()" class="clear-btn">
          <svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16">
            <path d="M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z"/>
          </svg>
        </button>
      </div>
    </header>

    <section class="filter-section">
      <div class="filter-tabs">
        <button
          v-for="tab in filterTabs"
          :key="tab.key"
          @click="onFilterClick(tab.key)"
          :class="['filter-tab', { active: activeFilter === tab.key }]"
        >
          <svg v-if="tab.key === 'price'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
          <svg v-if="tab.key === 'time'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
          </svg>
          <svg v-if="tab.key === 'category'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
          </svg>
          {{ tab.label }}{{ tab.key === 'price' && activeFilter === 'price' ? (priceOrder === 'asc' ? '↑' : '↓') : '' }}
        </button>
      </div>
      <transition name="slide-down">
        <div v-if="showCategoryFilter" class="category-filter">
          <button
            v-for="cat in categories"
            :key="cat.id || 'all'"
            @click="selectCategory(cat.id)"
            :class="['category-chip', { active: selectedCategoryId === cat.id }]"
          >
            {{ cat.name }}
          </button>
        </div>
      </transition>
    </section>

    <main class="products-main">
      <div v-if="loading && products.length === 0" class="skeleton-grid">
        <div v-for="i in 8" :key="i" class="skeleton-card">
          <div class="skeleton-image"></div>
          <div class="skeleton-body">
            <div class="skeleton-line skeleton-line--title"></div>
            <div class="skeleton-line skeleton-line--title skeleton-line--short"></div>
            <div class="skeleton-row">
              <div class="skeleton-line skeleton-line--price"></div>
              <div class="skeleton-line skeleton-line--tag"></div>
            </div>
            <div class="skeleton-row">
              <div class="skeleton-line skeleton-line--avatar"></div>
              <div class="skeleton-line skeleton-line--name"></div>
            </div>
          </div>
        </div>
      </div>

      <div v-else-if="error && products.length === 0" class="error-state">
        <div class="error-illustration">
          <svg viewBox="0 0 120 120" width="120" height="120">
            <circle cx="60" cy="60" r="50" fill="var(--color-primary-50)" stroke="var(--color-primary-200)" stroke-width="2"/>
            <circle cx="45" cy="50" r="4" fill="var(--color-primary-300)"/>
            <circle cx="75" cy="50" r="4" fill="var(--color-primary-300)"/>
            <path d="M40 72 Q60 62 80 72" stroke="var(--color-primary-400)" stroke-width="3" fill="none" stroke-linecap="round"/>
          </svg>
        </div>
        <p class="error-title">加载遇到了问题</p>
        <p class="error-text">{{ error }}</p>
        <button @click="retryLoad" class="retry-btn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="16" height="16">
            <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
          </svg>
          重新加载
        </button>
      </div>

      <div v-else-if="!loading && products.length === 0" class="empty-state">
        <div class="empty-illustration">
          <svg viewBox="0 0 160 160" width="160" height="160">
            <rect x="30" y="40" width="100" height="80" rx="12" fill="var(--color-primary-50)" stroke="var(--color-primary-200)" stroke-width="2"/>
            <circle cx="65" cy="70" r="10" fill="var(--color-primary-100)" stroke="var(--color-primary-300)" stroke-width="1.5"/>
            <path d="M30 100 L65 75 L90 95 L110 80 L130 100" stroke="var(--color-primary-300)" stroke-width="2" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            <circle cx="120" cy="50" r="18" fill="var(--color-primary-100)" stroke="var(--color-primary-300)" stroke-width="1.5"/>
            <text x="120" y="55" text-anchor="middle" font-size="20" fill="var(--color-primary-400)">?</text>
          </svg>
        </div>
        <p class="empty-title">暂无相关商品</p>
        <p class="empty-text">换个关键词试试，或者浏览其他分类</p>
        <button @click="resetFilters" class="reset-btn">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="16" height="16">
            <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/><path d="M3 3v5h5"/>
          </svg>
          重置筛选
        </button>
      </div>

      <div v-else class="products-grid">
        <ProductCard
          v-for="product in products"
          :key="product.id"
          :product="product"
          @click="$router.push(`/products/${product.id}`)"
          @like-toggled="(data) => handleLikeToggle(product, data)"
        />
      </div>

      <div v-if="loadingMore" class="load-more">
        <div class="bounce-dots">
          <span class="dot"></span>
          <span class="dot"></span>
          <span class="dot"></span>
        </div>
        <span class="load-more-text">正在加载更多</span>
      </div>

      <div v-if="!hasMore && products.length > 0 && !loading" class="end-reached">
        <div class="end-line"></div>
        <span class="end-text">已经到底啦</span>
        <div class="end-line"></div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { productApi } from '../services/api'
import ProductCard from '../components/ProductCard.vue'
import { usePullRefresh } from '../use/usePullRefresh'

const route = useRoute()

const products = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const error = ref(null)

const searchKeyword = ref('')
const searchFocused = ref(false)
const activeFilter = ref('default')
const priceOrder = ref('asc') // 价格排序方向: asc/desc
const selectedCategoryId = ref(null)

const currentPage = ref(1)
const pageSize = 20

const showCategoryFilter = ref(false)

const filterTabs = [
  { key: 'default', label: '综合' },
  { key: 'price', label: '价格' },
  { key: 'time', label: '最新' },
  { key: 'category', label: '分类' }
]

const categories = [
  { id: null, name: '全部' },
  { id: 1, name: '数码电子' },
  { id: 2, name: '书籍教材' },
  { id: 3, name: '生活日用' },
  { id: 4, name: '服饰鞋包' },
  { id: 5, name: '美妆护肤' },
  { id: 6, name: '运动户外' }
]

onMounted(async () => {
  if (route.query.keyword) {
    searchKeyword.value = String(route.query.keyword || '')
  }
  if (route.query.categoryId) {
    selectedCategoryId.value = parseInt(String(route.query.categoryId || ''))
    activeFilter.value = 'category'
    showCategoryFilter.value = true
  }
  await loadProducts()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})

watch(activeFilter, (newVal) => {
  showCategoryFilter.value = newVal === 'category'
  if (newVal !== 'category') {
    showCategoryFilter.value = false
  }
  currentPage.value = 1
  loadProducts()
})

/* 点击筛选按钮：重复点击"价格"时切换升序/降序 */
function onFilterClick(key) {
  if (key === 'price' && activeFilter.value === 'price') {
    // 重复点击价格按钮：切换排序方向
    priceOrder.value = priceOrder.value === 'asc' ? 'desc' : 'asc'
    currentPage.value = 1
    loadProducts()
  } else {
    activeFilter.value = key
    if (key === 'price') {
      priceOrder.value = 'asc'
    }
  }
}

async function loadProducts(isLoadMore = false) {
  try {
    error.value = null
    if (isLoadMore) {
      loadingMore.value = true
    } else {
      loading.value = true
    }

    const params: Record<string, unknown> = {
      page: currentPage.value,
      size: pageSize,
      status: 1
    }

    if (searchKeyword.value.trim()) {
      params.keyword = searchKeyword.value.trim()
    }
    if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    }
    if (activeFilter.value === 'price') {
      params.sortBy = priceOrder.value === 'asc' ? 'price_asc' : 'price_desc'
    } else if (activeFilter.value === 'time') {
      params.sortBy = 'time_desc'
    }

    const response = await productApi.getProducts(params)

    if (response.code === 200) {
      const data = response.data || {}
      const newProducts = data.list || data.records || data.items || []

      if (isLoadMore) {
        products.value = [...products.value, ...newProducts]
      } else {
        products.value = newProducts
      }

      const total = data.total || 0
      hasMore.value = products.value.length < total
    } else {
      throw new Error(response.message || '加载商品失败')
    }
  } catch (err) {
    console.error('加载商品失败:', err)
    error.value = err.message || '加载失败，请稍后重试'
    if (isLoadMore) {
      currentPage.value--
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 下拉刷新：重新加载第一页数据
const { isRefreshing, pullDistance, canTrigger } = usePullRefresh(async () => {
  currentPage.value = 1
  hasMore.value = true
  await loadProducts()
})

function retryLoad() {
  currentPage.value = 1
  loadProducts()
}

function handleSearch() {
  currentPage.value = 1
  loadProducts()
}

function selectCategory(categoryId) {
  selectedCategoryId.value = categoryId
  currentPage.value = 1
  loadProducts()
}

function resetFilters() {
  searchKeyword.value = ''
  activeFilter.value = 'default'
  selectedCategoryId.value = null
  currentPage.value = 1
  loadProducts()
}

function handleLikeToggle(product, data) {
  const idx = products.value.findIndex(p => p.id === product.id)
  if (idx !== -1) {
    products.value[idx].isLiked = data.isLiked
    products.value[idx].likeCount = data.count
  }
}

function handleScroll() {
  if (loadingMore.value || !hasMore.value) return
  const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
  const scrollHeight = document.documentElement.scrollHeight
  const clientHeight = document.documentElement.clientHeight
  if (scrollTop + clientHeight >= scrollHeight - 150) {
    currentPage.value++
    loadProducts(true)
  }
}
</script>

<style scoped>
.products-page {
  min-height: 100vh;
  background-color: var(--color-bg-page);
  position: relative;
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

.mobile-header {
  display: none;
}

.filter-section {
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
  background: var(--gradient-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(16, 185, 129, 0.08);
}

.filter-tabs {
  display: flex;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-6);
  overflow-x: auto;
  scrollbar-width: none;
  max-width: 1200px;
  margin: 0 auto;
}

.filter-tabs::-webkit-scrollbar {
  display: none;
}

.filter-tab {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-5);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  background: var(--color-bg-primary);
  border-radius: var(--radius-full);
  white-space: nowrap;
  border: 1.5px solid var(--color-border-light);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
  font-family: var(--font-sans);
}

.filter-tab:hover:not(.active) {
  border-color: var(--color-primary-300);
  color: var(--color-primary-600);
  background: var(--color-primary-50);
}

.filter-tab.active {
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  border-color: transparent;
  box-shadow: var(--shadow-green);
  font-weight: var(--font-semibold);
}

.filter-tab.active svg {
  stroke: currentColor;
}

.category-filter {
  padding: var(--space-3) var(--space-6) var(--space-4);
  overflow-x: auto;
  scrollbar-width: none;
  display: flex;
  gap: var(--space-2_5);
  max-width: 1200px;
  margin: 0 auto;
}

.category-filter::-webkit-scrollbar {
  display: none;
}

.category-chip {
  padding: var(--space-1_5) var(--space-4);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  background: var(--color-bg-primary);
  border-radius: var(--radius-full);
  border: 1.5px solid var(--color-border-light);
  cursor: pointer;
  white-space: nowrap;
  transition: all var(--duration-normal) var(--ease-out);
  font-family: var(--font-sans);
}

.category-chip:hover:not(.active) {
  border-color: var(--color-primary-300);
  color: var(--color-primary-600);
  background: var(--color-primary-50);
}

.category-chip.active {
  background: var(--color-primary-50);
  color: var(--color-primary-700);
  border-color: var(--color-primary-400);
  font-weight: var(--font-semibold);
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.products-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: var(--space-6) var(--space-6) var(--space-12);
}

.products-grid {
  column-count: 4;
  column-gap: 8px;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.skeleton-card {
  background: var(--color-bg-primary);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.skeleton-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  background: linear-gradient(
    90deg,
    var(--color-primary-50) 25%,
    var(--color-primary-100) 37%,
    var(--color-primary-50) 63%
  );
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.skeleton-body {
  padding: var(--space-3) var(--space-3_5) var(--space-3);
}

.skeleton-line {
  border-radius: var(--radius-sm);
  background: linear-gradient(
    90deg,
    var(--color-gray-100) 25%,
    var(--color-gray-200) 37%,
    var(--color-gray-100) 63%
  );
  background-size: 400% 100%;
  animation: shimmer 1.4s ease infinite;
}

.skeleton-line--title {
  height: 14px;
  width: 100%;
  margin-bottom: var(--space-1_5);
}

.skeleton-line--short {
  width: 65%;
}

.skeleton-line--price {
  height: 20px;
  width: 60px;
}

.skeleton-line--tag {
  height: 18px;
  width: 40px;
  border-radius: var(--radius-full);
}

.skeleton-line--avatar {
  height: 20px;
  width: 20px;
  border-radius: 50%;
}

.skeleton-line--name {
  height: 12px;
  width: 50px;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-2);
}

@keyframes shimmer {
  0% { background-position: 100% 50%; }
  100% { background-position: 0 50%; }
}

.error-state {
  text-align: center;
  padding: var(--space-20) var(--space-8);
  animation: fadeIn var(--duration-slow) var(--ease-out) both;
}

.error-illustration {
  margin-bottom: var(--space-6);
}

.error-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2);
}

.error-text {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--space-8);
}

.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-8);
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  border: none;
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
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
  transform: translateY(0) scale(0.97);
}

.empty-state {
  text-align: center;
  padding: var(--space-20) var(--space-8);
  animation: fadeIn var(--duration-slow) var(--ease-out) both;
}

.empty-illustration {
  margin-bottom: var(--space-6);
}

.empty-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2);
}

.empty-text {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--space-8);
}

.reset-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-8);
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  border: none;
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
  box-shadow: var(--shadow-green);
  transition: all var(--duration-normal) var(--ease-out);
  font-family: var(--font-sans);
}

.reset-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-green-lg);
}

.reset-btn:active {
  transform: translateY(0) scale(0.97);
}

.load-more {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-8) 0;
}

.bounce-dots {
  display: flex;
  gap: var(--space-2);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary-400);
  animation: bounce-dot 1.4s ease-in-out infinite;
}

.dot:nth-child(1) {
  animation-delay: 0s;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes bounce-dot {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.load-more-text {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.end-reached {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-10) 0 var(--space-6);
}

.end-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent,
    var(--color-primary-200),
    transparent
  );
}

.end-text {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
  white-space: nowrap;
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all var(--duration-normal) var(--ease-out);
  overflow: hidden;
}

.slide-down-enter-from,
.slide-down-leave-to {
  max-height: 0;
  opacity: 0;
  padding-top: 0;
  padding-bottom: 0;
}

.slide-down-enter-to,
.slide-down-leave-from {
  max-height: 80px;
  opacity: 1;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1024px) {
  .products-grid {
    column-count: 3;
    column-gap: 8px;
  }

  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
    gap: 16px;
  }

  .products-main {
    padding: var(--space-4) var(--space-4) var(--space-10);
  }

  .filter-tabs {
    padding: var(--space-3) var(--space-4);
  }

  .category-filter {
    padding: var(--space-3) var(--space-4) var(--space-3);
  }
}

@media (max-width: 768px) {
  .mobile-header {
    display: flex;
    align-items: center;
    gap: var(--space-2_5);
    padding: var(--space-2_5) var(--space-3);
    position: sticky;
    top: 0;
    z-index: var(--z-sticky);
    background: var(--gradient-glass);
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-bottom: 1px solid rgba(16, 185, 129, 0.08);
  }

  .back-btn {
    width: 36px;
    height: 36px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-text-primary);
    border-radius: 50%;
    flex-shrink: 0;
    background: none;
    border: none;
    cursor: pointer;
    transition: background var(--duration-fast) var(--ease-out);
  }

  .back-btn svg {
    width: 22px;
    height: 22px;
  }

  .back-btn:active {
    background: var(--color-gray-100);
  }

  .search-bar {
    flex: 1;
    display: flex;
    align-items: center;
    gap: var(--space-2);
    background: var(--color-bg-tertiary);
    border-radius: var(--radius-full);
    padding: var(--space-2) var(--space-3_5);
    border: 1.5px solid transparent;
    transition: all var(--duration-normal) var(--ease-out);
  }

  .search-bar.focused {
    border-color: var(--color-primary-400);
    background: var(--color-bg-primary);
    box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
  }

  .search-icon {
    width: 18px;
    height: 18px;
    color: var(--color-text-tertiary);
    flex-shrink: 0;
    transition: color var(--duration-fast) var(--ease-out);
  }

  .search-bar.focused .search-icon {
    color: var(--color-primary-500);
  }

  .search-input {
    flex: 1;
    border: none;
    background: none;
    font-size: var(--text-sm);
    color: var(--color-text-primary);
    outline: none;
    font-family: var(--font-sans);
  }

  .search-input::placeholder {
    color: var(--color-text-tertiary);
  }

  .clear-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 20px;
    height: 20px;
    background: var(--color-gray-300);
    border: none;
    border-radius: 50%;
    color: white;
    cursor: pointer;
    flex-shrink: 0;
    padding: 0;
    transition: background var(--duration-fast) var(--ease-out);
  }

  .clear-btn:active {
    background: var(--color-gray-400);
  }

  .filter-section {
    top: 0;
  }

  .filter-tabs {
    padding: var(--space-2_5) var(--space-3);
    gap: var(--space-1_5);
  }

  .filter-tab {
    padding: var(--space-1_5) var(--space-3_5);
    font-size: var(--text-xs);
  }

  .category-filter {
    padding: var(--space-2) var(--space-3) var(--space-3);
    gap: var(--space-2);
  }

  .category-chip {
    padding: var(--space-1) var(--space-3);
    font-size: 11px;
  }

  .products-grid {
    column-count: 2;
    column-gap: 8px;
  }

  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .products-main {
    padding: var(--space-3) var(--space-3) var(--space-10);
  }

  .error-state {
    padding: var(--space-12) var(--space-6);
  }

  .empty-state {
    padding: var(--space-12) var(--space-6);
  }

  .error-illustration svg,
  .empty-illustration svg {
    width: 100px;
    height: 100px;
  }
}
</style>
