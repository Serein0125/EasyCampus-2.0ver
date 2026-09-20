<template>
  <div class="products-page">
    <section class="filter-section">
      <div class="filter-tabs">
        <button
          v-for="tab in otherFilterTabs"
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
          {{ tab.label }}{{ tab.key === 'price' && activeFilter === 'price' ? (priceOrder === 'asc' ? '↑' : '↓') : '' }}
        </button>

        <div
          class="category-tab-wrap"
          @mouseenter="openCategoryDropdown()"
          @mouseleave="scheduleCloseCategoryDropdown()"
        >
          <button
            class="filter-tab"
            :class="{ active: activeFilter === 'category' }"
            @click="onFilterClick('category')"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>
            </svg>
            分类
            <svg
              :class="['caret-icon', { open: showCategoryDropdown }]"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              width="12"
              height="12"
            >
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </button>

          <transition name="dropdown">
            <div v-if="showCategoryDropdown" class="category-dropdown">
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
        </div>
      </div>
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
        <el-empty description="暂无相关商品">
          <p class="empty-text">换个关键词试试，或者浏览其他分类</p>
          <el-button type="primary" round @click="resetFilters">重置筛选</el-button>
        </el-empty>
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

const route = useRoute()

const products = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const error = ref(null)

const searchKeyword = ref('')
const activeFilter = ref('default')
const priceOrder = ref('asc') // 价格排序方向: asc/desc
const selectedCategoryId = ref(null)

const currentPage = ref(1)
const pageSize = 20

const showCategoryDropdown = ref(false)
let categoryCloseTimer: ReturnType<typeof setTimeout> | null = null

const filterTabs = [
  { key: 'default', label: '综合' },
  { key: 'price', label: '价格' },
  { key: 'time', label: '最新' },
  { key: 'category', label: '分类' }
]

// 分类 Tab 单独渲染（需要包裹悬停下拉面板），其余 Tab 用 v-for 循环
const otherFilterTabs = filterTabs.filter(t => t.key !== 'category')

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
  }
  await loadProducts()
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  if (categoryCloseTimer) {
    clearTimeout(categoryCloseTimer)
    categoryCloseTimer = null
  }
})

watch(activeFilter, (newVal) => {
  currentPage.value = 1
  loadProducts()
})

/* 分类下拉：鼠标进入分类按钮/下拉面板时打开 */
function openCategoryDropdown() {
  if (categoryCloseTimer) {
    clearTimeout(categoryCloseTimer)
    categoryCloseTimer = null
  }
  showCategoryDropdown.value = true
}

/* 分类下拉：鼠标移出时延迟关闭，给移动到面板留出时间 */
function scheduleCloseCategoryDropdown() {
  if (categoryCloseTimer) {
    clearTimeout(categoryCloseTimer)
  }
  categoryCloseTimer = setTimeout(() => {
    showCategoryDropdown.value = false
    categoryCloseTimer = null
  }, 150)
}

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
    // 分类按钮：键盘与触摸用户没有 hover，点击时直接把面板展开
    if (key === 'category') {
      openCategoryDropdown()
    } else {
      showCategoryDropdown.value = false
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

// 下拉刷新已移除（H5 移动端特化）
function retryLoad() {
  currentPage.value = 1
  loadProducts()
}

function selectCategory(categoryId) {
  selectedCategoryId.value = categoryId
  showCategoryDropdown.value = false
  if (activeFilter.value !== 'category') {
    activeFilter.value = 'category' // 触发 watch 重新加载并高亮分类 Tab
  } else {
    currentPage.value = 1
    loadProducts()
  }
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
  overflow-x: visible;
  scrollbar-width: none;
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
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

.caret-icon {
  transition: transform var(--duration-normal) var(--ease-out);
}

.caret-icon.open {
  transform: rotate(180deg);
}

.category-tab-wrap {
  position: relative;
}

.category-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2_5) var(--space-3);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  z-index: var(--z-dropdown);
  white-space: nowrap;
  max-width: min(620px, calc(100vw - 32px));
  overflow-x: auto;
  scrollbar-width: thin;
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
  padding: var(--space-3) var(--space-4) var(--space-3);
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
  padding: var(--space-8) var(--space-4);
  animation: fadeIn var(--duration-slow) var(--ease-out) both;
}

.empty-text {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin: 0 0 var(--space-4);
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

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all var(--duration-normal) var(--ease-out);
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-6px);
}

.dropdown-enter-to,
.dropdown-leave-from {
  opacity: 1;
  transform: translateX(-50%) translateY(0);
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
}

@media (max-width: 768px) {
  .filter-section {
    top: 0;
  }

  .filter-tabs {
    overflow-x: auto;
    padding: var(--space-2_5) var(--space-3);
    gap: var(--space-1_5);
  }

  .filter-tab {
    padding: var(--space-1_5) var(--space-4);
    font-size: var(--text-xs);
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
    padding: var(--space-8) var(--space-4);
  }

  .error-illustration svg {
    width: 100px;
    height: 100px;
  }
}
</style>
