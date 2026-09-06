<template>
  <button class="like-button" :class="{ liked: isLiked, loading: loading }" @click.stop="toggleLike" :disabled="loading">
    <svg v-if="isLiked" viewBox="0 0 24 24" width="16" height="16" fill="#ff4757" stroke="#ff4757" stroke-width="2">
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
    </svg>
    <svg v-else viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2">
      <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
    </svg>
    <span :class="{ 'count-liked': isLiked }">{{ formattedCount }}</span>
  </button>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { likeApi, favoriteApi } from '../services/api'
import { useAuthStore } from '../store/auth'
import { useToast } from '../use/useToast'

const props = defineProps({
  isLiked: { type: Boolean, default: false },
  count: { type: Number, default: 0 },
  targetType: { type: String, required: true },
  targetId: { type: [Number, String], required: true }
})

const emit = defineEmits(['toggled'])

const { isAuthenticated } = useAuthStore()
const toast = useToast()
const loading = ref(false)

const formattedCount = computed(() => {
  const c = props.count || 0
  if (c >= 10000) return (c / 10000).toFixed(1) + 'w'
  if (c >= 1000) return (c / 1000).toFixed(1) + 'k'
  return c.toString()
})

async function toggleLike() {
  if (!isAuthenticated.value) {
    toast.showToast('请先登录', 'error')
    return
  }
  if (loading.value) return
  loading.value = true
  const prevIsLiked = props.isLiked
  const prevCount = props.count
  try {
    const res = await likeApi.toggleLike(props.targetType, props.targetId)
    if (res.code === 200) {
      emit('toggled', res.data.isLiked, res.data.likeCount)
      if (props.targetType && props.targetType.toLowerCase() === 'product') {
        try {
          if (res.data.isLiked) {
            await favoriteApi.addFavorite(props.targetId)
          } else {
            await favoriteApi.removeFavorite(props.targetId)
          }
        } catch {}
      }
    }
  } catch {
    emit('toggled', prevIsLiked, prevCount)
    toast.showToast('操作失败，请重试', 'error')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.like-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  font-size: 13px;
  color: #999;
  transition: color 0.2s, transform 0.2s;
}
.like-button.loading {
  opacity: 0.6;
  pointer-events: none;
}
.like-button:hover {
  transform: scale(1.1);
}
.like-button.liked {
  color: #ff4757;
}
.count-liked {
  color: #ff4757;
}
</style>
