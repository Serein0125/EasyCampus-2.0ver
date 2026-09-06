<template>
  <button class="back-btn" @click="goBack">
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
  </button>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const props = defineProps({
  fallback: { type: String, default: '/' }
})

const router = useRouter()

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push(props.fallback)
  }
}
</script>

<style scoped>
.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: none;
  color: var(--color-text-primary, #333);
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background 0.15s;
}

.back-btn:active {
  background: var(--color-bg-secondary, #f0f2f5);
}
</style>
