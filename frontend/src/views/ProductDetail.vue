<template>
  <div class="detail-page">
    <header class="detail-nav">
      <div class="nav-left">
        <BackButton />
        <img :src="product?.sellerAvatar || defaultAvatar" class="nav-avatar" @click="goToSeller" @error="onAvatarError" />
        <span class="nav-username" @click="goToSeller">{{ product?.sellerName || '匿名卖家' }}</span>
      </div>
      <div class="nav-right">
        <button v-if="isAuthenticated && currentUser?.id !== product?.sellerId" class="follow-btn" :class="{ followed: isFollowing }" @click="toggleFollow">
          <svg v-if="!isFollowing" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="8.5" cy="7" r="4"/><polyline points="17 11 19 13 23 9"/></svg>
          {{ isFollowing ? '已关注' : '关注' }}
        </button>
        <button class="share-btn" @click="handleShare">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
            <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
          </svg>
        </button>
      </div>
    </header>

    <div v-if="loading" class="loading-state">
      <div class="skeleton-image"></div>
      <div class="skeleton-block">
        <div class="skeleton-line w-80"></div>
        <div class="skeleton-line w-40"></div>
        <div class="skeleton-line w-60"></div>
      </div>
    </div>

    <div v-else-if="error" class="error-state">
      <span class="error-icon">😕</span>
      <p>{{ error }}</p>
      <button @click="loadProduct">重试</button>
    </div>

    <main v-else-if="product" class="detail-content">
      <div class="content-wrapper">
        <section v-if="productImages.length > 0" class="image-gallery">
          <div class="gallery-grid" :class="{ single: productImages.length === 1, multi: productImages.length > 1 }">
            <div v-for="(img, idx) in productImages" :key="idx" class="gallery-item" @click="previewImage(img)">
              <img :src="img" loading="lazy" class="gallery-img" @error="onImageError" />
            </div>
          </div>
          <span v-if="productImages.length > 1" class="gallery-counter">{{ currentImage + 1 }} / {{ productImages.length }}</span>
        </section>

        <section class="text-section">
          <div class="price-row">
            <span class="price-current">¥{{ formatPrice(product.price) }}</span>
            <span v-if="product.originalPrice && product.originalPrice > product.price" class="price-original">¥{{ formatPrice(product.originalPrice) }}</span>
            <span v-if="product.originalPrice && product.originalPrice > product.price" class="discount-badge">-{{ Math.round((1 - product.price / product.originalPrice) * 100) }}%</span>
          </div>
          <h1 class="detail-title">{{ product.name }}</h1>
          <span v-if="product.status === 2" class="sold-badge">已售出</span>
          <div v-if="productTags.length" class="detail-tags">
            <span v-for="tag in productTags" :key="tag" class="tag-hashtag">{{ tag }}</span>
          </div>
          <div v-if="product.sellerCampus" class="seller-campus-info">
            <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9,22 9,12 15,12 15,22"/></svg>
            <span>卖家校区：{{ product.sellerCampus }}</span>
          </div>
          <p class="detail-body">{{ product.description || '暂无详细描述' }}</p>
        </section>

        <section v-if="product.detailImages?.length" class="extra-images">
          <img v-for="(img, idx) in product.detailImages" :key="idx" :src="img" class="extra-img" loading="lazy" @click="previewImage(img)" @error="onImageError" />
        </section>

        <section class="comment-section-wrapper">
          <CommentSection :target-id="product.id" :target-type="'product'" :author-id="product.sellerId" :initial-comments="[]" />
        </section>
      </div>
    </main>

    <footer v-if="product" class="bottom-bar">
      <div class="bottom-comment-input" @click="focusComment">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
        </svg>
        <span>添加评论...</span>
      </div>
      <div class="bottom-actions">
        <LikeButton :is-liked="product.isLiked" :count="product.likeCount || 0" target-type="PRODUCT" :target-id="product.id" @toggled="onLikeToggled" />
        <button class="action-btn" :class="{ active: isFavorited }" @click="toggleFavorite">
          <svg v-if="!isFavorited" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="var(--color-primary-500)" stroke="var(--color-primary-500)" stroke-width="2">
            <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z"/>
          </svg>
        </button>
        <button class="action-btn" @click="focusComment">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
          </svg>
          <span class="action-count">{{ product.commentCount || 0 }}</span>
        </button>
      </div>
      <!-- 功能按钮行：聊一聊、联系卖家/标记已售出 -->
      <div class="bottom-func-actions">
        <button class="chat-btn" @click="focusChat" :disabled="product.status === 2">
          <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 11.5a8.38 8.38 0 01-.9 3.8 8.5 8.5 0 01-7.6 4.7 8.38 8.38 0 01-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 01-.9-3.8 8.5 8.5 0 014.7-7.6 8.38 8.38 0 013.8-.9h.5a8.48 8.48 0 018 8v.5z"/>
          </svg>
          聊一聊
        </button>
        <!-- 卖家：标记已售出 -->
        <button v-if="isOwner && product.status !== 2" class="sold-btn" @click="markAsSold">标记已售出</button>
        <!-- 买家：联系卖家 -->
        <button v-if="!isOwner" class="buy-btn" @click="focusChat" :disabled="product.status === 2">
          {{ product.status === 2 ? '已售出' : '联系卖家' }}
        </button>
      </div>
    </footer>
    <ImageViewer :visible="viewerVisible" :images="allViewableImages" :initial-index="viewerIndex" @close="viewerVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { productApi, followApi, favoriteApi } from '../services/api'
