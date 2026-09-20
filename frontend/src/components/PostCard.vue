<template>
  <el-card
    class="post-card group/card break-inside-avoid mb-2 cursor-pointer"
    shadow="hover"
    :body-style="{ padding: '0px' }"
    @click="emit('click')"
  >
    <div class="card-image relative w-full aspect-[4/3] flex items-center justify-center overflow-hidden bg-linear-135 from-primary-50 to-primary-100">
      <img
        v-if="post.coverImage"
        :src="post.coverImage"
        :alt="post.title"
        class="cover-image block w-full h-full object-cover transition-transform duration-[350ms] ease-emphasized group-hover/card:scale-[1.03]"
        loading="lazy"
        @error="onImageError"
      />
      <div v-else class="cover-placeholder flex items-center justify-center w-full min-h-[160px] max-h-[180px] p-5 bg-linear-135 from-page via-primary-100 to-primary-200">
        <p class="placeholder-text m-0 text-[14px] leading-[1.6] text-primary-800 text-center line-clamp-5">{{ contentSnippet }}</p>
      </div>
      <span v-if="postTypeLabel" class="type-badge absolute top-2.5 left-2.5 px-2.5 py-[3px] text-[11px] font-semibold rounded-full tracking-[0.03em] backdrop-blur-[8px] z-[2] text-white" :class="typeBadgeClass">
        {{ postTypeLabel }}
      </span>
      <span v-if="post.isAd" class="ad-badge absolute top-2.5 right-2.5 px-2 py-0.5 bg-black/50 text-white/90 text-[10px] font-medium rounded-sm tracking-[0.05em] z-[2] backdrop-blur-[4px]">广告</span>
      <div class="card-shine absolute inset-0 -translate-x-full pointer-events-none z-[3] group-hover/card:animate-card-shine bg-[linear-gradient(105deg,transparent_40%,rgba(255,255,255,0.3)_45%,rgba(255,255,255,0.1)_50%,transparent_55%)]"></div>
    </div>

    <div class="card-content flex flex-col gap-0.5 pt-[6px] px-2 pb-2">
      <h3 class="card-title text-[13px] font-semibold text-gray-900 leading-[1.4] line-clamp-2 break-all">{{ post.title }}</h3>

      <div v-if="postTags.length" class="card-tags flex flex-wrap gap-1">
        <span v-for="tag in postTags" :key="tag" class="tag-hashtag text-[11px] text-primary-600 whitespace-nowrap cursor-pointer">{{ tag }}</span>
      </div>

      <div v-if="post.createdAt" class="card-time text-[10px] text-gray-400 pt-px">{{ timeAgo }}</div>

      <div class="card-user-bar mt-0.5 flex items-center justify-between px-2.5 py-0.5 text-[11px] font-medium leading-[1.5] whitespace-nowrap rounded-full">
        <div class="user-left flex items-center gap-1 min-w-0 flex-1 cursor-pointer group/user" @click.stop="goToUser">
          <img :src="post.userAvatar || defaultAvatar" class="user-avatar w-5 h-5 rounded-full object-cover bg-gray-100 shrink-0 border border-primary-100 transition-colors duration-[120ms] ease-emphasized group-hover/user:border-primary-400" loading="lazy" @error="onAvatarError" />
          <span class="user-name text-[11px] text-gray-400 truncate transition-colors duration-[120ms] ease-emphasized group-hover/user:text-primary-600">{{ post.userName || '匿名用户' }}</span>
        </div>
        <div class="like-wrapper flex items-center gap-1" @click.stop>
          <LikeButton
            :is-liked="post.isLiked"
            :count="post.likeCount"
            target-type="POST"
            :target-id="post.id"
            @toggled="onLikeToggled"
          />
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import LikeButton from './LikeButton.vue'

const props = defineProps({
  post: { type: Object, required: true }
})

