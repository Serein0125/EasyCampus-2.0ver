<template>
  <div class="home-page">
    <!-- 有广告Banner时：显示轮播 -->
    <section v-if="banners.length > 0" class="banner-section" @wheel.prevent="onBannerWheel">
      <!-- 桌面端：无限循环轮播 -->
      <div class="banner-track"
        :style="{ transform: `translateX(-${displayIndex * 100}%)`, transition: isWrapping ? 'none' : undefined }"
        @transitionend="onTrackTransitionEnd">
        <div
          v-for="(banner, index) in displayBanners"
          :key="index"
          class="banner-slide"
        >
          <img
            v-if="banner.coverImage"
            :src="banner.coverImage"
            class="banner-cover"
            @error="onCoverError"
            alt=""
          />
          <div class="banner-overlay"></div>
          <div class="banner-content">
            <span v-if="banner.isAd" class="banner-ad-badge">推广</span>
            <h2 class="banner-title">{{ banner.title }}</h2>
            <p class="banner-subtitle">{{ banner.subtitle }}</p>
            <button class="banner-cta" @click="handleBannerCta(banner)">{{ banner.cta }}</button>
          </div>
        </div>
      </div>
      <button v-if="banners.length > 1" class="banner-arrow banner-arrow--prev" @click="slideBackward">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
      </button>
      <button v-if="banners.length > 1" class="banner-arrow banner-arrow--next" @click="slideForward">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="9 18 15 12 9 6"/></svg>
      </button>
      <div v-if="banners.length > 1" class="banner-dots">
        <span
          v-for="(_, index) in banners"
          :key="index"
          class="banner-dot"
          :class="{ active: normalizedBanner === index }"
          @click="goToBanner(index)"
        ></span>
      </div>
    </section>

    <!-- 无广告Banner时：显示广告推流占位海报 -->
    <section v-else class="banner-section banner-placeholder" @click="goToAdCreate">
      <div class="banner-slide placeholder-slide">
        <div class="banner-overlay placeholder-overlay"></div>
        <div class="banner-content placeholder-content">
          <span class="banner-ad-badge">推广位招租</span>
          <h2 class="banner-title">广告推流</h2>
          <p class="banner-subtitle">选择套餐发布广告，让你的内容获得更多曝光</p>
          <button class="banner-cta" @click.stop="goToAdCreate">立即发布</button>
        </div>
        <!-- 装饰背景图标 -->
        <div class="placeholder-icon-wrap">
          <svg viewBox="0 0 24 24" width="80" height="80" fill="none" stroke="rgba(255,255,255,0.15)" stroke-width="1.5">
            <rect x="2" y="3" width="20" height="14" rx="2" ry="2"/>
            <line x1="8" y1="21" x2="16" y2="21"/>
            <line x1="12" y1="17" x2="12" y2="21"/>
          </svg>
        </div>
      </div>
    </section>

    <main class="feed-content">
      <div v-if="loading" class="skeleton-grid">
        <div v-for="i in 8" :key="i" class="skeleton-card">
          <div class="skeleton-image"></div>
          <div class="skeleton-body">
            <div class="skeleton-line skeleton-line--long"></div>
            <div class="skeleton-line skeleton-line--medium"></div>
            <div class="skeleton-line skeleton-line--short"></div>
          </div>
        </div>
      </div>

      <div v-else-if="error" class="error-state">
        <div class="error-illustration">
          <div class="error-circle">
            <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="var(--color-primary-400)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/>
              <path d="M12 8v4"/>
              <path d="M12 16h.01"/>
            </svg>
          </div>
        </div>
        <p class="error-text">{{ error }}</p>
        <button @click="loadFeed()" class="retry-btn">
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
          点击重试
        </button>
      </div>

      <div v-else-if="showLoginGuide" class="login-guide">
        <div class="login-guide-card">
          <div class="login-guide-icon-wrap">
            <svg viewBox="0 0 24 24" width="40" height="40" fill="none" stroke="var(--color-primary-500)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
          </div>
          <h3 class="login-guide-title">登录后可查看关注内容</h3>
          <p class="login-guide-desc">关注感兴趣的账号，获取他们的最新动态</p>
          <button @click="goToLogin" class="login-guide-btn">去登录</button>
        </div>
      </div>

      <div v-else-if="!showLoginGuide && feedItems.length === 0 && recommends.length === 0" class="empty-state">
        <div class="empty-illustration">
          <div class="empty-circle">
            <span class="empty-emoji">{{ emptyIcon }}</span>
          </div>
          <div class="empty-dots">
            <span class="empty-dot"></span>
            <span class="empty-dot"></span>
            <span class="empty-dot"></span>
          </div>
        </div>
        <p class="empty-text">{{ emptyText }}</p>
        <p class="empty-subtext">{{ emptySubText }}</p>
      </div>

      <template v-else>
        <section v-if="recommends.length > 0 && activeTab !== 'following'" class="recommend-section">
          <div class="section-header">
            <div class="section-header-left">
              <h3 class="section-title">✨ 猜你喜欢</h3>
              <span class="section-desc">根据你的浏览推荐</span>
            </div>
          </div>
          <div class="recommend-scroll">
            <template v-for="item in recommends" :key="'rec-' + item.itemType + '-' + item.id">
              <PostCard v-if="item.itemType === 'POST'" :post="item" @click="goToPost(item)" @like-toggled="(data) => handleLikeToggle(item, data)" />
              <ProductCard v-else-if="item.itemType === 'PRODUCT'" :product="item" @click="goToProduct(item.id)" @like-toggled="(data) => handleLikeToggle(item, data)" />
            </template>
          </div>
        </section>

        <div class="section-divider" v-if="recommends.length > 0 && activeTab !== 'following' && feedItems.length > 0">
          <span class="divider-line"></span>
          <span class="divider-text">全部内容</span>
          <span class="divider-line"></span>
        </div>

        <div class="feed-grid" :class="{ 'feed-grid--fade': isFading }">
          <template v-for="item in feedItems" :key="item.itemType + '-' + item.id">
            <PostCard v-if="item.itemType === 'POST'" :post="item" @click="goToPost(item)" @like-toggled="(data) => handleLikeToggle(item, data)" />
            <ProductCard v-else-if="item.itemType === 'PRODUCT'" :product="item" @click="goToProduct(item.id)" @like-toggled="(data) => handleLikeToggle(item, data)" />
          </template>
        </div>
      </template>

      <div v-if="loadingMore" class="loading-more">
        <div class="loading-dots">
          <span class="loading-dot"></span>
          <span class="loading-dot"></span>
          <span class="loading-dot"></span>
        </div>
        <span class="loading-text">加载中...</span>
      </div>

      <div v-if="loadError && !loading && feedItems.length > 0" class="load-error">
        <span class="load-error-text">加载失败</span>
        <button @click="retryLoadMore" class="retry-small-btn">点击重试</button>
      </div>

      <div v-if="!hasMore && !loadingMore && feedItems.length > 0" class="no-more">
        <span class="no-more-line"></span>
        <span class="no-more-text">已经到底啦~</span>
        <span class="no-more-line"></span>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { feedApi, postApi } from '../services/api'
