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

      <section class="form-section">
        <div class="form-group">
          <label class="form-label required">商品标题</label>
          <input
            type="text"
            v-model="formData.name"
            placeholder="请输入商品标题（5-30字）"
            maxlength="30"
            class="form-input"
            :class="{ 'input-error': errors.name }"
            @blur="validateField('name')"
            @input="clearError('name')"
          />
          <span class="char-count">{{ formData.name.length }}/30</span>
          <p v-if="errors.name" class="error-text">{{ errors.name }}</p>
        </div>

        <div class="form-group">
          <label class="form-label required">商品描述</label>
          <textarea
            v-model="formData.description"
            placeholder="描述一下商品的成色、购买时间、使用情况等..."
            rows="4"
            maxlength="500"
            class="form-textarea"
            :class="{ 'input-error': errors.description }"
            @blur="validateField('description')"
            @input="clearError('description')"
          ></textarea>
          <span class="char-count">{{ formData.description.length }}/500</span>
          <p v-if="errors.description" class="error-text">{{ errors.description }}</p>
        </div>

        <div class="form-row">
          <div class="form-group half">
            <label class="form-label required">价格（元）</label>
            <div class="price-input-wrapper" :class="{ 'input-error-border': errors.price }">
              <span class="price-prefix">¥</span>
              <input
                type="number"
                v-model.number="formData.price"
                placeholder="0.00"
                min="0"
                step="0.01"
                class="form-input price-input"
                @blur="validateField('price')"
                @input="clearError('price')"
              />
            </div>
            <p v-if="errors.price" class="error-text">{{ errors.price }}</p>
          </div>

          <div class="form-group half">
            <label class="form-label">原价（元）</label>
            <div class="price-input-wrapper">
              <span class="price-prefix">¥</span>
              <input
                type="number"
                v-model.number="formData.originalPrice"
                placeholder="选填"
                min="0"
                step="0.01"
                class="form-input price-input"
              />
            </div>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label required">分类</label>
          <select
            v-model="formData.categoryId"
            class="form-select"
            :class="{ 'input-error': errors.categoryId }"
            @change="clearError('categoryId')"
          >
            <option value="">请选择分类</option>
            <option v-for="cat in categories" :key="cat.id" :value="cat.id">
              {{ cat.name }}
            </option>
          </select>
          <p v-if="errors.categoryId" class="error-text">{{ errors.categoryId }}</p>
          <p v-if="categoriesLoading" class="loading-hint">加载分类中...</p>
        </div>

        <div class="form-group">
          <label class="form-label required">成色</label>
          <div class="condition-options">
            <button
              v-for="cond in conditions"
              :key="cond.value"
              @click="selectCondition(cond.value)"
              :class="['condition-option', { active: formData.conditionLevel === cond.value }]"
            >
              {{ cond.label }}
            </button>
          </div>
          <p v-if="errors.conditionLevel" class="error-text">{{ errors.conditionLevel }}</p>
        </div>

        <div class="form-group">
          <label class="form-label">交付方式</label>
          <div class="condition-options">
            <button
              v-for="dm in deliveryMethods"
              :key="dm.value"
              @click="formData.deliveryMethod = dm.value"
              :class="['condition-option', { active: formData.deliveryMethod === dm.value }]"
            >
              {{ dm.label }}
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="form-label">交易地点</label>
          <input
            type="text"
            v-model="formData.location"
            placeholder="如：图书馆门口、宿舍楼大厅等"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label class="form-label">标签（选填，最多 5 个）</label>
          <TagInput v-model="tags" :preset-tags="tagPresets" :max-tags="5" placeholder="输入标签后按回车或逗号分隔..." />
        </div>
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
        <button
          @click="handleSubmit"
          :disabled="submitting || !isFormValid"
          class="publish-btn"
          :class="{ active: isFormValid && !submitting }"
        >
          {{ submitting ? '发布中...' : '发布' }}
        </button>
      </div>
    </main>

  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useToast } from '../use/useToast'
import { productApi, categoryApi } from '../services/api'
import TagInput from '../components/TagInput.vue'
import ImageUploader from '../components/ImageUploader.vue'

const router = useRouter()
const { isAuthenticated } = useAuthStore()
const toast = useToast()

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

const tagPresets = [
  '数码', '书籍', '生活', '运动', '美食', '服饰', '美妆',
  '家具', '电器', '文具', '乐器', '植物', '宠物用品',
  '二手', '闲置', '全新', '九成新', '包邮', '可刀'
]

const errors = reactive({
  name: '',
  description: '',
  price: '',
  categoryId: '',
  conditionLevel: ''
})

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

function selectCondition(value) {
  formData.value.conditionLevel = value
  if (errors.conditionLevel) errors.conditionLevel = ''
}

