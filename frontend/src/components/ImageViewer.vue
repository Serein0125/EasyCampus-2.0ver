<template>
  <el-image-viewer
    v-if="visible && images.length > 0"
    :url-list="images"
    :initial-index="initialIndex"
    :show-progress="images.length > 1"
    hide-on-click-modal
    :teleported="true"
    @close="close"
  />
</template>

<script setup lang="ts">
import { type PropType } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  images: { type: Array as PropType<string[]>, default: () => [] },
  initialIndex: { type: Number, default: 0 }
})

const emit = defineEmits(['close'])

function close() {
  emit('close')
}
</script>