import PostCard from '../components/PostCard.vue'
import ProductCard from '../components/ProductCard.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<string>((route.query.tab as string) || 'discover')

const adBanners = ref([])
const banners = computed(() => {
  // 广告帖子转换为 banner 格式
  return adBanners.value.map(ad => ({
    title: ad.title || '推广内容',
    subtitle: (ad.content || '').replace(/<[^>]+>/g, '').substring(0, 30),
    cta: '查看详情',
    route: `/community/posts/${ad.id}`,
    isAd: true,
    postId: ad.id,
    coverImage: ad.coverImage || null
  }))
})

let bannerTimer = null
const wheelLocked = ref(false)
let wheelTimer = null
const isWrapping = ref(false)
const isTransitioning = ref(false)

// 无限循环轮播：在首尾各克隆一个 slide，通过 transitionend 回调实现无缝跳转
const displayBanners = computed(() => {
  const arr = banners.value
  if (arr.length <= 1) return arr
  return [arr[arr.length - 1], ...arr, arr[0]]
})
// displayIndex 在扩展数组中的位置（1 = 第一条真实 banner）
const displayIndex = ref(1)
// 归一化索引（0..n-1），用于圆点指示器等
const normalizedBanner = computed(() => {
  const n = banners.value.length
  if (n <= 1) return 0
  const di = displayIndex.value
  if (di === 0) return n - 1
  if (di === n + 1) return 0
  return di - 1
})
const isFading = ref(false)