function validateField(field) {
  switch (field) {
    case 'name':
      if (!formData.value.name.trim()) {
        errors.name = '请输入商品标题'
      } else if (formData.value.name.trim().length < 5) {
        errors.name = '标题至少5个字符'
      } else {
        errors.name = ''
      }
      break
    case 'description':
      if (!formData.value.description.trim()) {
        errors.description = '请输入商品描述'
      } else {
        errors.description = ''
      }
      break
    case 'price':
      if (formData.value.price === null || formData.value.price === '') {
        errors.price = '请输入价格'
      } else if (formData.value.price <= 0) {
        errors.price = '价格必须大于0'
      } else {
        errors.price = ''
      }
      break
    case 'categoryId':
      if (!formData.value.categoryId) {
        errors.categoryId = '请选择分类'
      } else {
        errors.categoryId = ''
      }
      break
    case 'conditionLevel':
      if (!formData.value.conditionLevel) {
        errors.conditionLevel = '请选择成色'
      } else {
        errors.conditionLevel = ''
      }
      break
  }
}

function clearError(field) {
  errors[field] = ''
}

function validateAll() {
  validateField('name')
  validateField('description')
  validateField('price')
  validateField('categoryId')
  validateField('conditionLevel')
  return !errors.name && !errors.description && !errors.price && !errors.categoryId && !errors.conditionLevel
}

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

  if (!validateAll()) {
    toast.showToast('请完善必填信息', 'error')
    return
  }

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
}

.submit-tooltip {
  background: #fff3e0;
  color: #e65100;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
}

.publish-btn {
  padding: 12px 32px;
  border-radius: 24px;
  border: none;
  background: #d0d0d0;
  color: #fff;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  transition: all 0.25s ease;
  letter-spacing: 2px;
}

.publish-btn.active {
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), #059669);
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.35);
}

.publish-btn.active:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(16, 185, 129, 0.5);
}

.publish-btn.active:active { transform: translateY(0) scale(0.96); }

.publish-btn:disabled {
  cursor: not-allowed;
}

.form-content {
  padding-bottom: 40px;
}

.upload-section {
  background-color: #fff;
  padding: 20px 16px;
  margin-top: 10px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 14px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.image-item {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: 12px;
  overflow: hidden;
  background-color: #f5f5f5;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-uploading {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.image-error-mask {
  position: absolute;
  inset: 0;
  background: rgba(255, 77, 79, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
}

.remove-btn {
  position: absolute;
  top: 6px;
  right: 6px;
  width: 24px;
  height: 24px;
  background-color: rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(4px);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.remove-btn svg { width: 12px; height: 12px; }
.remove-btn:active { background-color: rgba(255, 77, 79, 0.9); }

.upload-trigger {
  aspect-ratio: 1 / 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  background-color: #fafafa;
  border: 2px dashed #e0e0e0;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.upload-trigger:active {
  background-color: #FFF7E6;
  border-color: #FFD591;
}

.upload-icon-wrapper {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-icon-wrapper svg { width: 32px; height: 32px; }

.upload-text {
  font-size: 13px;
  color: #bbb;
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

.form-group {
  margin-top: 20px;
  position: relative;
}

.form-group.half {
  flex: 1;
}

.form-row {
  display: flex;
  gap: 16px;
}

.form-label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.form-label.required::after {
  content: '*';
  color: #FF4D4F;
  margin-left: 3px;
}

.form-input,
.form-select,
.form-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  font-size: 15px;
  color: #333;
  outline: none;
  transition: all 0.25s ease;
  background-color: #fafafa;
}

.form-input:focus,
.form-select:focus,
.form-textarea:focus {
  border-color: var(--color-primary-500, #10b981);
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.08);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #ccc;
}

.form-textarea {
  resize: vertical;
  min-height: 100px;
  line-height: 1.6;
}

.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' fill='%23999' viewBox='0 0 16 16'%3E%3Cpath d='M8 11L3 6h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 14px center;
  padding-right: 36px;
}

.char-count {
  position: absolute;
  right: 0;
  bottom: -22px;
  font-size: 12px;
  color: #ccc;
}

.input-error {
  border-color: #FF4D4F !important;
  background-color: #FFF2F0 !important;
}

.input-error-border {
  border-color: #FF4D4F !important;
}

.error-text {
  font-size: 12px;
  color: #FF4D4F;
  margin-top: 6px;
  margin-bottom: 0;
}

.loading-hint {
  font-size: 12px;
  color: #999;
  margin-top: 6px;
}

.price-input-wrapper {
  display: flex;
  align-items: center;
  gap: 4px;
  background-color: #fafafa;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  overflow: hidden;
  transition: all 0.25s ease;
}

.price-input-wrapper:focus-within {
  border-color: var(--color-primary-500, #10b981);
  background-color: #fff;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.08);
}

.price-prefix {
  padding-left: 14px;
  font-size: 17px;
  font-weight: 700;
  color: #FF4D4F;
}

.price-input {
  border: none !important;
  background: none !important;
  padding: 12px 14px !important;
  box-shadow: none !important;
  font-weight: 600;
}

.condition-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.condition-option {
  padding: 9px 18px;
  background-color: #f5f5f5;
  border: 1px solid transparent;
  border-radius: 18px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s ease;
}

.condition-option.active {
  background-color: #FFF7E6;
  border-color: #FFD591;
  color: #FA8C16;
  font-weight: 600;
}

.condition-option:active {
  transform: scale(0.95);
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