import { useAuthStore } from '../store/auth'
import { useToast } from '../use/useToast'
import BackButton from '../components/BackButton.vue'
import LikeButton from '../components/LikeButton.vue'
import CommentSection from '../components/CommentSection.vue'
import ImageViewer from '../components/ImageViewer.vue'

const route = useRoute()
const router = useRouter()
const { isAuthenticated, currentUser } = useAuthStore()
const toast = useToast()

const product = ref(null)
const loading = ref(true)
const error = ref(null)
const currentImage = ref(0)
const isFollowing = ref(false)
const isFavorited = ref(false)
const viewerVisible = ref(false)
const viewerIndex = ref(0)

const isOwner = computed(() => isAuthenticated.value && currentUser.value?.id === product.value?.sellerId)

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><circle cx="20" cy="20" r="20" fill="#eee"/><circle cx="20" cy="15" r="8" fill="#ccc"/><ellipse cx="20" cy="35" rx="12" ry="8" fill="#ccc"/></svg>')

const productImages = computed(() => {
  if (!product.value) return []
  let urls = product.value.imageUrls
  if (typeof urls === 'string') {
    try { urls = JSON.parse(urls) } catch { urls = null }
  }
  if (Array.isArray(urls) && urls.length) return urls
  if (product.value.coverImage) return [product.value.coverImage]
  return []
})

const productTags = computed(() => {
  if (!product.value?.tags) return []
  return product.value.tags.split(',').filter(t => t.trim())
})

/* 合并主图和详情图，供 ImageViewer 使用，去重 */
const allViewableImages = computed(() => {
  const main = productImages.value || []
  const detail = (product.value?.detailImages || []).filter(img => !main.includes(img))
  return [...main, ...detail]
})

onMounted(() => { loadProduct() })

async function loadProduct() {
  loading.value = true
  error.value = null
  try {
    const res = await productApi.getProductDetail(String(route.params.id))
    if (res.code === 200) {
      product.value = res.data
      if (product.value.sellerId && isAuthenticated.value) checkFollowStatus(product.value.sellerId)
      if (isAuthenticated.value) checkFavoriteStatus(product.value.id)
    } else {
      // 后端返回非200时，检查是否是内容不存在
      const msg = res.message || ''
      if (msg.includes('不存在') || msg.includes('未找到') || res.code === 404) {
        error.value = '内容不存在或已被删除'
      } else {
        error.value = msg || '加载失败'
      }
    }
  } catch (e) {
    // 区分404（内容不存在）和其他错误（网络问题）
    const status = e?.response?.status
    const message = e?.response?.data?.message || e?.message || ''
    if (status === 404 || message.includes('不存在') || message.includes('未找到')) {
      error.value = '内容不存在或已被删除'
    } else {
      error.value = '网络错误，请检查网络连接'
    }
  } finally {
    loading.value = false
  }
}

