<template>
  <div class="detail-page">
    <header class="detail-nav">
      <div class="nav-left">
        <BackButton />
        <img :src="post?.userAvatar || defaultAvatar" class="nav-avatar" @click="goToUser" @error="onAvatarError" />
        <span class="nav-username" @click="goToUser">{{ post?.userName || '匿名用户' }}</span>
      </div>
      <div class="nav-right">
        <button v-if="isAuthenticated && currentUser?.id !== post?.userId" class="follow-btn" :class="{ followed: isFollowing }" @click="toggleFollow">
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
        <div class="skeleton-line w-60"></div>
        <div class="skeleton-line w-40"></div>
      </div>
    </div>

    <div v-else-if="error" class="error-state">
      <span class="error-icon">😕</span>
      <p>{{ error }}</p>
      <button @click="loadPost">重试</button>
    </div>

    <main v-else-if="post" class="detail-content">
      <section v-if="postImages.length > 0" class="image-gallery">
        <div class="gallery-grid" :class="{ single: postImages.length === 1, multi: postImages.length > 1 }">
          <div v-for="(img, idx) in postImages" :key="idx" class="gallery-item" @click="previewImage(img)">
            <img :src="img" loading="lazy" class="gallery-img" @error="onImageError" />
          </div>
        </div>
      </section>

      <section class="text-section">
        <h1 class="detail-title">
          <span v-if="post.isAd" class="ad-tag">广告</span>
          {{ post.title }}
        </h1>
        <div v-if="postTags.length" class="detail-tags">
          <span v-for="tag in postTags" :key="tag" class="tag-hashtag">{{ tag }}</span>
        </div>
        <div v-if="isAdmin" class="admin-actions">
          <button class="admin-btn" :class="{ active: post.isPinned }" @click="togglePin">
            {{ post.isPinned ? '取消置顶' : '置顶' }}
          </button>
          <button class="admin-btn" :class="{ active: post.isEssence }" @click="toggleEssence">
            {{ post.isEssence ? '取消加精' : '加精' }}
          </button>
        </div>
        <div v-if="post.userCampus" class="user-campus-info">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#999" stroke-width="2"><path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2z"/><polyline points="9,22 9,12 15,12 15,22"/></svg>
          <span>发布者校区：{{ post.userCampus }}</span>
        </div>
        <p class="detail-body" v-html="renderedContent"></p>
      </section>

      <section class="comment-section-wrapper">
        <CommentSection :target-id="post.id" :target-type="'post'" :author-id="post.userId" :initial-comments="[]" />
      </section>
    </main>

    <footer v-if="post" class="bottom-bar">
      <div class="bottom-comment-input" @click="focusComment">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#999" stroke-width="2">
          <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
        </svg>
        <span>添加评论...</span>
      </div>
      <div class="bottom-actions">
        <LikeButton :is-liked="post.isLiked" :count="post.likeCount" target-type="POST" :target-id="post.id" @toggled="onLikeToggled" />
        <button class="action-btn" :class="{ active: isFavorited }" @click="toggleFavorite">
          <svg v-if="!isFavorited" viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" width="20" height="20" fill="#FF6A00" stroke="#FF6A00" stroke-width="2">
            <path d="M19 21l-7-5-7 5V5a2 2 0 012-2h10a2 2 0 012 2z"/>
          </svg>
        </button>
        <button class="action-btn" @click="focusComment">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
          </svg>
          <span class="action-count">{{ post.commentCount || 0 }}</span>
        </button>
      </div>
    </footer>
    <ImageViewer :visible="viewerVisible" :images="postImages" :initial-index="viewerIndex" @close="viewerVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { postApi, followApi, favoriteApi } from '../services/api'
import { useAuthStore } from '../store/auth'
import { useToast } from '../use/useToast'
import DOMPurify from 'dompurify'
import BackButton from '../components/BackButton.vue'
import LikeButton from '../components/LikeButton.vue'
import CommentSection from '../components/CommentSection.vue'
import ImageViewer from '../components/ImageViewer.vue'

const route = useRoute()
const router = useRouter()
const { isAuthenticated, currentUser } = useAuthStore()
const toast = useToast()

const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')

const post = ref(null)
const loading = ref(true)
const error = ref(null)
const isFollowing = ref(false)
const isFavorited = ref(false)
const viewerVisible = ref(false)
const viewerIndex = ref(0)

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><circle cx="20" cy="20" r="20" fill="#eee"/><circle cx="20" cy="15" r="8" fill="#ccc"/><ellipse cx="20" cy="35" rx="12" ry="8" fill="#ccc"/></svg>')

