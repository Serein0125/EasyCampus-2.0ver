<template>
  <el-dialog
    v-model="dialogVisible"
    title="选择发布类型"
    width="420px"
    align-center
    class="publish-action-dialog"
  >
    <div class="sheet-options">
      <button @click="handlePublish('post')" class="sheet-option">
        <span class="option-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="28" height="28">
            <path d="M12 20h9"/><path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
          </svg>
        </span>
        <div class="option-info">
          <span class="option-title">发布帖子</span>
          <span class="option-desc">分享想法、提问或讨论</span>
        </div>
        <svg class="option-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
          <polyline points="9,18 15,12 9,6"/>
        </svg>
      </button>
      <button @click="handlePublish('product')" class="sheet-option">
        <span class="option-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="28" height="28">
            <path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 002 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/>
            <polyline points="3.27 6.96 12 12.01 20.73 6.96"/>
            <line x1="12" y1="22.08" x2="12" y2="12"/>
          </svg>
        </span>
        <div class="option-info">
          <span class="option-title">发布交易</span>
          <span class="option-desc">出售闲置物品</span>
        </div>
        <svg class="option-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18">
          <polyline points="9,18 15,12 9,6"/>
        </svg>
      </button>
    </div>
    <template #footer>
      <el-button round @click="emit('close')">取消</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['close'])
const router = useRouter()

// el-dialog 的 v-model：关闭（遮罩点击/取消）时向父级抛出 close
const dialogVisible = computed({
  get: () => props.visible,
  set: (v: boolean) => { if (!v) emit('close') }
})

function handlePublish(type) {
  emit('close')
  if (type === 'post') {
    router.push('/community/posts/create')
  } else {
    router.push('/products/create')
  }
}
</script>

<style scoped>
.sheet-options {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sheet-option {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 16px 12px;
  border: none;
  background: none;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s ease;
  text-align: left;
}

.sheet-option:active {
  background: #f5f5f5;
}

.option-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--color-primary-500, #10b981);
  background: #FFF7E6;
}

.option-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.option-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.option-desc {
  font-size: 13px;
  color: #999;
}

.option-arrow {
  color: #ccc;
  flex-shrink: 0;
}
</style>
