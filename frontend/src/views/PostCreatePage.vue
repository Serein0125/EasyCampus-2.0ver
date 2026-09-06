<template>
  <div class="post-create-page">
    <div class="header">
      <button class="back-btn" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <span class="header-title">发布帖子</span>
    </div>

    <div class="form-area">
      <section class="upload-section">
        <div class="section-label">帖子图片（选填）</div>
        <ImageUploader v-model="imageUrls" :max-count="9" :max-size="10" />
      </section>

      <input v-model="title" class="title-input" placeholder="请输入帖子标题（2-200字）" maxlength="200" />
      <div class="type-selector">
        <button v-for="t in postTypes" :key="t.value" :class="{ active: postType === t.value }" @click="postType = t.value">{{ t.label }}</button>
      </div>
      <textarea v-model="content" class="content-input" placeholder="分享你的想法..." maxlength="10000"></textarea>
      <div class="char-count">{{ content.length }}/10000</div>

      <div class="tag-section">
        <div class="section-label">圈子标签（至少选 1 个，最多 5 个）</div>
        <TagInput v-model="tags" :preset-tags="presetTags" :max-tags="5" placeholder="输入标签后按回车或逗号分隔..." />
      </div>

      <!-- 活动类型时显示联系方式输入 -->
      <div v-if="postType === 'ACTIVITY'" class="contact-section">
        <div class="section-label">报名联系方式（必填，报名者可见）</div>
        <input v-model="contact" class="contact-input" placeholder="微信号/QQ/手机号，方便报名者联系你" maxlength="100" />
      </div>

      <div v-if="error" class="error-msg">{{ error }}</div>

      <div class="submit-row">
        <div v-if="missingHint" class="submit-tooltip">{{ missingHint }}</div>
        <button
          class="fab-submit"
          :class="{ disabled: !canSubmit, loading: submitting }"
          :disabled="!canSubmit || submitting"
          @click="submitPost"
        >
          {{ submitting ? '发布中...' : '发布' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { postApi } from '../services/api'
import TagInput from '../components/TagInput.vue'
import ImageUploader from '../components/ImageUploader.vue'

const router = useRouter()
const title = ref('')
const content = ref('')
const postType = ref('DISCUSSION')
const error = ref(null)
const submitting = ref(false)
const imageUrls = ref([])
const tags = ref([])
const contact = ref('')

const postTypes = [
  { value: 'DISCUSSION', label: '讨论' },
  { value: 'SHOWCASE', label: '展示' },
  { value: 'HELP', label: '求助' },
  { value: 'ACTIVITY', label: '活动' }
]

const presetTags = [
  '数码', '书籍', '运动', '美食', '音乐',
  '学习', '求职', '考研', '二手', '闲置',
  '分享', '经验', '求助', '摄影', '旅行',
  '电影', '游戏', '留学', '吐槽', '美妆'
]

const canSubmit = computed(() =>
  title.value.trim().length >= 2 &&
  content.value.trim().length > 0 &&
  tags.value.length > 0 &&
  (postType.value !== 'ACTIVITY' || contact.value.trim()) &&
  !submitting.value
)

const missingHint = computed(() => {
  if (title.value.trim().length < 2) return '请填写标题'
  if (!content.value.trim()) return '请填写内容'
  if (tags.value.length === 0) return '请添加标签'
  if (postType.value === 'ACTIVITY' && !contact.value.trim()) return '请填写报名联系方式'
  return ''
})

async function submitPost() {
  if (!canSubmit.value) return
  error.value = null
  submitting.value = true
  try {
    const res = await postApi.createPost({
      title: title.value.trim(),
      content: content.value.trim(),
      postType: postType.value,
      imageUrls: imageUrls.value.length > 0 ? imageUrls.value : null,
      coverImage: imageUrls.value.length > 0 ? imageUrls.value[0] : null,
      tags: tags.value.length > 0 ? tags.value.join(',') : null,
      contact: postType.value === 'ACTIVITY' ? contact.value.trim() : null
    })
    if (res.code === 200) {
      router.replace(`/community/posts/${res.data.id}`)
    } else {
      error.value = res.message || '发布失败'
    }
  } catch (e) {
    error.value = e.message || '发布失败，请检查网络连接'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.post-create-page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40px; }
.header {
  display: flex; align-items: center; padding: 12px 16px;
  background: #fff; border-bottom: 1px solid var(--color-bg-secondary, #f0f2f5);
  position: sticky; top: 0; z-index: 300;
}
.back-btn {
  display: flex; align-items: center; justify-content: center;
  width: 36px; height: 36px; border-radius: 50%;
  background: #f5f5f5; border: none; color: var(--color-text-primary, #333); cursor: pointer;
  flex-shrink: 0;
}
.back-btn:active { background: #e0e0e0; }
.header-title { flex: 1; text-align: center; font-size: 16px; font-weight: 600; margin-right: 36px; }

.submit-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding-top: 20px;
}

.submit-tooltip {
  background: #fff3e0;
  color: #e65100;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
}

.fab-submit {
  padding: 12px 32px;
  border-radius: 24px;
  border: none;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-600, #059669));
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.35);
  transition: all 0.25s ease;
  letter-spacing: 2px;
}
.fab-submit:not(.disabled):hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(16, 185, 129, 0.5);
}
.fab-submit:not(.disabled):active { transform: translateY(0) scale(0.96); }
.fab-submit.disabled {
  background: #d0d0d0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  cursor: not-allowed;
}
.form-area { padding: 16px; }
.section-label { font-size: 14px; font-weight: 600; color: var(--color-text-primary, #333); margin-bottom: 10px; }
.upload-section { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 16px; }
.title-input {
  width: 100%; padding: 12px; border: none; border-radius: 8px;
  font-size: 18px; font-weight: 600; outline: none; background: #fff;
  margin-bottom: 12px; box-sizing: border-box;
}
.type-selector { display: flex; gap: 8px; margin-bottom: 12px; }
.type-selector button {
  padding: 6px 14px; border-radius: 16px; border: 1px solid #e0e0e0;
  background: #fff; font-size: 13px; cursor: pointer; color: var(--color-text-secondary, #666);
}
.type-selector button.active { border-color: var(--color-primary-500, #10b981); color: var(--color-primary-500, #10b981); background: #fff8f2; }
.content-input {
  width: 100%; min-height: 200px; padding: 12px; border: none;
  border-radius: 8px; font-size: 15px; line-height: 1.8;
  outline: none; background: #fff; resize: vertical; box-sizing: border-box;
}
.char-count { text-align: right; font-size: 12px; color: var(--color-text-tertiary, #999); padding: 4px 0; }
.tag-section { background: #fff; border-radius: 12px; padding: 16px; margin-top: 16px; }
.contact-section { background: #fff; border-radius: 12px; padding: 16px; margin-top: 16px; }
.contact-input { width: 100%; padding: 10px 12px; border: 1px solid #e0e0e0; border-radius: 8px; font-size: 14px; outline: none; transition: border-color 0.2s; box-sizing: border-box; }
.contact-input:focus { border-color: var(--color-primary-500, #10b981); }
.error-msg { color: #ff4757; font-size: 14px; padding: 8px 0; }
</style>