async function checkFollowStatus(userId) {
  try {
    const res = await followApi.checkFollowing(userId)
    if (res.code === 200) isFollowing.value = res.data.isFollowing
  } catch {}
}

async function checkFavoriteStatus(productId) {
  try {
    const res = await favoriteApi.checkFavorited(productId)
    if (res.code === 200) isFavorited.value = res.data
  } catch {}
}

async function toggleFollow() {
  if (!product.value) return
  try {
    const res = await followApi.toggleFollow(product.value.sellerId)
    if (res.code === 200) isFollowing.value = res.data.isFollowing
  } catch {}
}

function onLikeToggled(isLiked, count) {
  if (product.value) { product.value.isLiked = isLiked; product.value.likeCount = count }
}

async function toggleFavorite() {
  /* 未登录时提示并跳转登录页 */
  if (!isAuthenticated.value) {
    toast.showToast('请先登录')
    router.push('/login')
    return
  }
  if (!product.value) return
  try {
    if (isFavorited.value) {
      await favoriteApi.removeFavorite(product.value.id)
      isFavorited.value = false
    } else {
      await favoriteApi.addFavorite(product.value.id)
      isFavorited.value = true
    }
  } catch {
    isFavorited.value = !isFavorited.value
    toast.showToast('操作失败，请重试', 'error')
  }
}
function focusComment() { window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' }) }
function previewImage(url) {
  const idx = allViewableImages.value.indexOf(url)
  viewerIndex.value = idx >= 0 ? idx : 0
  viewerVisible.value = true
}
async function handleShare() {
  try {
    await navigator.clipboard.writeText(window.location.href)
    toast.showToast('链接已复制', 'success')
  } catch {
    toast.showToast('复制失败，请手动复制', 'error')
  }
}

function focusChat() {
  if (!isAuthenticated.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  router.push({ path: `/chat/${product.value.sellerId}`, query: { productId: product.value.id } })
}

function formatPrice(price) {
  if (!price) return '0'
  return Number(price).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

/* 统一的图片加载失败占位图 */
const BROKEN_IMAGE = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect width="200" height="200" fill="#f3f4f6"/><path d="M60 80h80v60H60z" fill="#e5e7eb"/><circle cx="80" cy="95" r="8" fill="#d1d5db"/><path d="M60 140l30-25 20 15 30-30v40H60z" fill="#d1d5db"/></svg>')

function onAvatarError(e) { e.target.src = defaultAvatar }
function onImageError(e) {
  e.target.src = BROKEN_IMAGE
}
function goToSeller() { if (product.value?.sellerId) router.push(`/users/${product.value.sellerId}`) }

async function markAsSold() {
  if (!product.value) return
  try {
    const res = await productApi.markAsSold(product.value.id)
    if (res.code === 200) {
      product.value.status = 2
      toast.showToast('已标记为售出')
    } else {
      toast.showToast(res.message || '操作失败')
    }
  } catch {
    toast.showToast('操作失败，请重试')
  }
}
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background-color: var(--color-bg-page);
  padding-bottom: 80px;
}

.detail-nav {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 210;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: var(--header-height);
  padding: 0 var(--space-6);
  background: var(--gradient-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(16, 185, 129, 0.08);
  box-shadow: var(--shadow-sm);
}

.nav-left {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  min-width: 0;
  flex: 1;
}

.nav-avatar {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-full);
  object-fit: cover;
  background: var(--color-primary-100);
  flex-shrink: 0;
  cursor: pointer;
  border: 2px solid var(--color-primary-200);
  transition: border-color var(--duration-normal) var(--ease-out);
}

.nav-avatar:hover {
  border-color: var(--color-primary-400);
}

.nav-username {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  transition: color var(--duration-fast);
}

.nav-username:hover {
  color: var(--color-primary-600);
}

.nav-right {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}

.follow-btn {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1_5) var(--space-4);
  border-radius: var(--radius-full);
  border: 1.5px solid var(--color-primary-300);
  background: var(--color-primary-50);
  color: var(--color-primary-600);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
}

.follow-btn:hover {
  background: var(--color-primary-100);
  border-color: var(--color-primary-400);
  box-shadow: var(--shadow-green);
}

.follow-btn.followed {
  background: var(--color-primary-100);
  border-color: var(--color-primary-200);
  color: var(--color-primary-700);
}

.share-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: rgba(16, 185, 129, 0.08);
  color: var(--color-primary-600);
  cursor: pointer;
  border-radius: var(--radius-full);
  transition: all var(--duration-normal) var(--ease-out);
}