const emit = defineEmits(['click', 'likeToggled'])
const router = useRouter()

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><circle cx="20" cy="20" r="20" fill="#f0fdf4"/><circle cx="20" cy="15" r="8" fill="#6ee7b7"/><ellipse cx="20" cy="35" rx="12" ry="8" fill="#6ee7b7"/></svg>')

const POST_TYPE_MAP = {
  DISCUSSION: '讨论',
  SHOWCASE: '展示',
  HELP: '求助',
  ACTIVITY: '活动'
}

/* 帖子类型徽章的多态样式：原来是 4 个动态拼接的 scoped 类，迁移为类名映射表 */
const TYPE_BADGE_CLASS: Record<string, string> = {
  DISCUSSION: 'bg-[rgba(16,185,129,0.85)] shadow-[0_2px_8px_rgba(16,185,129,0.35)]',
  SHOWCASE: 'bg-[rgba(139,92,246,0.85)] shadow-[0_2px_8px_rgba(139,92,246,0.35)]',
  HELP: 'bg-[rgba(249,115,22,0.85)] shadow-[0_2px_8px_rgba(249,115,22,0.35)]',
  ACTIVITY: 'bg-[rgba(244,63,94,0.85)] shadow-[0_2px_8px_rgba(244,63,94,0.35)]'
}

const postTypeLabel = computed(() => {
  return POST_TYPE_MAP[props.post.postType] || ''
})

const typeBadgeClass = computed(() => {
  return TYPE_BADGE_CLASS[props.post.postType] || ''
})

const postTags = computed(() => {
  if (!props.post.tags) return []
  return props.post.tags.split(',').filter(t => t.trim())
})

const timeAgo = computed(() => {
  if (!props.post.createdAt) return ''
  const diffMs = Date.now() - new Date(props.post.createdAt).getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  if (diffHour < 24) return `${diffHour}小时前`
  if (diffDay < 30) return `${diffDay}天前`
  return new Date(props.post.createdAt).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
})

const contentSnippet = computed(() => {
  const text = (props.post.content || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
  return text.length > 120 ? text.slice(0, 120) + '...' : text
})

/* 统一的图片加载失败占位图 */
const BROKEN_IMAGE = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect width="200" height="200" fill="#f3f4f6"/><path d="M60 80h80v60H60z" fill="#e5e7eb"/><circle cx="80" cy="95" r="8" fill="#d1d5db"/><path d="M60 140l30-25 20 15 30-30v40H60z" fill="#d1d5db"/></svg>')

function onAvatarError(e) {
  e.target.src = defaultAvatar
}

function onImageError(e) {
  e.target.src = BROKEN_IMAGE
}

function onLikeToggled(isLiked, count) {
  // 单向数据流：不直接修改 props，由父组件监听 likeToggled 更新数据源
  emit('likeToggled', { isLiked, count })
}

function goToUser() {
  if (props.post.userId) router.push(`/users/${props.post.userId}`)
}
</script>

<style scoped>
/*
 * 保留的少量 scoped CSS：
 * :deep 穿透到子组件 LikeButton 的内部状态（.liked），原子类无法跨组件边界表达，
 * 强行用全局 CSS 又会污染其他页面——这是「渐进迁移、不过度教条」的取舍
 */
.like-wrapper :deep(.like-button.liked) {
  animation: heartBeat 1.3s ease-in-out;
}

@keyframes heartBeat {
  0% { transform: scale(1); }
  14% { transform: scale(1.3); }
  28% { transform: scale(1); }
  42% { transform: scale(1.3); }
  70% { transform: scale(1); }
}
</style>

<style>
/* el-card 根节点由 EP 渲染，scoped 无法命中，用全局类兜底（圆角/悬停微动效） */
.post-card.el-card {
  border-radius: 14px;
  overflow: hidden;
  transition: transform 0.2s cubic-bezier(0.16, 1, 0.3, 1), box-shadow 0.2s ease;
}
.post-card.el-card:hover {
  transform: translateY(-2px);
}
</style>
