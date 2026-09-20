<template>
  <el-card
    class="activity-card"
    shadow="hover"
    :body-style="{ padding: '0px' }"
    @click="emit('click', activity.id)"
  >
    <div class="card-cover">
      <img
        v-if="activity.coverImage"
        :src="activity.coverImage"
        :alt="activity.title"
        class="cover-img"
        loading="lazy"
        @error="imageError = true"
      />
      <div v-else class="cover-placeholder">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="32" height="32">
          <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
          <line x1="16" y1="2" x2="16" y2="6"/>
          <line x1="8" y1="2" x2="8" y2="6"/>
          <line x1="3" y1="10" x2="21" y2="10"/>
        </svg>
      </div>
    </div>

    <div class="card-body">
      <h3 class="card-title">{{ activity.title }}</h3>

      <div class="card-meta">
        <span class="meta-item meta-org">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/></svg>
          {{ activity.userName || '活动组织者' }}
        </span>
        <span class="meta-item meta-time">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="15" height="15"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
          {{ formatDate(activity.startTime) }}
        </span>
      </div>

      <div class="card-footer">
        <span v-if="activity.categoryName" class="category-tag">{{ activity.categoryName }}</span>
        <span v-if="activity.location" class="location-text">{{ activity.location }}</span>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'

defineProps({
  activity: { type: Object, required: true }
})
const emit = defineEmits(['click'])

const imageError = ref(false)

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const now = new Date()
  const isSameYear = d.getFullYear() === now.getFullYear()
  const month = d.getMonth() + 1
  const day = d.getDate()
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  if (isSameYear) return `${month}月${day}日 ${hours}:${minutes}`
  return `${d.getFullYear()}/${month}/${day} ${hours}:${minutes}`
}
</script>

<style scoped>
.card-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 4 / 3;
  background: linear-gradient(135deg, var(--color-primary-400), var(--color-primary-600));
  overflow: hidden;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.5);
}

.card-body {
  padding: var(--space-4) var(--space-4);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2_5);
}

.card-title {
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  line-height: var(--leading-snug);
  margin: 0;
  text-align: center;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-meta {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1_5);
}

.meta-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1_5);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.meta-org {
  color: var(--color-primary-600);
  font-weight: var(--font-medium);
}

.meta-org svg {
  flex-shrink: 0;
  color: var(--color-primary-400);
}

.meta-time svg {
  flex-shrink: 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding-top: var(--space-1_5);
}

.category-tag {
  display: inline-flex;
  align-items: center;
  padding: var(--space-0_5) var(--space-2);
  background: var(--color-primary-50);
  color: var(--color-primary-700);
  font-size: 10px;
  font-weight: var(--font-semibold);
  border-radius: var(--radius-full);
  white-space: nowrap;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
}

.location-text {
  font-size: 10px;
  color: var(--color-text-tertiary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
}
</style>

<style>
/* el-card 根节点由 EP 渲染，scoped 无法命中，用全局类兜底（圆角/悬停微动效） */
.activity-card.el-card {
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: transform var(--duration-normal) var(--ease-out),
              box-shadow var(--duration-normal) var(--ease-out);
}

.activity-card.el-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-card-hover);
}

.activity-card.el-card:active {
  transform: translateY(-2px) scale(0.98);
}
</style>
