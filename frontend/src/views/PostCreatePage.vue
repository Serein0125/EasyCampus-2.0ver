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

      <el-card shadow="never" class="form-card">
        <el-input v-model="title" placeholder="请输入帖子标题（2-200字）" maxlength="200" class="title-input" />

        <div class="type-row">
          <el-radio-group v-model="postType">
            <el-radio-button v-for="t in postTypes" :key="t.value" :value="t.value">{{ t.label }}</el-radio-button>
          </el-radio-group>
        </div>

        <el-input
          v-model="content"
          type="textarea"
          :rows="8"
          maxlength="10000"
          show-word-limit
          class="content-input"
          placeholder="分享你的想法..."
        />

        <div class="tag-section">
          <div class="section-label">圈子标签（至少选 1 个，最多 5 个）</div>
          <TagInput v-model="tags" :preset-tags="presetTags" :max-tags="5" placeholder="输入标签后按回车添加..." />
        </div>

        <!-- 活动类型时显示联系方式输入 -->
        <div v-if="postType === 'ACTIVITY'" class="contact-section">
          <div class="section-label">报名联系方式（必填，报名者可见）</div>
          <el-input v-model="contact" placeholder="微信号/QQ/手机号，方便报名者联系你" maxlength="100" />
        </div>

        <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="error-alert" />

        <div class="submit-row">
          <div v-if="missingHint" class="submit-tooltip">{{ missingHint }}</div>
          <el-button
            type="primary"
            size="large"
            round
            :disabled="!canSubmit"
            :loading="submitting"
            @click="submitPost"
          >
            {{ submitting ? '发布中...' : '发布' }}
          </el-button>
        </div>
      </el-card>
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

.form-area { padding: 16px; max-width: 760px; margin: 0 auto; }
.section-label { font-size: 14px; font-weight: 600; color: var(--color-text-primary, #333); margin-bottom: 10px; }
.upload-section { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 16px; }

.form-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none;
  padding-left: 0;
  font-size: 18px;
  font-weight: 600;
}

.type-row { margin-bottom: 16px; }

.content-input :deep(.el-textarea__inner) {
  font-size: 15px;
  line-height: 1.8;
}

.tag-section { margin-top: 16px; }
.contact-section { margin-top: 16px; }

.error-alert { margin-top: 12px; }

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
</style>