const feedItems = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const hasMore = ref(true)
const error = ref(null)
const loadError = ref(false)
const currentPage = ref(1)
const pageSize = 12

const loadedIds = ref(new Set())

const recommends = ref([])
const recLoading = ref(false)

const isAuthenticated = computed(() => authStore.isAuthenticated.value)

const showLoginGuide = computed(() => {
  return activeTab.value === 'following' && !isAuthenticated.value && !loading.value
})

const emptyIcon = computed(() => {
  const icons = { discover: '📭', following: '👥', campus: '🏫' }
  return icons[activeTab.value] || '📭'
})

const emptyText = computed(() => {
  const texts = { discover: '暂无内容', following: '暂无关注内容', campus: '暂无校区内容' }
  return texts[activeTab.value] || '暂无内容'
})

const emptySubText = computed(() => {
  const texts = { discover: '快去发布第一条内容吧！', following: '关注更多用户，获取他们的最新动态', campus: '设置你的校区，发现身边的同学' }
  return texts[activeTab.value] || ''
})

let scrollHandler = null

onMounted(() => {
  loadFeed()
  loadRecommendations()
  fetchAdBanners()
  setupScrollObserver()
  startBannerAutoplay()
})

onUnmounted(() => {
  if (scrollHandler) {
    window.removeEventListener('scroll', scrollHandler)
    scrollHandler = null
  }
  stopBannerAutoplay()
})

function startBannerAutoplay() {
  stopBannerAutoplay()
  bannerTimer = setInterval(() => {
    slideForward()
  }, 5000)
}

function stopBannerAutoplay() {
  if (bannerTimer) {
    clearInterval(bannerTimer)
    bannerTimer = null
  }
}

// 前进一步：从最后一条真实 slide 滑向 clone-first，transitionend 后无缝跳回
function slideForward() {
  const n = banners.value.length
  if (n <= 1 || isTransitioning.value) return
  isTransitioning.value = true
  displayIndex.value = displayIndex.value + 1
}

// 后退一步：从第一条真实 slide 滑向 clone-last，transitionend 后无缝跳回
function slideBackward() {
  const n = banners.value.length
  if (n <= 1 || isTransitioning.value) return
  isTransitioning.value = true
  displayIndex.value = displayIndex.value - 1
}

// transitionend 回调：滑到克隆 slide 后，关闭过渡动画，瞬间跳回真实位置
function onTrackTransitionEnd(e) {
  // 仅处理 transform 属性的过渡完成，忽略其他属性（避免多次触发）
  if (e.propertyName !== 'transform') return

  const n = banners.value.length
  if (displayIndex.value === 0) {
    // 滑到了 clone-last → 瞬间跳回真实最后一张
    isWrapping.value = true
    displayIndex.value = n
    // 使用双 requestAnimationFrame 确保浏览器完成跳转后再恢复过渡
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        isWrapping.value = false
        isTransitioning.value = false
      })
    })
  } else if (displayIndex.value === n + 1) {
    // 滑到了 clone-first → 瞬间跳回真实第一张
    isWrapping.value = true
    displayIndex.value = 1
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        isWrapping.value = false
        isTransitioning.value = false
      })
    })
  } else {
    // 正常相邻 slide 过渡完成，解锁
    isTransitioning.value = false
  }
}

// 点击圆点直接跳转到指定 banner（归一化索引 0..n-1）
function goToBanner(normalizedIndex) {
  const n = banners.value.length
  if (n <= 1) return
  // 直接跳转：短暂关闭过渡动画
  isWrapping.value = true
  nextTick(() => {
    displayIndex.value = normalizedIndex + 1
    nextTick(() => { isWrapping.value = false })
  })
}