/** 移动端虚拟键盘适配：键盘弹出时将底部栏固定在键盘上方 */
function setupKeyboardAdapter() {
  // 仅移动端生效
  if (!window.visualViewport || window.innerWidth > 768) return null

  const viewportHandler = () => {
    const vv = window.visualViewport
    const bottomBar = document.querySelector<HTMLElement>('.detail-page .bottom-bar')
    if (!bottomBar) return

    if (vv.height < window.innerHeight * 0.85) {
      // 键盘弹出：将底部栏固定在可视区域底部（键盘上方）
      bottomBar.style.bottom = (window.innerHeight - vv.height - vv.offsetTop) + 'px'
    } else {
      // 键盘收起：恢复原始位置
      bottomBar.style.bottom = ''
    }
  }

  window.visualViewport.addEventListener('resize', viewportHandler)
  window.visualViewport.addEventListener('scroll', viewportHandler)

  return () => {
    window.visualViewport.removeEventListener('resize', viewportHandler)
    window.visualViewport.removeEventListener('scroll', viewportHandler)
  }
}

/** 键盘适配清理函数 */
let cleanupKeyboard = null

const postImages = computed(() => {
  if (!post.value) return []
  if (post.value.imageUrls?.length) return post.value.imageUrls
  if (post.value.coverImage) return [post.value.coverImage]
  return []
})

const renderedContent = computed(() => {
  if (!post.value?.content) return ''
  // 将换行符转为 <br>，再用 DOMPurify 过滤危险标签防止 XSS
  const rawHtml = post.value.content.replace(/\n/g, '<br>')
  return DOMPurify.sanitize(rawHtml, {
    ALLOWED_TAGS: ['br', 'p', 'strong', 'em', 'a', 'img', 'ul', 'ol', 'li', 'h1', 'h2', 'h3', 'blockquote', 'code', 'pre'],
    ALLOWED_ATTR: ['href', 'src', 'alt', 'target']
  })
})

const postTags = computed(() => {
  if (!post.value?.tags) return []
  return post.value.tags.split(',').filter(t => t.trim())
})

onMounted(() => {
  loadPost()
  // 移动端键盘适配
  cleanupKeyboard = setupKeyboardAdapter()
})

onUnmounted(() => {
  // 清理键盘适配监听
  if (cleanupKeyboard) {
    cleanupKeyboard()
    cleanupKeyboard = null
  }
})

