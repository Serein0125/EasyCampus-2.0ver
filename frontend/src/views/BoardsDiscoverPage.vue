<template>
  <div class="boards-discover-page">
    <header class="header">
      <span class="header-title">兴趣圈子</span>
    </header>

    <div v-if="loading" class="loading-state">
      <el-skeleton v-for="i in 4" :key="i" animated :rows="3" class="board-skeleton" />
    </div>

    <el-empty v-else-if="boards.length === 0" description="暂无圈子，敬请期待" class="boards-empty" />

    <div v-else class="board-grid">
      <el-card v-for="board in boards" :key="board.id" class="board-card" shadow="hover" @click="goToBoard(board.id)">
        <div class="board-icon" :style="{ background: boardColors[board.id % boardColors.length] }">
          {{ board.name?.charAt(0) }}
        </div>
        <div class="board-info">
          <h4>{{ board.name }}</h4>
          <p>{{ board.description || '暂无简介' }}</p>
          <span class="member-count">{{ board.productCount || 0 }} 件商品</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { categoryApi } from '../services/api'

const router = useRouter()
const boards = ref([])
const loading = ref(true)
const boardColors = ['#ff6a00', '#e91e63', '#9c27b0', '#3f51b5', '#2196f3', '#009688', '#4caf50', '#ff9800', '#795548', '#607d8b']

onMounted(async () => {
  try {
    const res = await categoryApi.getCategories()
    if (res.code === 200) {
      boards.value = res.data || []
    }
  } catch {} finally {
    loading.value = false
  }
})

function goToBoard(id) {
  router.push(`/boards/${id}`)
}
</script>

<style scoped>
.boards-discover-page { min-height: 100vh; background: #f5f5f5; padding-bottom: 70px; }
.header { padding: 16px; background: #fff; border-bottom: 1px solid #f0f0f0; }
.header-title { font-size: 18px; font-weight: 700; color: #333; }
.board-grid { padding: 12px 16px; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.board-icon {
  width: 48px; height: 48px; border-radius: 12px; display: flex;
  align-items: center; justify-content: center; color: #fff;
  font-size: 22px; font-weight: 700; margin-bottom: 10px;
}
.board-info h4 { font-size: 14px; font-weight: 600; color: #333; margin: 0 0 4px; }
.board-info p { font-size: 12px; color: #999; margin: 0 0 6px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.member-count { font-size: 11px; color: var(--color-primary-500, #10b981); }
</style>

<style>
/* el-card 根节点由 EP 渲染，scoped 无法命中，用全局类兜底 */
.boards-discover-page .board-card.el-card {
  border-radius: 14px;
  cursor: pointer;
  transition: transform 0.15s;
}

.boards-discover-page .board-card.el-card .el-card__body {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 16px;
}

.boards-discover-page .board-card.el-card:hover {
  transform: translateY(-2px);
}

.boards-discover-page .board-skeleton {
  margin-bottom: 12px;
  border-radius: 14px;
}

.boards-discover-page .boards-empty {
  padding: 80px 0;
}
</style>
