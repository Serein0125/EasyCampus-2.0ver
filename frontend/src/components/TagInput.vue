<template>
  <div class="tag-input-wrapper">
    <div class="tags-display">
      <span v-for="(tag, idx) in tags" :key="idx" class="tag-chip">
        {{ tag }}
        <button class="tag-remove" @click="removeTag(idx)">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </span>
    </div>
    <div v-if="tags.length < maxTags" class="tag-input-row">
      <input
        :value="inputValue"
        @input="onInput"
        @keydown.enter.prevent="addTag(inputValue)"
        @keydown.backspace="onBackspace"
        @focus="showSuggestions = true"
        @blur="onBlur"
        :placeholder="tags.length === 0 ? placeholder : '添加标签...'"
        class="tag-input"
        maxlength="15"
      />
    </div>
    <div v-if="showSuggestions && filteredSuggestions.length > 0 && tags.length < maxTags" class="suggestions-dropdown">
      <div
        v-for="s in filteredSuggestions"
        :key="s"
        class="suggestion-item"
        @mousedown.prevent="addTag(s)"
      >
        {{ s }}
      </div>
    </div>
    <p v-if="tags.length >= maxTags" class="tag-hint">已添加{{ maxTags }}个标签，达到上限</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, type PropType } from 'vue'

const props = defineProps({
  presetTags: { type: Array as PropType<string[]>, default: () => [] },
  maxTags: { type: Number, default: 5 },
  placeholder: { type: String, default: '输入标签...' }
})

const tags = defineModel<string[]>({ default: () => [] })

const inputValue = ref('')
const showSuggestions = ref(false)

const filteredSuggestions = computed(() => {
  const kw = inputValue.value.trim().toLowerCase()
  if (!kw) return props.presetTags
  return props.presetTags.filter(t => t.toLowerCase().includes(kw) && !tags.value.includes(t))
})

function sanitize(val) {
  return val.replace(/[<>"'&\\/]/g, '').trim()
}

function addTag(raw) {
  const val = sanitize(raw)
  if (!val || val.length < 1 || val.length > 15) return
  if (tags.value.length >= props.maxTags) return
  if (tags.value.includes(val)) {
    inputValue.value = ''
    showSuggestions.value = false
    return
  }
  const newTags = [...tags.value, val]
  tags.value = newTags
  inputValue.value = ''
  showSuggestions.value = false
}

function removeTag(idx) {
  const newTags = tags.value.filter((_, i) => i !== idx)
  tags.value = newTags
}

function onInput(e) {
  const raw = e.target.value
  if (raw.endsWith(',')) {
    const parts = raw.split(',')
    parts.filter(p => p.trim()).forEach(p => addTag(p.trim()))
    inputValue.value = ''
    return
  }
  const cleaned = raw.replace(/[<>"'&\\]/g, '')
  inputValue.value = cleaned
  if (cleaned.trim()) showSuggestions.value = true
}

function onBackspace() {
  if (inputValue.value === '' && tags.value.length > 0) {
    removeTag(tags.value.length - 1)
  }
}

function onBlur() {
  setTimeout(() => {
    if (inputValue.value.trim()) {
      addTag(inputValue.value)
    }
    showSuggestions.value = false
  }, 150)
}
</script>

<style scoped>
.tag-input-wrapper {
  position: relative;
}

.tags-display {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  background: linear-gradient(135deg, #FFF7E6, #FFF0D9);
  border: 1px solid #FFD591;
  border-radius: 14px;
  font-size: 13px;
  color: #FA8C16;
  font-weight: 500;
}

.tag-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border: none;
  background: none;
  color: #FFB366;
  cursor: pointer;
  padding: 0;
  border-radius: 50%;
  transition: all 0.15s ease;
}

.tag-remove:hover {
  color: #FF4D4F;
  background: rgba(255, 77, 79, 0.1);
}

.tag-input-row {
  display: flex;
}

.tag-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  font-size: 14px;
  color: #333;
  outline: none;
  background: #fafafa;
  transition: all 0.25s ease;
}

.tag-input:focus {
  border-color: var(--color-primary-500, #10b981);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.08);
}

.tag-input::placeholder {
  color: #ccc;
}

.tag-hint {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

.suggestions-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 50;
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  max-height: 180px;
  overflow-y: auto;
  margin-top: 4px;
}

.suggestion-item {
  padding: 10px 14px;
  font-size: 14px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s ease;
}

.suggestion-item:hover {
  background: #FFF7E6;
}

.suggestion-item:active {
  background: #FFE7BA;
}
</style>
