<template>
  <div class="create-product-page">
    <header class="page-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h1 class="header-title">发布商品</h1>
    </header>

    <main class="form-content">
      <section class="upload-section">
        <div class="section-title">商品图片</div>
        <ImageUploader v-model="imageUrls" :max-count="9" :max-size="10" />
        <p class="upload-tip">第一张为封面图，最多上传9张（选填）</p>
      </section>

      <section class="ai-section">
        <div class="ai-card">
          <div class="ai-head">
            <span class="ai-spark">✨</span>
            <div class="ai-head-text">
              <div class="ai-title">AI 智能文案</div>
              <div class="ai-subtitle">上传图片后一键生成标题与描述</div>
            </div>
          </div>
          <button
            type="button"
            class="ai-btn"
            :disabled="aiGenerating"
            @click="generateWithAI"
          >
            <span v-if="aiGenerating" class="ai-loading"></span>
            {{ aiGenerating ? 'AI 正在识别商品...' : '✨ AI 生成文案' }}
          </button>
          <p v-if="aiError" class="ai-error">{{ aiError }}</p>
        </div>
      </section>

      <section class="form-section">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="rules"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="商品标题" prop="name">
            <el-input
              v-model="formData.name"
              placeholder="请输入商品标题（5-30字）"
              maxlength="30"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="商品描述" prop="description">
            <el-input
              v-model="formData.description"
              type="textarea"
              :rows="4"
              maxlength="500"
              show-word-limit
              placeholder="描述一下商品的成色、购买时间、使用情况等..."
            />
          </el-form-item>

          <div class="form-row">
            <el-form-item label="价格（元）" prop="price" class="form-item-half">
              <el-input-number
                v-model="formData.price"
                :min="0"
                :precision="2"
                :step="0.5"
                :controls="false"
                placeholder="0.00"
                class="price-input-number"
              >
                <template #prefix>
                  <span class="price-prefix">¥</span>
                </template>
              </el-input-number>
            </el-form-item>

            <el-form-item label="原价（元）" class="form-item-half">
              <el-input-number
                v-model="formData.originalPrice"
                :min="0"
                :precision="2"
                :controls="false"
                placeholder="选填"
                class="price-input-number"
              >
                <template #prefix>
                  <span class="price-prefix">¥</span>
                </template>
              </el-input-number>
            </el-form-item>
          </div>

          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="formData.categoryId" placeholder="请选择分类" class="full-width">
              <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
            </el-select>
          </el-form-item>

          <el-form-item label="成色" prop="conditionLevel">
            <el-radio-group v-model="formData.conditionLevel">
              <el-radio-button v-for="cond in conditions" :key="cond.value" :value="cond.value">
                {{ cond.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="交付方式">
            <el-radio-group v-model="formData.deliveryMethod">
              <el-radio-button v-for="dm in deliveryMethods" :key="dm.value" :value="dm.value">
                {{ dm.label }}
              </el-radio-button>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="交易地点">
            <el-input
              v-model="formData.location"
              placeholder="如：图书馆门口、宿舍楼大厅等"
            />
          </el-form-item>

          <el-form-item label="标签（选填，最多 5 个）">
            <TagInput v-model="tags" :preset-tags="tagPresets" :max-tags="5" placeholder="输入标签后按回车添加..." />
          </el-form-item>
        </el-form>
      </section>

      <section class="notice-section">
        <h3 class="notice-title">发布须知</h3>
        <ul class="notice-list">
          <li>请确保商品信息真实有效，禁止发布违禁品</li>
          <li>图片需清晰展示商品实际状况</li>
          <li>建议使用平台聊天功能沟通，保障交易安全</li>
          <li>平台有权下架违规或虚假商品</li>
        </ul>
      </section>

      <div class="submit-row">
        <span v-if="missingHint" class="submit-tooltip">{{ missingHint }}</span>
        <el-button
          type="primary"
          size="large"
          round
          :disabled="submitting || !isFormValid"
          :loading="submitting"
          @click="handleSubmit"
        >
          {{ submitting ? '发布中...' : '发布' }}
        </el-button>
      </div>
    </main>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useToast } from '../use/useToast'
import { productApi, categoryApi, getBaseURL } from '../services/api'
import TagInput from '../components/TagInput.vue'
import ImageUploader from '../components/ImageUploader.vue'

const router = useRouter()
const { isAuthenticated } = useAuthStore()
const toast = useToast()

const formRef = ref()

const formData = ref({
  name: '',
  description: '',
  price: null,
  originalPrice: null,
  categoryId: '',
  conditionLevel: null,
  deliveryMethod: 3,
  location: ''
})

const imageUrls = ref([])
const submitting = ref(false)
const categories = ref([])
const categoriesLoading = ref(false)
const tags = ref([])

// AI 生成文案相关状态
const aiGenerating = ref(false)
const aiError = ref('')
let aiAbortController: AbortController | null = null

const tagPresets = [
  '数码', '书籍', '生活', '运动', '美食', '服饰', '美妆',
  '家具', '电器', '文具', '乐器', '植物', '宠物用品',
  '二手', '闲置', '全新', '九成新', '包邮', '可刀'
]

const rules = {
  name: [
    { required: true, message: '请输入商品标题', trigger: 'blur' },
    { min: 5, max: 30, message: '标题至少5个字符', trigger: 'blur' }
  ],
  description: [{ required: true, message: '请输入商品描述', trigger: 'blur' }],
  price: [
    { validator: (_rule: unknown, value: number | null, callback: (e?: Error) => void) => {
      if (value === null || value === undefined) {
        callback(new Error('请输入价格'))
      } else if (value <= 0) {
        callback(new Error('价格必须大于0'))
      } else {
        callback()
      }
    }, trigger: 'change' }
  ],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  conditionLevel: [{ required: true, message: '请选择成色', trigger: 'change' }]
}

const conditions = [
  { value: 1, label: '全新' },
  { value: 2, label: '几乎全新' },
  { value: 3, label: '轻微使用痕迹' },
  { value: 4, label: '明显使用痕迹' },
  { value: 5, label: '一般' }
]

const deliveryMethods = [
  { value: 1, label: '自提' },
  { value: 2, label: '快递' },
  { value: 3, label: '均可' }
]

const isFormValid = computed(() => {
  return (
    formData.value.name.trim().length >= 5 &&
    formData.value.description.trim().length > 0 &&
    formData.value.price > 0 &&
    formData.value.categoryId !== '' &&
    formData.value.conditionLevel !== null
  )
})

const missingHint = computed(() => {
  if (formData.value.name.trim().length < 5) return '请填写标题（至少5字）'
  if (!formData.value.description.trim()) return '请填写商品描述'
  if (!formData.value.price || formData.value.price <= 0) return '请设置价格'
  if (!formData.value.categoryId) return '请选择分类'
  if (!formData.value.conditionLevel) return '请选择成色'
  return ''
})

async function loadCategories() {
  categoriesLoading.value = true
  try {
    const response = await categoryApi.getCategories()
    if (response.code === 200 && response.data) {
      categories.value = response.data.map(cat => ({
        id: cat.id,
        name: cat.name
      }))
    }
  } catch (e) {
    console.warn('加载分类失败，使用默认分类', e)
    categories.value = [
      { id: 1, name: '数码电子' },
      { id: 2, name: '书籍教材' },
      { id: 3, name: '生活日用' },
      { id: 4, name: '服饰鞋包' },
      { id: 5, name: '美妆护肤' },
      { id: 6, name: '运动户外' }
    ]
  } finally {
    categoriesLoading.value = false
  }
}


async function handleSubmit() {
  if (!isFormValid.value || submitting.value) return

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (!isAuthenticated.value) {
    toast.showToast('请先登录', 'error')
    router.push({ path: '/login', query: { redirect: '/products/create' } })
    return
  }

  try {
    submitting.value = true

    const uploadedUrls = imageUrls.value
    const coverImage = uploadedUrls.length > 0 ? uploadedUrls[0] : null

    const productData = {
      name: formData.value.name.trim(),
      description: formData.value.description.trim(),
      price: Number(formData.value.price),
      originalPrice: formData.value.originalPrice ? Number(formData.value.originalPrice) : null,
      categoryId: Number(formData.value.categoryId),
      conditionLevel: formData.value.conditionLevel,
      deliveryMethod: formData.value.deliveryMethod,
      location: formData.value.location.trim() || null,
      imageUrls: uploadedUrls.length > 0 ? uploadedUrls : null,
      coverImage: coverImage,
      tags: tags.value.length > 0 ? tags.value.join(',') : null
    }

    const response = await productApi.createProduct(productData)

    if (response.code === 200) {
      toast.showToast('发布成功！', 'success')
      setTimeout(() => {
        router.push('/products')
      }, 1200)
    } else {
      toast.showToast(response.message || '发布失败，请稍后重试', 'error')
    }
  } catch (error) {
    console.error('发布失败:', error)
    let msg = '网络错误，请检查网络连接后重试'
    if (error.message) {
      if (error.message.includes('401') || error.message.includes('过期')) {
        msg = '登录已过期，请重新登录'
        setTimeout(() => {
          router.push({ path: '/login', query: { redirect: '/products/create' } })
        }, 1500)
      } else if (error.message.includes('参数错误') || error.message.includes('400')) {
        msg = '提交数据格式有误，请检查填写内容'
      } else if (error.message) {
        msg = error.message
      }
    }
    toast.showToast(msg, 'error')
  } finally {
    submitting.value = false
  }
}

/**
 * AI 生成商品文案：调用后端 SSE 流式接口，把大模型逐 token 的增量实时解析成标题与描述
 * 请求治理：AbortController 可取消、生成中禁用防抖、流式累积 + 标记解析
 */
async function generateWithAI() {
  if (aiGenerating.value) return

  if (!imageUrls.value.length && !formData.value.name.trim()) {
    toast.showToast('请先上传商品图片或填写标题', 'warning')
    return
  }

  aiGenerating.value = true
  aiError.value = ''
  aiAbortController = new AbortController()

  try {
    const token = localStorage.getItem('token')
    const res = await fetch(`${getBaseURL()}/v2/ai/product/description`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({
        imageUrls: imageUrls.value.slice(0, 1),
        titleHint: formData.value.name.trim() || null
      }),
      signal: aiAbortController.signal
    })

    // 区分流式响应与 JSON 错误（如 API Key 未配置时后端返回普通 JSON）
    const contentType = res.headers.get('content-type') || ''
    if (!contentType.includes('text/event-stream')) {
      const json = await res.json().catch(() => null)
      throw new Error(json?.message || `AI 服务异常(${res.status})`)
    }

    if (!res.body) throw new Error('当前浏览器不支持流式响应')

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    // 把 SSE 帧还原成纯文本：丢掉 "data:" 前缀与空行
    const toText = (raw: string) =>
      raw
        .split('\n')
        .filter((line) => line.trim() !== '')
        .map((line) => (line.startsWith('data:') ? line.slice(5) : line))
        .join('')

    // 实时解析：标题取「标题：」到「描述：」之间的内容，描述取「描述：」之后的内容。
    // 注意：模型输出可能不带换行（标题和描述紧挨着），所以不能用换行切分，只能按标记切分
    const applyParsed = (text: string) => {
      const hasTitle = /标题[:：]/.test(text)
      const hasDesc = /描述[:：]/.test(text)

      // 标题只有等「描述：」出现后才能确定边界，避免把描述误吞进标题
      if (hasTitle && hasDesc) {
        const titleMatch = text.match(/标题[:：]\s*(.*?)(?:描述[:：]|$)/)
        if (titleMatch?.[1].trim()) {
          formData.value.name = titleMatch[1].trim()
        }
      }
      if (hasDesc) {
        const descMatch = text.match(/描述[:：]\s*([\s\S]*)$/)
        if (descMatch?.[1].trim()) {
          formData.value.description = descMatch[1].trim()
        }
      }
    }

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // 检测流内错误标记（后端异常时推送 [ERROR] 前缀）
      if (buffer.includes('[ERROR]')) {
        throw new Error(buffer.replace(/[\s\S]*?\[ERROR\]/, '').trim() || '生成失败，请稍后重试')
      }

      // 流式实时回填
      applyParsed(toText(buffer))
    }

    // 流结束兜底：模型若没有严格按「标题：/描述：」格式输出，也能尽量回填
    const finalText = toText(buffer).trim()
    const hasTitle = /标题[:：]/.test(finalText)
    const hasDesc = /描述[:：]/.test(finalText)
    if (hasTitle && !hasDesc) {
      // 只输出了标题没输出描述
      const titleOnly = finalText.match(/标题[:：]\s*(.+)$/)
      if (titleOnly?.[1]) formData.value.name = titleOnly[1].trim()
    } else if (!hasTitle && !hasDesc) {
      // 既没有标题也没有描述标记：把整段正文当作描述回填（描述字段渲染支持换行）
      if (finalText) {
        formData.value.description = finalText
      }
    }

    if (formData.value.description.trim() || formData.value.name.trim()) {
      toast.showToast('AI 文案已生成，请核对修改', 'success')
    } else {
      aiError.value = 'AI 未生成有效文案，请重试'
      toast.showToast(aiError.value, 'warning')
    }
  } catch (e) {
    if ((e as Error)?.name === 'AbortError') return
    const msg = (e as Error)?.message || '生成失败，请稍后重试'
    aiError.value = msg
    toast.showToast(msg, 'error')
  } finally {
    aiGenerating.value = false
    aiAbortController = null
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.create-product-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.page-header {
  position: sticky;
  top: 0;
  z-index: 300;
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
}

.back-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  border-radius: 50%;
  background: #f5f5f5;
  border: none;
  cursor: pointer;
  flex-shrink: 0;
}