function onBannerWheel(e) {
  if (wheelLocked.value) return
  wheelLocked.value = true
  if (wheelTimer) clearTimeout(wheelTimer)
  wheelTimer = setTimeout(() => { wheelLocked.value = false }, 800)

  if (e.deltaY > 0) {
    slideForward()
  } else {
    slideBackward()
  }
  startBannerAutoplay()
}

async function fetchAdBanners() {
  try {
    const res = await postApi.getPosts({ isAd: true, sortBy: 'hot', size: 20 })
    if (res.code === 200) {
      const data = res.data || {}
      const list = Array.isArray(data) ? data : (data.list || [])
      // 仅 Premium 套餐（exposureBoost >= 10）享有首页Banner位
      adBanners.value = list.filter(ad => (ad.exposureBoost || 0) >= 10)
      // 有广告贴时回到第一条真实 banner
      if (adBanners.value.length > 0) displayIndex.value = 1
    }
  } catch {
    // 广告加载失败不阻塞页面
  }
}

function setupScrollObserver() {
  let ticking = false
  scrollHandler = () => {
    if (ticking) return
    ticking = true
    requestAnimationFrame(() => {
      ticking = false
      if (loadingMore.value || !hasMore.value || loading.value || loadError.value) return
      const scrollTop = document.documentElement.scrollTop || document.body.scrollTop
      const scrollHeight = document.documentElement.scrollHeight
      const clientHeight = document.documentElement.clientHeight
      if (scrollTop + clientHeight >= scrollHeight - 200) {
        loadMoreItems()
      }
    })
  }
  window.addEventListener('scroll', scrollHandler, { passive: true })
}

function dedupItems(newItems) {
  const result = []
  for (const item of newItems) {
    const key = `${item.itemType}-${item.id}`
    if (!loadedIds.value.has(key)) {
      loadedIds.value.add(key)
      result.push(item)
    }
  }
  return result
}

