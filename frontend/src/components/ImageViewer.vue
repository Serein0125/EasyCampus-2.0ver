<template>
  <Teleport to="body">
    <div v-if="visible" class="image-viewer" @click.self="close">
      <!-- 顶部栏 -->
      <div class="viewer-header">
        <span class="viewer-counter">{{ currentIndex + 1 }} / {{ images.length }}</span>
        <button class="viewer-close" @click="close">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>

      <!-- 图片区域 -->
      <div class="viewer-body" @touchstart="onTouchStart" @touchmove="onTouchMove" @touchend="onTouchEnd">
        <div class="viewer-track" :style="trackStyle">
          <div v-for="(img, i) in images" :key="i" class="viewer-slide">
            <img :src="img" class="viewer-img" @click.stop @error="onImgError($event)" />
          </div>
        </div>
      </div>

      <!-- 左右箭头（桌面端） -->
      <button v-if="images.length > 1 && currentIndex > 0" class="viewer-arrow prev" @click.stop="prev">
        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <button v-if="images.length > 1 && currentIndex < images.length - 1" class="viewer-arrow next" @click.stop="next">
        <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9,6 15,12 9,18"/></svg>
      </button>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, type PropType } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  images: { type: Array as PropType<string[]>, default: () => [] },
  initialIndex: { type: Number, default: 0 }
})

const emit = defineEmits(['close'])

const currentIndex = ref(0)
const touchStartX = ref(0)
const touchDeltaX = ref(0)
const isSwiping = ref(false)

watch(() => props.visible, (val) => {
  if (val) {
    currentIndex.value = props.initialIndex
    document.body.style.overflow = 'hidden'
  } else {
    document.body.style.overflow = ''
  }
})

watch(() => props.initialIndex, (val) => {
  currentIndex.value = val
})

const trackStyle = computed(() => ({
  transform: `translateX(calc(${-currentIndex.value * 100}% + ${touchDeltaX.value}px))`,
  transition: isSwiping.value ? 'none' : 'transform 0.3s ease'
}))

function close() {
  emit('close')
}

function prev() {
  if (currentIndex.value > 0) currentIndex.value--
}

function next() {
  if (currentIndex.value < props.images.length - 1) currentIndex.value++
}

function onTouchStart(e) {
  touchStartX.value = e.touches[0].clientX
  touchDeltaX.value = 0
  isSwiping.value = true
}

function onTouchMove(e) {
  const dx = e.touches[0].clientX - touchStartX.value
  touchDeltaX.value = dx
}

function onTouchEnd() {
  isSwiping.value = false
  const threshold = 60
  if (touchDeltaX.value < -threshold && currentIndex.value < props.images.length - 1) {
    currentIndex.value++
  } else if (touchDeltaX.value > threshold && currentIndex.value > 0) {
    currentIndex.value--
  }
  touchDeltaX.value = 0
}

function onImgError(e) {
  e.target.src = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"><rect width="200" height="200" fill="#f0f0f0"/><text x="100" y="110" text-anchor="middle" fill="#ccc" font-size="14">加载失败</text></svg>')
}

function onKeydown(e) {
  if (!props.visible) return
  if (e.key === 'Escape') close()
  else if (e.key === 'ArrowLeft') prev()
  else if (e.key === 'ArrowRight') next()
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<style scoped>
.image-viewer {
  position: fixed;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 9999;
  background: rgba(0, 0, 0, 0.92);
  display: flex;
  align-items: center;
  justify-content: center;
}

.viewer-header {
  position: absolute;
  top: 0; left: 0; right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  padding-top: calc(12px + env(safe-area-inset-top, 0px));
  z-index: 10;
}

.viewer-counter {
  color: rgba(255,255,255,0.8);
  font-size: 14px;
  font-weight: 500;
}

.viewer-close {
  width: 36px; height: 36px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.15);
  border: none; border-radius: 50%;
  color: #fff; cursor: pointer;
  transition: background 0.2s;
}

.viewer-close:hover {
  background: rgba(255,255,255,0.3);
}

.viewer-body {
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.viewer-track {
  display: flex;
  height: 100%;
  will-change: transform;
}

.viewer-slide {
  flex: 0 0 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 16px 16px;
}

.viewer-img {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
  border-radius: 4px;
  user-select: none;
  -webkit-user-drag: none;
}

.viewer-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 44px; height: 44px;
  display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.15);
  border: none; border-radius: 50%;
  color: #fff; cursor: pointer;
  transition: background 0.2s;
  z-index: 10;
}

.viewer-arrow:hover {
  background: rgba(255,255,255,0.3);
}

.viewer-arrow.prev { left: 16px; }
.viewer-arrow.next { right: 16px; }

@media (max-width: 768px) {
  .viewer-arrow { display: none; }
}
</style>