.back-btn:active { background: #e0e0e0; }

.back-btn svg { width: 22px; height: 22px; }

.header-title {
  flex: 1;
  text-align: center;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0;
  margin-right: 36px;
}

.submit-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 0 16px 24px;
  max-width: 720px;
  margin: 0 auto;
}

.submit-tooltip {
  background: #fff3e0;
  color: #e65100;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
}

.form-content {
  padding-bottom: 40px;
  max-width: 720px;
  margin: 0 auto;
}

.upload-section {
  background-color: #fff;
  padding: 20px 16px;
  margin-top: 10px;
}

.ai-section {
  margin-top: 10px;
  padding: 0 16px;
}

.ai-card {
  background: linear-gradient(135deg, #f0fdf4 0%, #ecfdf5 55%, #fefce8 100%);
  border: 1px solid #dcfce7;
  border-radius: 16px;
  padding: 16px;
}

.ai-head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.ai-spark {
  font-size: 22px;
  line-height: 1;
}

.ai-head-text {
  flex: 1;
}

.ai-title {
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.ai-subtitle {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.ai-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 0;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 4px 14px rgba(16, 185, 129, 0.25);
}

.ai-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.35);
}

.ai-btn:active:not(:disabled) {
  transform: translateY(0) scale(0.98);
}