async function loadPost() {
  loading.value = true
  error.value = null
  try {
    const res = await postApi.getPostDetail(String(route.params.id))
    if (res.code === 200) {
      post.value = res.data
      if (post.value.userId && isAuthenticated.value) checkFollowStatus(post.value.userId)
      if (isAuthenticated.value) checkFavoriteStatus(String(route.params.id))
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
      error.value = '加载失败，请检查网络连接'
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

async function checkFavoriteStatus(postId) {
  try {
    const res = await favoriteApi.checkPostFavorited(postId)
    if (res.code === 200) isFavorited.value = res.data
  } catch {}
}

async function toggleFollow() {
  if (!post.value) return
  try {
    const res = await followApi.toggleFollow(post.value.userId)
    if (res.code === 200) isFollowing.value = res.data.isFollowing
  } catch {}
}

function onLikeToggled(isLiked, count) {
  if (post.value) { post.value.isLiked = isLiked; post.value.likeCount = count }
}

async function toggleFavorite() {
  if (!post.value) return
  if (!isAuthenticated.value) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  try {
    if (isFavorited.value) {
      await favoriteApi.removePostFavorite(post.value.id)
      isFavorited.value = false
    } else {
      await favoriteApi.addPostFavorite(post.value.id)
      isFavorited.value = true
    }
  } catch {
    isFavorited.value = !isFavorited.value
  }
}
function focusComment() { window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' }) }

async function togglePin() {
  if (!post.value) return
  try {
    const newState = !post.value.isPinned
    await postApi.togglePin(post.value.id, newState)
    post.value.isPinned = newState
    toast.showToast(newState ? '已置顶' : '已取消置顶')
  } catch {
    toast.showToast('操作失败')
  }
}

async function toggleEssence() {
  if (!post.value) return
  try {
    const newState = !post.value.isEssence
    await postApi.toggleEssence(post.value.id, newState)
    post.value.isEssence = newState
    toast.showToast(newState ? '已加精' : '已取消加精')
  } catch {
    toast.showToast('操作失败')
  }
}
function handleShare() {
  navigator.clipboard?.writeText(window.location.href).then(() => {
    showMiniToast('链接已复制')
  }).catch(() => {
    showMiniToast('复制失败')
  })
}

/* 简易 DOM toast，用于无 useToast 的场景 */
function showMiniToast(msg) {
  const toastEl = document.createElement('div')
  toastEl.textContent = msg
  toastEl.style.cssText = 'position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);background:rgba(0,0,0,0.75);color:#fff;padding:10px 24px;border-radius:8px;font-size:14px;z-index:9999;pointer-events:none;'
  document.body.appendChild(toastEl)
  setTimeout(() => toastEl.remove(), 2000)
}
function previewImage(url) {
  const idx = postImages.value.indexOf(url)
  viewerIndex.value = idx >= 0 ? idx : 0
  viewerVisible.value = true
}
/* 统一的图片加载失败占位图 */
const BROKEN_IMAGE = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect width="200" height="200" fill="#f3f4f6"/><path d="M60 80h80v60H60z" fill="#e5e7eb"/><circle cx="80" cy="95" r="8" fill="#d1d5db"/><path d="M60 140l30-25 20 15 30-30v40H60z" fill="#d1d5db"/></svg>')

function onAvatarError(e) { e.target.src = defaultAvatar }
function onImageError(e) { e.target.src = BROKEN_IMAGE }
function goToUser() { if (post.value?.userId) router.push(`/users/${post.value.userId}`) }
</script>

<style scoped>
.detail-page {
  min-height: 100vh;
  background-color: #F5F7FA;
  padding-bottom: 66px;
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
  height: 56px;
  padding: 0 16px;
  background: #FFFFFF;
  border-bottom: 1px solid #E8ECF0;
}

.nav-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
  flex: 1;
}

.nav-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  background: #eee;
  flex-shrink: 0;
  cursor: pointer;
}

.nav-username {
  font-size: 14px;
  font-weight: 500;
  color: #333333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}

.nav-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.follow-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border-radius: 4px;
  border: 1px solid #DDE1E6;
  background: #F0F2F5;
  color: #666666;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.follow-btn.followed {
  background: #E8F4FD;
  border-color: #B3D8F5;
  color: #1890FF;
}

.share-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  color: #666666;
  cursor: pointer;
  border-radius: 4px;
}

.share-btn:active { background: #F0F2F5; }

.loading-state { padding-top: 56px; }
.skeleton-image {
  width: 100%;
  aspect-ratio: 1/1;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
}
.skeleton-block { padding: 16px; background: #fff; }
.skeleton-line {
  height: 18px;
  border-radius: 4px;
  background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  margin-bottom: 12px;
}
.w-80 { width: 80%; } .w-60 { width: 60%; } .w-40 { width: 40%; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.error-state {
  padding: 120px 32px;
  text-align: center;
  color: #999999;
}
.error-icon { font-size: 64px; margin-bottom: 16px; display: block; }
.error-state button {
  padding: 10px 32px; border: none; background: var(--color-primary-500, #10b981);
  color: #fff; border-radius: 4px; font-size: 14px; cursor: pointer; margin-top: 16px;
}

.detail-content {
  padding-top: 56px;
}

.image-gallery {
  background: #FFFFFF;
  padding: 0;
}

.gallery-grid {
  display: grid;
  gap: 2px;
}

.gallery-grid.single { grid-template-columns: 1fr; }
.gallery-grid.multi { grid-template-columns: repeat(2, 1fr); }

.gallery-item {
  overflow: hidden;
  background: #F5F7FA;
  cursor: pointer;
}

.gallery-img {
  width: 100%;
  object-fit: contain;
  display: block;
}

.gallery-grid.single .gallery-item { aspect-ratio: 1/1; }
.gallery-grid.single .gallery-img { height: 100%; }
.gallery-grid.multi .gallery-item { aspect-ratio: 1/1; }
.gallery-grid.multi .gallery-img { height: 100%; }

.text-section {
  background: #FFFFFF;
  padding: 16px;
  margin-top: 0;
}

.detail-title {
  font-size: 18px;
  font-weight: 700;
  color: #333333;
  line-height: 1.4;
  margin: 0 0 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.ad-tag {
  display: inline-block;
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.06);
  color: #999;
  font-size: 11px;
  font-weight: 500;
  border-radius: 3px;
  flex-shrink: 0;
  vertical-align: middle;
}

.detail-body {
  font-size: 16px;
  font-weight: 400;
  color: #333333;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.tag-hashtag {
  font-size: 12px;
  color: var(--color-primary-600, #059669);
  white-space: nowrap;
  cursor: pointer;
}

.admin-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.admin-btn {
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid var(--color-border-light, #e5e7eb);
  background: var(--color-bg-primary, #fff);
  color: var(--color-text-secondary, #6b7280);
  cursor: pointer;
  transition: all 0.15s;
}

.admin-btn.active {
  background: var(--color-primary-50, #ecfdf5);
  border-color: var(--color-primary-500, #10b981);
  color: var(--color-primary-600, #059669);
}

.admin-btn:active {
  transform: scale(0.96);
}

.user-campus-info {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #999;
  margin-bottom: 12px;
}

.comment-section-wrapper {
  margin-top: 16px;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: var(--z-sticky, 200);
  display: flex;
  align-items: center;
  height: 50px;
  padding: 0 16px;
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: #FFFFFF;
  border-top: 1px solid #E8ECF0;
}

.bottom-comment-input {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  width: 60%;
  padding: 8px 12px;
  background: #F5F7FA;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #999999;
  margin-right: 12px;
}

.bottom-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 10px;
  border: none;
  background: none;
  color: #666666;
  cursor: pointer;
  border-radius: 4px;
  font-size: 14px;
  transition: all 0.2s ease;
}

.action-btn:active { background: #F0F2F5; }

.action-count {
  font-size: 13px;
  color: #666666;
}

/* 移动端：隐藏评论输入框，让操作按钮获得全部空间 */
@media (max-width: 768px) {
  .bottom-comment-input {
    display: none;
  }

  .bottom-actions {
    flex: 1;
    justify-content: space-around;
  }
}
</style>
