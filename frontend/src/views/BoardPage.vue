<template>
  <div class="board-detail-page">
    <header class="header">
      <button class="back-btn" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
      </button>
      <span class="header-title">{{ board?.name || '加载中...' }}</span>
    </header>
    <div class="content">
      <el-card shadow="never" class="board-placeholder-card">
        <el-empty>
          <template #description>
            <p class="placeholder-text">圈子详情页 - 建设中</p>
            <p class="placeholder-sub">这里将展示该圈子的帖子和商品</p>
          </template>
        </el-empty>
      </el-card>
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { categoryApi } from '../services/api'

const route = useRoute()
const board = ref(null)

onMounted(async () => {
  try {
    const res = await categoryApi.getCategoryDetail(String(route.params.id))
    if (res.code === 200) board.value = res.data
  } catch {}
})
</script>
<style scoped>
.board-detail-page { min-height: 100vh; background: #f5f5f5; }
.header { display: flex; align-items: center; padding: 12px 16px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.back-btn { background: none; border: none; cursor: pointer; display: flex; color: #333; }
.header-title { flex: 1; text-align: center; font-size: 16px; font-weight: 600; margin-right: 32px; }
.content { padding: 60px 20px; text-align: center; }
.placeholder-text { font-size: 16px; color: #999; margin: 0; }
.placeholder-sub { font-size: 14px; color: #ccc; margin: 8px 0 0; }
</style>

<style>
.board-placeholder-card.el-card {
  border-radius: 16px;
}
</style>