.share-btn:hover {
  background: rgba(16, 185, 129, 0.15);
  transform: scale(1.05);
}

.loading-state {
  padding-top: var(--header-height);
}

.skeleton-image {
  width: 100%;
  max-height: 500px;
  aspect-ratio: 16/10;
  background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-primary-50) 37%, var(--color-gray-100) 63%);
  background-size: 400% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
}

.skeleton-block {
  padding: var(--space-6);
  background: var(--color-bg-primary);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  margin-top: -12px;
  position: relative;
}

.skeleton-line {
  height: 20px;
  border-radius: var(--radius-sm);
  background: linear-gradient(90deg, var(--color-gray-100) 25%, var(--color-primary-50) 37%, var(--color-gray-100) 63%);
  background-size: 400% 100%;
  animation: shimmer 1.8s ease-in-out infinite;
  margin-bottom: var(--space-3);
}

.w-80 { width: 80%; }
.w-60 { width: 60%; }
.w-40 { width: 40%; }

.error-state {
  padding: 120px var(--space-8);
  text-align: center;
  color: var(--color-text-tertiary);
}

.error-icon {
  font-size: 64px;
  margin-bottom: var(--space-4);
  display: block;
}

.error-state p {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-4);
}

.error-state button {
  padding: var(--space-2_5) var(--space-8);
  border: none;
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  border-radius: var(--radius-full);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
  box-shadow: var(--shadow-green);
  transition: all var(--duration-normal) var(--ease-out);
}

.error-state button:hover {
  box-shadow: var(--shadow-green-lg);
  transform: translateY(-1px);
}

.detail-content {
  padding-top: var(--header-height);
}

.content-wrapper {
  max-width: var(--container-lg);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1fr;
  gap: 0;
}

.image-gallery {
  background: var(--color-bg-primary);
  position: relative;
  border-radius: 0;
  overflow: hidden;
}

.gallery-grid {
  display: grid;
  gap: 0;
}

.gallery-grid.single {
  grid-template-columns: 1fr;
}

.gallery-grid.multi {
  grid-template-columns: repeat(2, 1fr);
}

.gallery-item {
  overflow: hidden;
  background: var(--color-gray-100);
  cursor: pointer;
  position: relative;
  min-height: 200px;
}

.gallery-item::after {
  content: '';
  position: absolute;
  inset: 0;
  background: transparent;
  transition: background var(--duration-normal) var(--ease-out);
}

.gallery-item:hover::after {
  background: rgba(16, 185, 129, 0.05);
}

.gallery-img {
  width: 100%;
  object-fit: cover;
  display: block;
  transition: transform var(--duration-slow) var(--ease-out);
}

.gallery-item:hover .gallery-img {
  transform: scale(1.02);
}

.gallery-grid.single .gallery-item {
  max-height: 500px;
  min-height: 280px;
}