async function loadFeed(isLoadMore = false) {
  try {
    error.value = null
    loadError.value = false
    if (isLoadMore) {
      loadingMore.value = true
    } else {
      loading.value = true
      loadedIds.value = new Set()
    }

    const params = {
      page: currentPage.value,
      size: pageSize,
      type: activeTab.value
    }

    const res = await feedApi.getFeed(params)

    if (res.code === 200) {
      const data = res.data || {}
      const rawItems = data.list || data.records || data.items || []
      const newItems = dedupItems(rawItems)

      if (isLoadMore) {
        feedItems.value = [...feedItems.value, ...newItems]
      } else {
        feedItems.value = newItems
      }

      if (rawItems.length === 0 || newItems.length === 0) {
        hasMore.value = false
      } else if (rawItems.length < pageSize) {
        hasMore.value = false
      } else {
        const total = data.total || 0
        hasMore.value = feedItems.value.length < total
      }
    } else {
      throw new Error(res.message || '加载内容失败')
    }
  } catch (err) {
    console.error('加载Feed失败:', err)
    if (!isLoadMore) {
      error.value = err.message || '加载失败，请稍后重试'
    } else {
      loadError.value = true
      currentPage.value--
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 下拉刷新已移除（H5 移动端特化），滚动到底自动加载更多保持
function loadMoreItems() {
  if (!hasMore.value || loadingMore.value || loadError.value) return
  currentPage.value++
  loadFeed(true)
}

function retryLoadMore() {
  loadError.value = false
  loadFeed(true)
}

watch(() => route.query.tab, (newTab) => {
  if (newTab && newTab !== activeTab.value) {
    activeTab.value = String(newTab)
    currentPage.value = 1
    hasMore.value = true
    feedItems.value = []
    loadedIds.value = new Set()
    error.value = null
    loadError.value = false
    window.scrollTo({ top: 0, behavior: 'smooth' })
    loadFeed()
  }
})

function goToPost(item) {
  trackBehavior('POST', item.id)
  if (item.postType === 'ACTIVITY') {
    router.push(`/activities/${item.id}`)
  } else {
    router.push(`/community/posts/${item.id}`)
  }
}

function goToProduct(productId) {
  trackBehavior('PRODUCT', productId)
  router.push(`/products/${productId}`)
}

function handleLikeToggle(item, data) {
  item.isLiked = data.isLiked
  item.likeCount = data.count
}

function handleBannerCta(banner) {
  if (banner && banner.route) {
    router.push(banner.route)
  }
}

/** 跳转到广告编辑页（我的广告） */
function goToAdCreate() {
  router.push('/ads/create')
}

/** 封面图加载失败时隐藏 img 元素，露出 fallback 渐变背景 */
function onCoverError(e) {
  if (e.target) {
    e.target.style.display = 'none'
  }
}

function goToLogin() {
  router.push({ path: '/login', query: { redirect: '/' } })
}

async function loadRecommendations() {
  if (!isAuthenticated.value) return
  try {
    recLoading.value = true
    const res = await feedApi.getRecommendations(6)
    if (res.code === 200) {
      recommends.value = res.data?.list || []
    }
  } catch {
    // silent
  } finally {
    recLoading.value = false
  }
}

function trackBehavior(targetType, targetId) {
  if (!isAuthenticated.value) return
  feedApi.recordBehavior(targetType, targetId).catch(() => {})
}
</script>

<style scoped>
.home-page {
  min-height: 100vh;
  background: var(--color-bg-page, #f0fdf4);
  position: relative;
}

.banner-section {
  position: relative;
  width: 100%;
  max-width: var(--container-xl, 1200px);
  margin: 0 auto;
  overflow: hidden;
  border-radius: 0 0 var(--radius-2xl, 28px) var(--radius-2xl, 28px);
  aspect-ratio: 3 / 1;
  min-height: 220px;
  max-height: 340px;
  cursor: grab;
}

.banner-section:active {
  cursor: grabbing;
}

.banner-track {
  display: flex;
  width: 100%;
  height: 100%;
  transition: transform 0.6s var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.banner-slide {
  position: relative;
  min-width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  padding: var(--space-12, 3rem) var(--space-16, 4rem);
  overflow: hidden;
  /* 默认渐变背景，作为封面图加载失败或无封面时的 fallback */
  background: linear-gradient(135deg, #059669 0%, #10b981 40%, #34d399 100%);
}

/* 封面图：绝对定位铺满 slide */
.banner-cover {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
  z-index: 0;
}

/* 蒙版覆盖层：左深右浅渐变，确保左侧文字可读，右侧图片可见 */
.banner-overlay {
  position: absolute;
  inset: 0;
  z-index: 1;
  background: linear-gradient(to right, rgba(0, 0, 0, 0.55) 0%, rgba(0, 0, 0, 0.2) 50%, transparent 100%);
  pointer-events: none; /* 不阻挡下方按钮点击 */
}

/* 文字区域：绝对定位在左侧，无额外背景 */
.banner-content {
  position: relative;
  z-index: 2;
  max-width: 520px;
}

.banner-title {
  font-size: var(--text-4xl, 2.25rem);
  font-weight: var(--font-extrabold, 800);
  color: #ffffff;
  margin: 0 0 var(--space-3, 0.75rem);
  line-height: var(--leading-tight, 1.2);
  letter-spacing: -0.02em;
}

.banner-ad-badge {
  display: inline-block;
  padding: 2px 10px;
  background: rgba(255, 255, 255, 0.3);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  color: #ffffff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
  margin-bottom: var(--space-2, 0.5rem);
  letter-spacing: 0.05em;
}

.banner-subtitle {
  font-size: var(--text-lg, 1.0625rem);
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 var(--space-6, 1.5rem);
  font-weight: var(--font-normal, 400);
  line-height: var(--leading-relaxed, 1.65);
}

.banner-cta {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: var(--space-3, 0.75rem) var(--space-8, 2rem);
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  border: 1.5px solid rgba(255, 255, 255, 0.35);
  border-radius: var(--radius-full, 9999px);
  color: #ffffff;
  font-size: var(--text-base, 0.9375rem);
  font-weight: var(--font-semibold, 600);
  cursor: pointer;
  transition: all var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.banner-cta:hover {
  background: rgba(255, 255, 255, 0.35);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.banner-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 36px;
  height: 56px;
  border-radius: var(--radius-full);
  background: rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-out);
  z-index: 5;
}

.banner-arrow:hover {
  background: rgba(0, 0, 0, 0.3);
}

.banner-arrow--prev {
  left: 0;
}

.banner-arrow--next {
  right: 0;
}

.banner-dots {
  position: absolute;
  bottom: var(--space-5, 1.25rem);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: var(--space-2, 0.5rem);
  z-index: 5;
}

.banner-dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full, 9999px);
  background: rgba(255, 255, 255, 0.4);
  cursor: pointer;
  transition: all var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.banner-dot.active {
  width: 24px;
  background: #ffffff;
  box-shadow: 0 0 8px rgba(255, 255, 255, 0.5);
}

.feed-content {
  max-width: var(--container-xl, 1200px);
  margin: 0 auto;
  padding: var(--space-6, 1.5rem) var(--space-6, 1.5rem);
  overscroll-behavior-y: contain;
  -webkit-overflow-scrolling: touch;
}

.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-2, 0.5rem);
}

.skeleton-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-xl, 20px);
  overflow: hidden;
  box-shadow: var(--shadow-card, 0 2px 8px rgba(0,0,0,0.04), 0 0 1px rgba(0,0,0,0.06));
}

.skeleton-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  background: linear-gradient(90deg, var(--color-gray-100, #f3f4f6) 25%, var(--color-gray-200, #e5e7eb) 50%, var(--color-gray-100, #f3f4f6) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-body {
  padding: var(--space-3, 0.75rem) var(--space-4, 1rem) var(--space-4, 1rem);
  display: flex;
  flex-direction: column;
  gap: var(--space-2, 0.5rem);
}

.skeleton-line {
  height: 14px;
  border-radius: var(--radius-sm, 6px);
  background: linear-gradient(90deg, var(--color-gray-100, #f3f4f6) 25%, var(--color-gray-200, #e5e7eb) 50%, var(--color-gray-100, #f3f4f6) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s ease-in-out infinite;
}

.skeleton-line--long {
  width: 90%;
}

.skeleton-line--medium {
  width: 65%;
}

.skeleton-line--short {
  width: 40%;
  height: 10px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16, 4rem) var(--space-6, 1.5rem);
  text-align: center;
}

.error-illustration {
  margin-bottom: var(--space-6, 1.5rem);
}

.error-circle {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  background: var(--color-primary-50, #ecfdf5);
  display: flex;
  align-items: center;
  justify-content: center;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-8px); }
}

.error-text {
  font-size: var(--text-base, 0.9375rem);
  color: var(--color-text-secondary, #4b5563);
  margin-bottom: var(--space-6, 1.5rem);
  line-height: var(--leading-relaxed, 1.65);
}

.retry-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2, 0.5rem);
  padding: var(--space-3, 0.75rem) var(--space-8, 2rem);
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  color: #ffffff;
  border: none;
  border-radius: var(--radius-full, 9999px);
  font-size: var(--text-sm, 0.8125rem);
  font-weight: var(--font-semibold, 600);
  cursor: pointer;
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
  transition: all var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.retry-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-green-lg, 0 8px 25px rgba(16,185,129,0.3));
}

