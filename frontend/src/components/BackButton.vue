<template>
  <el-button class="app-back-btn" circle text @click="goBack" aria-label="返回">
    <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5">
      <polyline points="15,18 9,12 15,6" />
    </svg>
  </el-button>
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

<style>
/* el-button 根节点由 EP 渲染，scoped 难以稳定命中，用全局类兜底。
   类名加 app- 前缀避免与各视图自己的 .back-btn（文字型"取消"按钮等）互相污染 */
.app-back-btn {
  width: 36px;
  height: 36px;
  padding: 0;
}
</style>