.gallery-grid.single .gallery-img {
  width: 100%;
  height: auto;
  max-height: 500px;
  object-fit: cover;
}

.gallery-grid.multi .gallery-item {
  aspect-ratio: 1 / 1;
}

.gallery-grid.multi .gallery-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery-counter {
  position: absolute;
  bottom: var(--space-3);
  right: var(--space-3);
  padding: var(--space-1) var(--space-3);
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  color: #fff;
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  border-radius: var(--radius-full);
  letter-spacing: 0.02em;
}

.text-section {
  background: var(--color-bg-primary);
  padding: var(--space-6);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  margin-top: -12px;
  position: relative;
  z-index: 1;
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.04);
}

.price-row {
  display: flex;
  align-items: baseline;
  gap: var(--space-2_5);
  margin-bottom: var(--space-3);
}

.price-current {
  font-size: var(--text-4xl);
  font-weight: var(--font-extrabold);
  color: var(--color-primary-600);
  letter-spacing: -0.02em;
  line-height: 1;
}

.price-original {
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  text-decoration: line-through;
  font-weight: var(--font-normal);
}

.discount-badge {
  display: inline-flex;
  align-items: center;
  padding: var(--space-0_5) var(--space-2);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  box-shadow: var(--shadow-green);
  letter-spacing: 0.02em;
}

.detail-title {
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  line-height: var(--leading-snug);
  margin: 0 0 var(--space-3);
}

.detail-body {
  font-size: var(--text-base);
  font-weight: var(--font-normal);
  color: var(--color-text-secondary);
  line-height: var(--leading-relaxed);
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.tag-hashtag {
  font-size: 12px;
  color: var(--color-primary-600, #059669);
  white-space: nowrap;
  cursor: pointer;
}

.seller-campus-info {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin-bottom: var(--space-3);
  padding: var(--space-1_5) var(--space-3);
  background: var(--color-gray-50);
  border-radius: var(--radius-md);
  width: fit-content;
}

.seller-campus-info svg {
  stroke: var(--color-primary-400);
}

.extra-images {
  background: var(--color-bg-primary);
  margin-top: var(--space-3);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.extra-img {
  width: 100%;
  display: block;
  cursor: pointer;
  transition: opacity var(--duration-normal) var(--ease-out);
}

.extra-img:hover {
  opacity: 0.92;
}

.comment-section-wrapper {
  margin-top: var(--space-3);
  background: var(--color-bg-primary);
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-card);
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: var(--z-sticky);
  display: flex;
  align-items: center;
  height: 68px;
  padding: 0 var(--space-6);
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: var(--gradient-glass);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-top: 1px solid rgba(16, 185, 129, 0.08);
  box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.04);
}

.bottom-comment-input {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex: 1;
  max-width: 200px;
  padding: var(--space-2) var(--space-3);
  background: var(--color-gray-100);
  border-radius: var(--radius-full);
  cursor: pointer;
  font-size: var(--text-sm);
  color: var(--color-text-tertiary);
  margin-right: var(--space-3);
  transition: all var(--duration-normal) var(--ease-out);
}

.bottom-comment-input:hover {
  background: var(--color-gray-200);
}

.bottom-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}

/* 功能按钮行：PC端与操作按钮同行 */
.bottom-func-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-2) var(--space-2_5);
  border: none;
  background: none;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  transition: all var(--duration-normal) var(--ease-out);
}

.action-btn:hover {
  background: var(--color-primary-50);
  color: var(--color-primary-600);
}

.action-btn.active {
  color: var(--color-primary-500);
}

.action-count {
  font-size: var(--text-xs);
  color: var(--color-text-tertiary);
}

.chat-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1_5);
  padding: var(--space-2_5) var(--space-5);
  border: none;
  background: var(--gradient-primary);
  color: var(--color-text-inverse);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
  border-radius: var(--radius-full);
  box-shadow: var(--shadow-green);
  transition: all var(--duration-normal) var(--ease-out);
  white-space: nowrap;
}

