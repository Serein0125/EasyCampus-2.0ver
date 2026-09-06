<template>
  <div class="categories-page">
    <header class="page-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h1 class="header-title">分类浏览</h1>
    </header>

    <main class="main-content">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="error" class="error-state">
        <div class="error-icon">⚠️</div>
        <p>{{ error }}</p>
        <button @click="loadCategories" class="retry-btn">重试</button>
      </div>

      <div v-else-if="categories.length === 0" class="empty-state">
        <div class="empty-icon">📂</div>
        <h3>暂无分类</h3>
        <p>还没有创建任何商品分类</p>
      </div>

      <div v-else>
        <div class="stats-bar">
          <div class="stat-item">
            <span class="stat-value">{{ categories.length }}</span>
            <span class="stat-label">个分类</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ totalProducts }}</span>
            <span class="stat-label">件商品</span>
          </div>
        </div>

        <div class="category-list">
          <div
            v-for="category in categoriesWithCount"
            :key="category.id"
            class="category-card"
            @click="goToCategory(category.id)"
          >
            <div class="category-icon">{{ getCategoryIcon(category) }}</div>
            <div class="category-info">
              <div class="category-name">{{ category.name }}</div>
              <div class="category-desc">{{ category.description || '暂无描述' }}</div>
            </div>
            <div class="category-count">
              <span class="count-num">{{ category.productCount || 0 }}</span>
              <span class="count-label">件</span>
            </div>
            <svg class="arrow-right" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="2">
              <polyline points="9,18 15,12 9,6"/>
            </svg>
          </div>
        </div>

        <div v-if="categoryTree.length > 0" class="sub-categories-section">
          <h3 class="section-title">子分类</h3>
          <div
            v-for="parent in categoryTree"
            :key="parent.id"
            class="parent-group"
          >
            <div v-if="parent.children && parent.children.length > 0" class="sub-list">
              <div
                v-for="child in parent.children"
                :key="child.id"
                class="sub-category-card"
                @click="goToCategory(child.id)"
              >
                <span class="sub-icon">{{ getCategoryIcon(child) }}</span>
                <span class="sub-name">{{ child.name }}</span>
                <span class="sub-count">{{ getProductCountForCategory(child.id) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { categoryApi, productApi } from '../services/api'

const router = useRouter()

const categories = ref([])
const categoryTree = ref([])
const categoryProductCounts = ref<Record<string | number, number>>({})
const loading = ref(true)
const error = ref('')

const categoriesWithCount = computed(() => {
  return categories.value.map(cat => ({
    ...cat,
    productCount: categoryProductCounts.value[cat.id] || 0
  }))
})

const totalProducts = computed(() => {
  return Object.values(categoryProductCounts.value).reduce((sum, count) => sum + count, 0)
})

function getProductCountForCategory(categoryId) {
  return categoryProductCounts.value[categoryId] || 0
}

function getCategoryIcon(category) {
  const iconMap = {
    '电子产品': '📱', '手机': '📱', '电脑': '💻', '平板': '📱',
    '书籍教材': '📚', '教科书': '📖', '小说': '📕', '参考书': '📗',
    '生活用品': '🏠', '生活日用': '🏠', '家居': '🏠',
    '服装鞋帽': '👕', '服饰鞋包': '👕', '衣服': '👗', '鞋子': '👟',
    '美妆护肤': '💄', '化妆品': '💄',
    '运动户外': '⚽', '运动器材': '⚽', '运动': '🏃',
    '其他': '📦', '食品': '🍜', '票券': '🎫'
  }
  return iconMap[category.name] || '📂'
}

function goToCategory(categoryId) {
  router.push({ path: '/products', query: { categoryId } })
}

async function loadCategories() {
  loading.value = true
  error.value = ''
  try {
    const [listRes, treeRes] = await Promise.all([
      categoryApi.getCategories(),
      categoryApi.getCategoryTree()
    ])

    if (listRes.code === 200) {
      categories.value = listRes.data || []
    }

    if (treeRes.code === 200) {
      categoryTree.value = treeRes.data || []
    }

    const countPromises = categories.value.map(async (cat) => {
      try {
        const res = await productApi.getProducts({ categoryId: cat.id, page: 1, size: 1 })
        if (res.code === 200) {
          const data = res.data || {}
          categoryProductCounts.value[cat.id] = data.total || 0
        }
      } catch {
        categoryProductCounts.value[cat.id] = 0
      }
    })
    await Promise.all(countPromises)
  } catch (e) {
    console.error('加载分类失败:', e)
    error.value = '加载分类失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.categories-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  border-radius: 50%;
}

.back-btn svg { width: 22px; height: 22px; }

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
}

.main-content {
  padding: 12px 16px 80px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 0;
  color: #999;
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid #f0f0f0;
  border-top-color: var(--color-primary-500, #10b981);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-state {
  text-align: center;
  padding: 60px 0;
}

.error-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.error-state p {
  color: #666;
  margin-bottom: 16px;
}

.retry-btn {
  padding: 10px 24px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-state h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 8px;
}

.empty-state p {
  color: #999;
}

.stats-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 24px;
  background: #fff;
  padding: 16px;
  border-radius: 12px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.stat-item {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: var(--color-primary-500, #10b981);
}

.stat-label {
  font-size: 13px;
  color: #999;
}

.stat-divider {
  width: 1px;
  height: 24px;
  background-color: #eee;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-card:active {
  transform: scale(0.98);
  background-color: #fafafa;
}

.category-icon {
  font-size: 32px;
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f8f8;
  border-radius: 12px;
  flex-shrink: 0;
}

.category-info {
  flex: 1;
  min-width: 0;
}

.category-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}

.category-desc {
  font-size: 13px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.category-count {
  display: flex;
  align-items: baseline;
  gap: 2px;
  flex-shrink: 0;
}

.count-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary-500, #10b981);
}

.count-label {
  font-size: 12px;
  color: #999;
}

.arrow-right {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.sub-categories-section {
  margin-top: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 12px;
}

.parent-group {
  margin-bottom: 12px;
}

.sub-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.sub-category-card {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #fff;
  border-radius: 20px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.sub-category-card:active {
  background-color: #FFF7E6;
}

.sub-icon {
  font-size: 16px;
}

.sub-name {
  font-weight: 500;
}

.sub-count {
  font-size: 12px;
  color: var(--color-primary-500, #10b981);
  font-weight: 600;
}
</style>
