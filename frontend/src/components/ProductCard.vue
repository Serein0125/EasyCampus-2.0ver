<template>
  <div
    class="product-card group/card relative overflow-hidden bg-white rounded-xl shadow-card cursor-pointer break-inside-avoid mb-2 transition-[transform,box-shadow] duration-200 ease-emphasized hover:-translate-y-0.5 hover:shadow-card-hover active:-translate-y-0.5 active:scale-[0.99]"
    @click="emit('click')"
  >
    <div class="card-image relative w-full aspect-[4/3] flex items-center justify-center overflow-hidden bg-linear-135 from-gray-100 to-gray-200">
      <img
        v-if="product.coverImage"
        :src="product.coverImage"
        :alt="product.title"
        class="cover-image w-full h-full object-cover transition-transform duration-[350ms] ease-emphasized group-hover/card:scale-105"
        loading="lazy"
        @error="onImageError"
      />
      <div v-else class="cover-placeholder flex items-center justify-center w-full min-h-[120px] p-4 bg-linear-135 from-accent-50 via-accent-100 to-accent-200">
        <p class="placeholder-text m-0 text-[11px] leading-[1.6] text-[#9a3412] text-center line-clamp-5">{{ descriptionSnippet }}</p>
      </div>

      <span v-if="condition" class="condition-badge absolute top-2.5 left-2.5 px-2 py-0.5 bg-black/55 backdrop-blur-[8px] text-white text-xs font-semibold rounded-md leading-[1.4] z-[1]">{{ condition }}</span>

      <div class="chat-overlay absolute inset-x-0 bottom-0 flex justify-center pb-3 opacity-0 translate-y-2 transition-[opacity,transform] duration-200 ease-emphasized z-[1] group-hover/card:opacity-100 group-hover/card:translate-y-0">
        <button
          class="chat-btn inline-flex items-center gap-1.5 px-4 py-1.5 bg-linear-135 from-primary-500 via-primary-600 to-primary-700 text-white text-xs font-semibold border-none rounded-full shadow-green cursor-pointer transition-[transform,box-shadow] duration-[120ms] ease-spring hover:scale-105 hover:shadow-green-lg active:scale-[0.97]"
          @click.stop="onChatClick"
        >
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
          </svg>
          聊一聊
        </button>
      </div>
    </div>

    <div class="card-body pt-[6px] px-2 pb-2">
      <div class="price-row flex items-baseline gap-1 mb-0.5">
        <span class="price-current text-[15px] font-extrabold text-red-500 leading-[1.2] tracking-[-0.02em]">¥{{ formatPrice(product.price) }}</span>
        <span v-if="hasOriginalPrice" class="price-original text-xs text-gray-400 line-through leading-none">¥{{ formatPrice(product.originalPrice) }}</span>
        <span v-if="discountPercent" class="discount-badge inline-flex items-center px-2 py-0.5 bg-linear-135 from-[#ff6b35] to-[#f7931e] text-white text-xs font-extrabold rounded-md leading-[1.2] shadow-[0_2px_8px_rgba(255,107,53,0.4)] tracking-[-0.5px]">-{{ discountPercent }}%</span>
      </div>

      <h3 class="card-title text-[13px] font-semibold text-gray-900 leading-[1.4] mb-0.5 line-clamp-2">{{ product.title || product.name || '闲置好物' }}</h3>

      <div v-if="productTags.length" class="card-tags flex flex-wrap gap-1">
        <span v-for="tag in productTags" :key="tag" class="tag-hashtag text-[11px] text-primary-600 whitespace-nowrap cursor-pointer">{{ tag }}</span>
      </div>

      <div v-if="product.createdAt" class="card-time text-[10px] text-gray-400 pt-px">{{ timeAgo }}</div>

      <div class="card-user-bar flex items-center justify-between gap-1 pt-1 min-h-6">
        <div class="user-left flex items-center gap-1 min-w-0 flex-1 cursor-pointer group/user" @click.stop="goToUser">
          <img :src="product.userAvatar || product.sellerAvatar || defaultAvatar" class="user-avatar w-5 h-5 rounded-full object-cover bg-gray-100 shrink-0 border border-gray-100" loading="lazy" @error="onAvatarError" />
          <span class="user-name text-[11px] text-gray-400 truncate transition-colors duration-[120ms] ease-emphasized group-hover/user:text-primary-500">{{ product.userName || product.sellerName || '校园卖家' }}</span>
        </div>
        <div class="like-wrapper shrink-0" @click.stop>
          <LikeButton
            :is-liked="product.isLiked"
            :count="product.likeCount"
            target-type="PRODUCT"
            :target-id="product.id"
            @toggled="onLikeToggled"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import LikeButton from './LikeButton.vue'

const props = defineProps({
  product: { type: Object, required: true }
})

const emit = defineEmits(['click', 'likeToggled'])
const router = useRouter()

const defaultAvatar = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 40 40"><circle cx="20" cy="20" r="20" fill="#eee"/><circle cx="20" cy="15" r="8" fill="#ccc"/><ellipse cx="20" cy="35" rx="12" ry="8" fill="#ccc"/></svg>')

const productTags = computed(() => {
  if (!props.product.tags) return []
  return props.product.tags.split(',').filter(t => t.trim())
})

const hasOriginalPrice = computed(() => {
  const op = Number(props.product.originalPrice)
  const p = Number(props.product.price)
  return op > 0 && op > p
})

const discountPercent = computed(() => {
  if (!hasOriginalPrice.value) return null
  const op = Number(props.product.originalPrice)
  const p = Number(props.product.price)
  const percent = Math.round((1 - p / op) * 100)
  return percent > 0 ? percent : null
})

const condition = computed(() => {
  return props.product.condition || ''
})

const timeAgo = computed(() => {
  if (!props.product.createdAt) return ''
  const diffMs = Date.now() - new Date(props.product.createdAt).getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin}分钟前`
  if (diffHour < 24) return `${diffHour}小时前`
  if (diffDay < 30) return `${diffDay}天前`
  return new Date(props.product.createdAt).toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' })
})

const descriptionSnippet = computed(() => {
  const text = (props.product.description || '').replace(/<[^>]+>/g, '').replace(/\s+/g, ' ').trim()
  if (!text) return '暂无描述'
  return text.length > 100 ? text.slice(0, 100) + '...' : text
})

function formatPrice(price) {
  if (!price) return '0'
  return Number(price).toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

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

function onChatClick() {
  const sellerId = props.product.sellerId
  if (sellerId) {
    router.push(`/chat/${sellerId}`)
  }
}

function goToUser() {
  const sellerId = props.product.sellerId
  if (sellerId) router.push(`/users/${sellerId}`)
}
</script>
