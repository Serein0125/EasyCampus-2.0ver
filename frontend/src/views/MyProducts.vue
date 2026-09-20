<template>
  <div class="my-products-page">
    <header class="page-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h1 class="header-title">商品与帖子</h1>
    </header>

    <div class="tab-bar">
      <el-tabs v-model="activeTab" @tab-change="switchTab">
        <el-tab-pane name="products">
          <template #label>
            商品
            <span v-if="products.length > 0" class="tab-count">{{ products.length }}</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="posts">
          <template #label>
            帖子
            <span v-if="posts.length > 0" class="tab-count">{{ posts.length }}</span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <main class="main-content">
      <div v-if="loading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="error" class="error-state">
        <div class="error-icon">⚠️</div>
        <p>{{ error }}</p>
        <button @click="loadData" class="retry-btn">重试</button>
      </div>

      <template v-else>
        <div v-if="activeTab === 'products'">
          <el-empty v-if="products.length === 0" description="还没有发布商品">
            <p class="empty-sub">快去发布你的闲置物品吧</p>
            <router-link to="/products/create">
              <el-button type="primary" round>发布商品</el-button>
            </router-link>
          </el-empty>

          <div v-else class="item-list">
            <el-card
              v-for="product in products"
              :key="'p-' + product.id"
              shadow="hover"
              :body-style="{ padding: '0px' }"
              @click="goToProductDetail(product.id)"
            >
              <div class="product-card">
                <div class="product-image">
                  <img
                    v-if="getProductImage(product)"
                    :src="getProductImage(product)"
                    alt=""
                    loading="lazy"
                  />
                  <div v-else class="image-placeholder">
                    <span>{{ getCategoryEmoji(product.categoryId) }}</span>
                  </div>
                  <div class="status-badge" :class="getStatusClass(product.status)">
                    {{ getStatusText(product.status) }}
                  </div>
                </div>
                <div class="product-info">
                  <h3 class="product-name">{{ product.name }}</h3>
                  <div class="product-meta">
                    <span class="product-price">¥{{ product.price }}</span>
                    <span class="product-condition">{{ product.conditionText }}</span>
                  </div>
                  <div class="product-stats">
                    <span>👁 {{ product.viewCount || 0 }}</span>
                    <span>❤️ {{ product.likeCount || 0 }}</span>
                    <span class="product-time">{{ formatTime(product.createdAt) }}</span>
                  </div>
                </div>
                <div class="item-actions" @click.stop>
                  <button
                    v-if="product.status === 1"
                    @click="toggleProductStatus(product.id, 0)"
                    class="action-btn off-btn"
                  >下架</button>
                  <button
                    v-if="product.status === 0"
                    @click="toggleProductStatus(product.id, 1)"
                    class="action-btn on-btn"
                  >上架</button>
                  <button
                    @click="handleDeleteProduct(product)"
                    class="action-btn delete-btn"
                  >删除</button>
                </div>
              </div>
            </el-card>
          </div>
        </div>

        <div v-if="activeTab === 'posts'">
          <el-empty v-if="posts.length === 0" description="还没有发布帖子">
            <p class="empty-sub">分享你的校园生活动态</p>
            <router-link to="/community/posts/create">
              <el-button type="primary" round>发布帖子</el-button>
            </router-link>
          </el-empty>

          <div v-else class="item-list">
            <el-card
              v-for="post in posts"
              :key="'post-' + post.id"
              shadow="hover"
              :body-style="{ padding: '0px' }"
              @click="goToPostDetail(post.id)"
            >
              <div class="post-card">
                <div class="post-cover" v-if="getPostCover(post)">
                  <img :src="getPostCover(post)" alt="" loading="lazy" />
                </div>
                <div class="post-info">
                  <div class="post-type-badge" v-if="post.postType === 'ACTIVITY'">活动</div>
                  <h3 class="post-title">{{ post.title }}</h3>
                  <p class="post-summary" v-if="post.summary || post.content">{{ getPostSummary(post) }}</p>
                  <div class="post-tags" v-if="getPostTags(post).length">
                    <span v-for="tag in getPostTags(post)" :key="tag" class="tag-hashtag">{{ tag }}</span>
                  </div>
                  <div class="post-stats">
                    <span>👁 {{ post.viewCount || 0 }}</span>
                    <span>❤️ {{ post.likeCount || 0 }}</span>
                    <span>💬 {{ post.commentCount || 0 }}</span>
                    <span class="post-time">{{ formatTime(post.createdAt) }}</span>
                  </div>
                </div>
                <div class="item-actions" @click.stop>
                  <button
                    @click="handleDeletePost(post)"
                    class="action-btn delete-btn"
                  >删除</button>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </template>
    </main>

    <Teleport to="body">
      <div v-if="deleteDialog.visible" class="delete-modal-overlay" @click="cancelDelete">
        <div class="delete-modal" @click.stop>
          <div class="modal-icon">⚠️</div>
          <h3 class="modal-title">确定要删除吗？</h3>
          <p class="modal-desc">{{ deleteDialog.message }}</p>
          <div class="modal-actions">
            <button @click="cancelDelete" class="modal-btn cancel-btn">取消</button>
            <button @click="confirmDelete" class="modal-btn confirm-delete-btn" :disabled="deleteDialog.loading">
              {{ deleteDialog.loading ? '删除中...' : '确定删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { productApi, postApi } from '../services/api'
import { useToast } from '../use/useToast'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const toast = useToast()

const activeTab = ref('products')
const products = ref([])
const posts = ref([])
const loading = ref(true)
const error = ref('')

const deleteDialog = reactive({
  visible: false,
  type: '',
  id: null,
  message: '',
  loading: false
})

function getProductImage(product) {
  if (product.coverImage) return product.coverImage
  if (product.imageUrls) {
    try {
      const urls = typeof product.imageUrls === 'string' ? JSON.parse(product.imageUrls) : product.imageUrls
      if (Array.isArray(urls) && urls.length > 0) return urls[0]
    } catch {}
  }
  return null
}

function getCategoryEmoji(categoryId) {
  const map = { 1: '📱', 2: '📚', 3: '🏠', 4: '👕', 5: '💄', 6: '⚽' }
  return map[categoryId] || '📦'
}

function getStatusText(status) {
  const map = { 0: '已下架', 1: '在售', 2: '已售出', 3: '预约中' }
  return map[status] || '未知'
}

function getStatusClass(status) {
  const map = { 0: 'status-off', 1: 'status-on', 2: 'status-sold', 3: 'status-reserved' }
  return map[status] || ''
}

function getPostCover(post) {
  if (post.coverImage) return post.coverImage
  if (post.images) {
    try {
      const imgs = typeof post.images === 'string' ? JSON.parse(post.images) : post.images
      if (Array.isArray(imgs) && imgs.length > 0) return imgs[0]
    } catch {}
  }
  return null
}

function getPostSummary(post) {
  if (post.summary) return post.summary
  if (post.content) {
    const text = post.content.replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
    return text.length > 60 ? text.substring(0, 60) + '...' : text
  }
  return ''
}

function getPostTags(post) {
  if (!post.tags) return []
  return post.tags.split(',').filter(t => t.trim())
}

function formatTime(timestamp) {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  return date.toLocaleDateString('zh-CN')
}

function switchTab(tab) {
  activeTab.value = tab
}

function goToProductDetail(id) {
  router.push(`/products/${id}`)
}

function goToPostDetail(id) {
  router.push(`/community/posts/${id}`)
}

async function toggleProductStatus(productId, status) {
  try {
    const res = await productApi.toggleProductStatus(productId, status)
    if (res.code === 200) {
      const product = products.value.find(p => p.id === productId)
      if (product) product.status = status
      toast.showToast(status === 1 ? '已上架' : '已下架', 'success')
    } else {
      toast.showToast(res.message || '操作失败', 'error')
    }
  } catch (e) {
    toast.showToast(e.message || '操作失败', 'error')
  }
}

function handleDeleteProduct(product) {
  deleteDialog.type = 'product'
  deleteDialog.id = product.id
  deleteDialog.message = `确定要删除商品「${product.name}」吗？删除后不可恢复。`
  deleteDialog.loading = false
  deleteDialog.visible = true
}

function handleDeletePost(post) {
  deleteDialog.type = 'post'
  deleteDialog.id = post.id
  deleteDialog.message = `确定要删除帖子「${post.title}」吗？删除后不可恢复。`
  deleteDialog.loading = false
  deleteDialog.visible = true
}

function cancelDelete() {
  if (deleteDialog.loading) return
  deleteDialog.visible = false
}

async function confirmDelete() {
  if (deleteDialog.loading) return
  deleteDialog.loading = true

  try {
    if (deleteDialog.type === 'product') {
      const res = await productApi.deleteProduct(deleteDialog.id)
      if (res.code === 200) {
        products.value = products.value.filter(p => p.id !== deleteDialog.id)
        toast.showToast('商品已删除', 'success')
      } else {
        toast.showToast(res.message || '删除失败', 'error')
      }
    } else if (deleteDialog.type === 'post') {
      const res = await postApi.deletePost(deleteDialog.id)
      if (res.code === 200) {
        posts.value = posts.value.filter(p => p.id !== deleteDialog.id)
        toast.showToast('帖子已删除', 'success')
      } else {
        toast.showToast(res.message || '删除失败', 'error')
      }
    }
  } catch (e) {
    toast.showToast(e.message || '删除失败，请稍后重试', 'error')
  } finally {
    deleteDialog.visible = false
    deleteDialog.loading = false
  }
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const results = await Promise.allSettled([
      productApi.getMyProducts(),
      loadMyPosts()
    ])

    const productsRejected = results[0].status === 'rejected'
    const productsReason = productsRejected
      ? (results[0] as PromiseRejectedResult).reason
      : null

    // 401：登录态已过期（响应拦截器已清空本地 token/user），引导重新登录，
    // 不能再显示成「检查网络」——否则点重试仍是 401，陷入死循环
    if (productsReason?.status === 401) {
      error.value = '登录已过期，请重新登录'
      toast.showToast('登录已过期，请重新登录', 'warning')
      setTimeout(() => {
        router.replace({ path: '/login', query: { redirect: route.fullPath } })
      }, 800)
      return
    }

    if (results[0].status === 'fulfilled' && results[0].value.code === 200) {
      products.value = results[0].value.data || []
    } else if (productsRejected) {
      console.error('加载商品失败:', productsReason)
    }

    if (results[1].status === 'rejected') {
      console.error('加载帖子失败:', results[1].reason)
    }

    if (products.value.length === 0 && posts.value.length === 0) {
      if (productsRejected || (results[0].status === 'fulfilled' && results[0].value.code !== 200)) {
        error.value = '加载失败，请稍后重试'
      }
    }
  } catch (e) {
    console.error('加载数据失败:', e)
    error.value = '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

async function loadMyPosts() {
  const userId = authStore.currentUser.value?.id
  if (!userId) {
    const storedUser = localStorage.getItem('user')
    if (storedUser) {
      try {
        const parsed = JSON.parse(storedUser)
        if (parsed.id) {
          const res = await postApi.getUserPosts(parsed.id)
          if (res.code === 200) {
            posts.value = res.data || []
          }
        }
      } catch (e) {
        console.error('加载我的帖子失败:', e)
      }
    }
    return
  }
  const res = await postApi.getUserPosts(userId)
  if (res.code === 200) {
    posts.value = res.data || []
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-products-page {
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

.tab-bar {
  display: flex;
  background: #fff;
  padding: 0 16px;
}

.tab-bar :deep(.el-tabs__header) {
  margin: 0;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  margin-left: 4px;
  background: #f0f0f0;
  color: #999;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
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

@keyframes spin { to { transform: rotate(360deg); } }

.error-state {
  text-align: center;
  padding: 60px 0;
}

.error-icon { font-size: 48px; margin-bottom: 12px; }
.error-state p { color: #666; margin-bottom: 16px; }

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

.empty-sub {
  margin: 0 0 12px;
  font-size: 14px;
  color: #999;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.product-card {
  display: flex;
  gap: 14px;
  padding: 14px;
  cursor: pointer;
}

.product-card:active {
  transform: scale(0.98);
}

.product-image {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f5f5f5;
}

.product-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  background: #f8f8f8;
}

.status-badge {
  position: absolute;
  top: 6px;
  left: 6px;
  padding: 2px 8px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  color: white;
}

.status-on { background-color: #52c41a; }
.status-off { background-color: #999; }
.status-sold { background-color: #1890ff; }
.status-reserved { background-color: #faad14; }

.product-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.product-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.product-price {
  font-size: 17px;
  font-weight: 700;
  color: #FF4D4F;
}

.product-condition {
  font-size: 12px;
  color: #999;
  padding: 2px 6px;
  background: #f5f5f5;
  border-radius: 4px;
}

.product-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #bbb;
}

.product-time {
  margin-left: auto;
}

.post-card {
  display: flex;
  gap: 14px;
  padding: 14px;
  cursor: pointer;
}

.post-card:active {
  transform: scale(0.98);
}

.post-cover {
  width: 100px;
  height: 100px;
  border-radius: 10px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f5f5f5;
}

.post-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.post-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 6px;
}

.post-type-badge {
  display: inline-block;
  padding: 2px 8px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 4px;
  align-self: flex-start;
}

.post-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-summary {
  font-size: 13px;
  color: #999;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.5;
}

.post-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-hashtag {
  font-size: 11px;
  color: var(--color-primary-600, #059669);
  white-space: nowrap;
  cursor: pointer;
}

.post-stats {
  display: flex;
  gap: 12px;
  font-size: 12px;
  color: #bbb;
}

.post-time {
  margin-left: auto;
}

.item-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  flex-shrink: 0;
}

.action-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
}

.off-btn {
  background-color: #f5f5f5;
  color: #666;
}

.on-btn {
  background-color: #FFF7E6;
  color: #FA8C16;
}

.delete-btn {
  background-color: #FFF1F0;
  color: #FF4D4F;
}

.delete-btn:active {
  background-color: #FFCCC7;
}

.delete-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.delete-modal {
  background-color: #fff;
  border-radius: 16px;
  padding: 32px 24px;
  max-width: 320px;
  width: 100%;
  text-align: center;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.25s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px 0;
}

.modal-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 28px 0;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-btn {
  flex: 1;
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  border: none;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
}

.cancel-btn:active {
  background-color: #e8e8e8;
  transform: scale(0.98);
}

.confirm-delete-btn {
  background-color: #FF4D4F;
  color: white;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.25);
}

.confirm-delete-btn:active {
  background-color: #cf1322;
  transform: scale(0.98);
}

.confirm-delete-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

@media (min-width: 769px) {
  .my-products-page {
    max-width: 750px;
    margin: 0 auto;
    box-shadow: 0 0 20px rgba(0, 0, 0, 0.08);
    background: #fff;
  }
}
</style>