.ai-btn:disabled {
  cursor: not-allowed;
  opacity: 0.75;
}

.ai-loading {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: #fff;
  border-radius: 50%;
  animation: ai-spin 0.8s linear infinite;
}

@keyframes ai-spin {
  to { transform: rotate(360deg); }
}

.ai-error {
  margin: 10px 0 0;
  font-size: 12px;
  color: #ef4444;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
}

.upload-tip {
  margin-top: 12px;
  font-size: 12px;
  color: #999;
  text-align: center;
}

.form-section {
  margin-top: 10px;
  background-color: #fff;
  padding: 8px 16px 20px;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-item-half {
  flex: 1;
}

.price-input-number {
  width: 100%;
}

.price-input-number :deep(.el-input__wrapper) {
  padding-left: 6px;
}

.price-prefix {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary-500, #10b981);
  margin-right: 2px;
}

.full-width {
  width: 100%;
}

.notice-section {
  margin-top: 10px;
  background-color: #fff;
  padding: 20px 16px;
}

.notice-title {
  font-size: 16px;
  font-weight: 700;
  color: #333;
  margin: 0 0 14px 0;
}

.notice-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.notice-list li {
  position: relative;
  padding-left: 16px;
  font-size: 13px;
  color: #888;
  line-height: 2;
}

.notice-list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 10px;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background-color: var(--color-primary-500, #10b981);
}
</style>