.chat-btn:hover {
  box-shadow: var(--shadow-green-lg);
  transform: translateY(-1px);
}

.chat-btn:active {
  transform: scale(0.97);
}

.buy-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-2_5) var(--space-5);
  border: none;
  background: var(--color-primary-700);
  color: var(--color-text-inverse);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  cursor: pointer;
  border-radius: var(--radius-full);
  box-shadow: 0 4px 14px rgba(4, 120, 87, 0.3);
  transition: all var(--duration-normal) var(--ease-out);
  white-space: nowrap;
}

.buy-btn:hover {
  background: var(--color-primary-800);
  box-shadow: 0 6px 20px rgba(4, 120, 87, 0.35);
  transform: translateY(-1px);
}

.buy-btn:active {
  transform: scale(0.97);
}

.buy-btn:disabled {
  background: var(--color-gray-300);
  cursor: not-allowed;
  opacity: 0.7;
}

.sold-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: var(--color-gray-500, #6b7280);
  margin-left: 8px;
  vertical-align: middle;
}

.sold-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-2_5) var(--space-5);
  border: 1.5px solid var(--color-primary-500);
  background: transparent;
  color: var(--color-primary-600);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.sold-btn:active {
  background: var(--color-primary-50);
  transform: scale(0.97);
}

@media (min-width: 769px) {
  .content-wrapper {
    display: grid;
    grid-template-columns: 1fr 420px;
    gap: var(--space-6);
    padding: var(--space-6);
    align-items: start;
  }

  .image-gallery {
    border-radius: var(--radius-xl);
    overflow: hidden;
    box-shadow: var(--shadow-card);
  }

  .gallery-grid.single .gallery-item {
    max-height: 500px;
  }

  .gallery-grid.single .gallery-img {
    max-height: 500px;
  }

  .text-section {
    border-radius: var(--radius-xl);
    margin-top: 0;
    box-shadow: var(--shadow-card);
  }

  .extra-images {
    grid-column: 1 / -1;
    border-radius: var(--radius-xl);
  }

  .comment-section-wrapper {
    grid-column: 1 / -1;
  }

  .bottom-bar {
    max-width: var(--container-xl);
    left: 50%;
    transform: translateX(-50%);
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  }
}

@media (max-width: 768px) {
  .detail-page {
    padding-bottom: 100px;
  }

  .detail-nav {
    height: 52px;
    padding: 0 var(--space-4);
  }

  .nav-avatar {
    width: 32px;
    height: 32px;
  }

  .nav-username {
    font-size: var(--text-xs);
  }

  .follow-btn {
    padding: var(--space-1) var(--space-2_5);
    font-size: 11px;
  }

  .content-wrapper {
    padding: 0;
  }

  .text-section {
    padding: var(--space-4);
    border-radius: var(--radius-xl) var(--radius-xl) 0 0;
  }

  .price-current {
    font-size: var(--text-2xl);
  }

  .detail-title {
    font-size: var(--text-lg);
  }

  /* 移动端底部栏改为两行布局：操作按钮行 + 功能按钮行 */
  .bottom-bar {
    height: auto;
    flex-direction: column;
    padding: var(--space-2) var(--space-3);
    gap: var(--space-1);
  }

  .bottom-comment-input {
    display: none;
  }

  .bottom-actions {
    width: 100%;
    justify-content: space-around;
  }

  .bottom-func-actions {
    width: 100%;
    justify-content: center;
    gap: var(--space-3);
  }

  .chat-btn {
    padding: var(--space-2) var(--space-3);
    font-size: var(--text-xs);
  }

  .buy-btn {
    padding: var(--space-2) var(--space-3);
    font-size: var(--text-xs);
  }

  .gallery-grid.single .gallery-item {
    max-height: none;
    min-height: 240px;
    aspect-ratio: auto;
  }

  .gallery-grid.single .gallery-img {
    max-height: none;
    width: 100%;
    height: auto;
  }
}
</style>
