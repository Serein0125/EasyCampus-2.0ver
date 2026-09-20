<template>
  <div class="tag-input-wrapper">
    <el-select
      ref="selectRef"
      v-model="tags"
      multiple
      filterable
      allow-create
      default-first-option
      :multiple-limit="maxTags"
      :placeholder="tags.length === 0 ? placeholder : '添加标签...'"
      class="tag-input-select"
      @change="onModelChange"
      @blur="commitPendingInput"
    >
      <el-option v-for="s in filteredSuggestions" :key="s" :label="s" :value="s" />
    </el-select>
    <p v-if="tags.length >= maxTags" class="tag-hint">已添加{{ maxTags }}个标签，达到上限</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, type PropType } from 'vue'

// 单个标签的最大长度（迁移前原生输入框的限制值）
const MAX_TAG_LENGTH = 15

// el-select 根节点由 EP 渲染，需要用它在失焦时读取输入框里的残留文本
const selectRef = ref<{ $el?: HTMLElement } | null>(null)

const props = defineProps({
  presetTags: { type: Array as PropType<string[]>, default: () => [] },
  maxTags: { type: Number, default: 5 },
  placeholder: { type: String, default: '输入标签...' }
})

const tags = defineModel<string[]>({ default: () => [] })

// 仅展示未被选中的预设标签，关键词过滤由 el-select filterable 内部完成
const filteredSuggestions = computed(() => {
  return props.presetTags.filter((t) => !(tags.value || []).includes(t))
})

function sanitize(val: string) {
  const cleaned = val.replace(/[<>"'&\\/]/g, '').trim()
  // 超长标签直接拒绝（迁移前是 return 不添加），不做静默截断
  return cleaned.length > MAX_TAG_LENGTH ? '' : cleaned
}

/** 按中英文逗号拆成多个标签，恢复迁移前「逗号批量输入」的行为 */
function splitTags(raw: string): string[] {
  return raw.split(/[,，]/).map(sanitize).filter(Boolean)
}

// allow-create 生成的标签会绕过输入限制，且一次可能输入多个（逗号分隔），统一在这里清洗、拆分、去重
function onModelChange() {
  const cleaned = [...new Set((tags.value || []).flatMap((t) => splitTags(t)))].slice(0, props.maxTags)
  const same =
    cleaned.length === tags.value.length && cleaned.every((t, i) => t === tags.value[i])
  if (!same) tags.value = cleaned
}

// allow-create 只在回车/选中时落值，输入后直接点外部会丢弃。
// 这里在失焦时把输入框残留文本补提交，恢复迁移前「失焦自动添加」的行为
function commitPendingInput() {
  const input = selectRef.value?.$el?.querySelector('input')
  const raw = input?.value
  if (!raw) return
  const pending = splitTags(raw).filter((t) => !(tags.value || []).includes(t))
  if (pending.length) {
    tags.value = [...(tags.value || []), ...pending].slice(0, props.maxTags)
    onModelChange()
  }
  input.value = ''
}
</script>

<style scoped>
.tag-hint {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}
</style>

<style>
/* el-select 根节点由 EP 渲染，scoped 无法命中，用全局类兜底 */
.tag-input-select {
  width: 100%;
}
</style>