.retry-btn:active {
  transform: translateY(0) scale(0.97);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16, 4rem) var(--space-6, 1.5rem);
  text-align: center;
}

.empty-illustration {
  margin-bottom: var(--space-6, 1.5rem);
  position: relative;
}

.empty-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-50, #ecfdf5), var(--color-primary-100, #d1fae5));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.15));
}

.empty-emoji {
  font-size: 42px;
  opacity: 0.8;
}

.empty-dots {
  display: flex;
  justify-content: center;
  gap: var(--space-2, 0.5rem);
  margin-top: var(--space-3, 0.75rem);
}

.empty-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary-300, #6ee7b7);
  animation: pulse 2s ease-in-out infinite;
}

.empty-dot:nth-child(2) {
  animation-delay: 0.3s;
}

.empty-dot:nth-child(3) {
  animation-delay: 0.6s;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.4; transform: scale(0.8); }
}

.empty-text {
  font-size: var(--text-lg, 1.0625rem);
  font-weight: var(--font-semibold, 600);
  color: var(--color-text-primary, #111827);
  margin-bottom: var(--space-2, 0.5rem);
}

.empty-subtext {
  font-size: var(--text-sm, 0.8125rem);
  color: var(--color-text-tertiary, #9ca3af);
  line-height: var(--leading-relaxed, 1.65);
}

.login-guide {
  display: flex;
  justify-content: center;
  padding: var(--space-10, 2.5rem) var(--space-6, 1.5rem);
}

.login-guide-card {
  background: var(--color-bg-primary, #ffffff);
  border-radius: var(--radius-2xl, 28px);
  padding: var(--space-10, 2.5rem) var(--space-8, 2rem);
  text-align: center;
  width: 100%;
  max-width: 400px;
  box-shadow: var(--shadow-xl, 0 20px 25px -5px rgba(0,0,0,0.08), 0 8px 10px -6px rgba(0,0,0,0.04));
  border: 1px solid var(--color-border-light, #e5e7eb);
}

.login-guide-icon-wrap {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary-50, #ecfdf5), var(--color-primary-100, #d1fae5));
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-5, 1.25rem);
}

.login-guide-title {
  font-size: var(--text-lg, 1.0625rem);
  font-weight: var(--font-semibold, 600);
  color: var(--color-text-primary, #111827);
  margin: 0 0 var(--space-2, 0.5rem);
}

.login-guide-desc {
  font-size: var(--text-sm, 0.8125rem);
  color: var(--color-text-tertiary, #9ca3af);
  margin: 0 0 var(--space-6, 1.5rem);
  line-height: var(--leading-relaxed, 1.65);
}

.login-guide-btn {
  display: inline-flex;
  align-items: center;
  padding: var(--space-3, 0.75rem) var(--space-12, 3rem);
  background: var(--gradient-primary, linear-gradient(135deg, #10b981 0%, #059669 50%, #047857 100%));
  color: #ffffff;
  border: none;
  border-radius: var(--radius-full, 9999px);
  font-size: var(--text-base, 0.9375rem);
  font-weight: var(--font-semibold, 600);
  cursor: pointer;
  box-shadow: var(--shadow-green, 0 4px 14px rgba(16,185,129,0.25));
  transition: all var(--duration-normal, 200ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.login-guide-btn:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-green-lg, 0 8px 25px rgba(16,185,129,0.3));
}

.login-guide-btn:active {
  transform: scale(0.97);
}

.recommend-section {
  margin-bottom: var(--space-8, 2rem);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5, 1.25rem);
}

.section-header-left {
  display: flex;
  align-items: baseline;
  gap: var(--space-3, 0.75rem);
}

.section-title {
  font-size: var(--text-xl, 1.25rem);
  font-weight: var(--font-bold, 700);
  color: var(--color-text-primary, #111827);
  margin: 0;
}

.section-desc {
  font-size: var(--text-xs, 0.6875rem);
  color: var(--color-text-tertiary, #9ca3af);
  font-weight: var(--font-normal, 400);
}

.recommend-scroll {
  column-count: 4;
  column-gap: 8px;
  scrollbar-width: none;
  -webkit-overflow-scrolling: touch;
  padding-bottom: var(--space-2, 0.5rem);
}

.recommend-scroll::-webkit-scrollbar {
  display: none;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: var(--space-4, 1rem);
  margin: var(--space-8, 2rem) 0 var(--space-6, 1.5rem);
}

.divider-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, var(--color-border-light, #e5e7eb), transparent);
}

.divider-text {
  font-size: var(--text-xs, 0.6875rem);
  color: var(--color-text-tertiary, #9ca3af);
  font-weight: var(--font-medium, 500);
  white-space: nowrap;
}

.feed-grid {
  column-count: 4;
  column-gap: 8px;
  transition: opacity 0.3s var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.feed-grid--fade {
  opacity: 0;
}

.loading-more {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-8, 2rem) 0;
  min-height: 80px;
}

.loading-dots {
  display: flex;
  gap: var(--space-1_5, 0.375rem);
}

.loading-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--color-primary-400, #34d399);
  animation: loadingBounce 1.4s ease-in-out infinite;
}

.loading-dot:nth-child(2) {
  animation-delay: 0.16s;
}

.loading-dot:nth-child(3) {
  animation-delay: 0.32s;
}

@keyframes loadingBounce {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.loading-text {
  font-size: var(--text-sm, 0.8125rem);
  color: var(--color-text-tertiary, #9ca3af);
}

.load-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-5, 1.25rem) 0;
  min-height: 60px;
}

.load-error-text {
  font-size: var(--text-sm, 0.8125rem);
  color: var(--color-text-tertiary, #9ca3af);
}

.retry-small-btn {
  padding: var(--space-1_5, 0.375rem) var(--space-4, 1rem);
  border-radius: var(--radius-full, 9999px);
  border: 1.5px solid var(--color-border-medium, #d1d5db);
  background: var(--color-bg-primary, #ffffff);
  color: var(--color-text-secondary, #4b5563);
  font-size: var(--text-xs, 0.6875rem);
  font-weight: var(--font-medium, 500);
  cursor: pointer;
  transition: all var(--duration-fast, 120ms) var(--ease-out, cubic-bezier(0.16, 1, 0.3, 1));
}

.retry-small-btn:hover {
  border-color: var(--color-primary-400, #34d399);
  color: var(--color-primary-600, #059669);
  background: var(--color-primary-50, #ecfdf5);
}

.no-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3, 0.75rem);
  padding: var(--space-6, 1.5rem) 0 var(--space-10, 2.5rem);
  min-height: 60px;
}

.no-more-line {
  flex: 1;
  height: 1px;
  background: linear-gradient(to right, transparent, var(--color-border-light, #e5e7eb), transparent);
  max-width: 120px;
}

.no-more-text {
  font-size: var(--text-xs, 0.6875rem);
  color: var(--color-text-tertiary, #9ca3af);
  white-space: nowrap;
}

@media (max-width: 768px) {
  .skeleton-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-2, 0.5rem);
  }

  .recommend-scroll {
    column-count: 2;
    column-gap: 8px;
  }

  .feed-grid {
    column-count: 2;
    column-gap: 8px;
  }

  .feed-content {
    padding: var(--space-3, 0.75rem) var(--space-3, 0.75rem);
  }

  .section-title {
    font-size: var(--text-lg, 1.0625rem);
  }

  .login-guide-card {
    padding: var(--space-8, 2rem) var(--space-5, 1.25rem);
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
  }

  .recommend-scroll {
    column-count: 3;
    column-gap: 8px;
  }

  .feed-grid {
    column-count: 3;
    column-gap: 8px;
  }
}

@media (min-width: 1025px) {
  .home-page {
    padding-top: 0;
  }
}

/* ===== 广告推流占位海报 ===== */
.banner-placeholder {
  cursor: pointer;
}

.placeholder-slide {
  /* 使用偏蓝紫色渐变，与广告 banner 的绿色渐变区分 */
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 40%, #a78bfa 100%);
}

.placeholder-overlay {
  background: linear-gradient(to right, rgba(0, 0, 0, 0.45) 0%, rgba(0, 0, 0, 0.15) 60%, transparent 100%);
}

.placeholder-content {
  /* 继承 banner-content 的定位，无需额外设置 */
}

/* 装饰背景图标：绝对定位在右侧 */
.placeholder-icon-wrap {
  position: absolute;
  right: var(--space-8, 2rem);
  top: 50%;
  transform: translateY(-50%);
  z-index: 2;
  pointer-events: none;
}

@media (max-width: 768px) {
  .banner-placeholder {
    aspect-ratio: auto;
    min-height: 140px;
    max-height: 180px;
    border-radius: 0;
  }

  .placeholder-icon-wrap {
    right: var(--space-4, 1rem);
  }

  .placeholder-icon-wrap svg {
    width: 48px;
    height: 48px;
  }
}
</style>
