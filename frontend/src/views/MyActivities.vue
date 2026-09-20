<template>
  <div class="my-activities-page">
    <header class="page-header">
      <button class="back-btn" @click="$router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <h1 class="header-title">我的活动</h1>
      <button class="nav-action" @click="showCreateForm = true" title="创建活动">
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#FF6A00" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      </button>
    </header>

    <!-- 创建活动弹窗 -->
    <el-dialog
      v-model="showCreateForm"
      title="创建新活动"
      width="520px"
      class="create-form-dialog"
      align-center
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        @submit.prevent="handleCreate"
      >
        <el-form-item label="活动标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入活动标题" maxlength="200" />
        </el-form-item>

        <el-form-item label="活动海报">
          <ImageUploader v-model="form.imageUrls" :max-count="1" :max-size="5" />
          <p class="form-hint">建议上传 16:9 或 2:1 比例的横版海报</p>
        </el-form-item>

        <el-form-item label="活动介绍" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="10000"
            show-word-limit
            placeholder="请输入活动详细介绍"
          />
        </el-form-item>

        <el-form-item label="活动地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入详细活动地址" maxlength="200" />
        </el-form-item>

        <div class="form-row">
          <el-form-item label="开始时间" prop="startTime" class="form-item-half">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择开始时间"
              class="datetime-picker"
            />
          </el-form-item>

          <el-form-item label="结束时间" prop="endTime" class="form-item-half">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="选择结束时间"
              class="datetime-picker"
            />
          </el-form-item>
        </div>

        <el-form-item label="负责人联系方式" prop="contact">
          <el-input v-model="form.contact" placeholder="手机号码或邮箱" maxlength="200" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateForm = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">
          {{ submitting ? '创建中...' : '创建活动' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 活动列表 -->
    <div class="content-area">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <el-empty v-else-if="activities.length === 0" description="还没有创建活动">
        <el-button type="primary" round @click="showCreateForm = true">创建第一个活动</el-button>
      </el-empty>

      <div v-else class="activity-list">
        <el-card
          v-for="item in activities"
          :key="item.id"
          class="activity-item"
          shadow="hover"
          :body-style="{ padding: '0px' }"
          @click="goDetail(item.id)"
        >
          <div class="activity-item-inner">
            <div class="item-status" :class="getStatusClass(item)">{{ getStatusText(item) }}</div>
            <div class="item-body">
              <h3 class="item-title">{{ item.title }}</h3>
              <div class="item-meta">
                <span v-if="item.location" class="meta-line">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#FF6A00" stroke-width="2"><path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z"/><circle cx="12" cy="10" r="3"/></svg>
                  {{ item.location }}
                </span>
                <span v-if="item.startTime" class="meta-line">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#FF6A00" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
                  {{ formatDateTime(item.startTime) }}
                </span>
                <span v-if="item.contact" class="meta-line">
                  <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#FF6A00" stroke-width="2"><path d="M22 16.92v3a2 2 0 01-2.18 2 19.79 19.79 0 01-8.63-3.07 19.5 19.5 0 01-6-6 19.79 19.79 0 01-3.07-8.67A2 2 0 014.11 2h3a2 2 0 012 1.72c.127.96.361 1.903.7 2.81a2 2 0 01-.45 2.11L8.09 9.91a16 16 0 006 6l1.27-1.27a2 2 0 012.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0122 16.92z"/></svg>
                  {{ item.contact }}
                </span>
              </div>
            </div>
            <div class="item-right-actions">
              <button class="delete-btn" @click.stop="handleDeleteActivity(item)">删除</button>
              <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9,18 15,12 9,6"/></svg>
            </div>
          </div>
        </el-card>
      </div>

      <div v-if="hasMore && !loading" class="load-more">
        <button class="load-more-btn" @click="loadMore" :disabled="loadingMore">
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { activityApi } from '../services/api'
import { useAuthStore } from '../store/auth'
import { useToast } from '../use/useToast'
import ImageUploader from '../components/ImageUploader.vue'

const router = useRouter()
const toast = useToast()
const authStore = useAuthStore()

const activities = ref([])
const loading = ref(true)
const loadingMore = ref(false)
const page = ref(1)
const hasMore = ref(false)
const showCreateForm = ref(false)
const submitting = ref(false)
const formRef = ref()

const form = ref({
  title: '',
  content: '',
  location: '',
  startTime: '',
  endTime: '',
  contact: '',
  imageUrls: []
})

const rules = {
  title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入活动介绍', trigger: 'blur' }],
  location: [{ required: true, message: '请输入活动地点', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  contact: [
    { required: true, message: '请输入联系方式', trigger: 'blur' },
    { validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
      if (!value) return callback()
      const isPhone = /^1[3-9]\d{9}$/.test(value)
      const isEmail = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
      if (!isPhone && !isEmail) callback(new Error('请输入有效的手机号码或邮箱'))
      else callback()
    }, trigger: 'blur' }
  ],
  endTime: [
    { validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
      if (value && form.value.startTime && new Date(value) <= new Date(form.value.startTime)) {
        callback(new Error('结束时间必须晚于开始时间'))
      } else {
        callback()
      }
    }, trigger: 'change' }
  ]
}

onMounted(() => { loadActivities() })

async function loadActivities() {
  loading.value = true
  try {
    const res = await activityApi.getMyActivities({ page: 1, size: 20 })
    if (res.code === 200) {
      activities.value = res.data.list || []
      hasMore.value = activities.value.length < res.data.total
    }
  } catch {
    toast.showToast('加载活动列表失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  page.value++
  try {
    const res = await activityApi.getMyActivities({ page: page.value, size: 20 })
    if (res.code === 200) {
      const list = res.data.list || []
      activities.value.push(...list)
      hasMore.value = activities.value.length < res.data.total
    }
  } catch {
    toast.showToast('加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

async function handleCreate() {
  if (submitting.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const data = {
      title: form.value.title.trim(),
      content: form.value.content.trim(),
      location: form.value.location.trim(),
      startTime: form.value.startTime ? new Date(form.value.startTime).toISOString().slice(0, 19) : null,
      endTime: form.value.endTime ? new Date(form.value.endTime).toISOString().slice(0, 19) : null,
      contact: form.value.contact.trim(),
      campusTag: authStore.currentUser.value?.campus || '',
      imageUrls: form.value.imageUrls.length > 0 ? form.value.imageUrls : null,
      coverImage: form.value.imageUrls.length > 0 ? form.value.imageUrls[0] : null
    }
    const res = await activityApi.createActivity(data)
    if (res.code === 200) {
      toast.showToast('活动创建成功')
      showCreateForm.value = false
      formRef.value?.resetFields()
      form.value = { title: '', content: '', location: '', startTime: '', endTime: '', contact: '', imageUrls: [] }
      await loadActivities()
    } else {
      toast.showToast(res.message || '创建失败')
    }
  } catch (e) {
    toast.showToast(e.message || '创建失败，请重试')
  } finally {
    submitting.value = false
  }
}

function goDetail(id) {
  router.push(`/activities/${id}`)
}

function getStatusClass(item) {
  const now = new Date()
  const start = item.startTime ? new Date(item.startTime) : null
  const end = item.endTime ? new Date(item.endTime) : null
  if (!start) return 'status-upcoming'
  if (now < start) return 'status-upcoming'
  if (end && now > end) return 'status-past'
  return 'status-ongoing'
}

function getStatusText(item) {
  const now = new Date()
  const start = item.startTime ? new Date(item.startTime) : null
  const end = item.endTime ? new Date(item.endTime) : null
  if (!start) return '待定'
  if (now < start) return '即将开始'
  if (end && now > end) return '已结束'
  return '进行中'
}

function formatDateTime(d) {
  if (!d) return ''
  const dt = new Date(d)
  const y = dt.getFullYear()
  const M = String(dt.getMonth() + 1).padStart(2, '0')
  const day = String(dt.getDate()).padStart(2, '0')
  const h = String(dt.getHours()).padStart(2, '0')
  const m = String(dt.getMinutes()).padStart(2, '0')
  return `${y}年${M}月${day}日 ${h}:${m}`
}

// 删除活动（二次确认）
async function handleDeleteActivity(item) {
  const ok = await toast.showConfirm(`确定要删除活动「${item.title}」吗？删除后不可恢复。`)
  if (!ok) return
  try {
    const res = await activityApi.deleteActivity(item.id)
    if (res.code === 200) {
      activities.value = activities.value.filter(a => a.id !== item.id)
      toast.showToast('活动已删除', 'success')
    } else {
      toast.showToast(res.message || '删除失败', 'error')
    }
  } catch (e) {
    toast.showToast(e.message || '删除失败，请稍后重试', 'error')
  }
}
</script>

<style scoped>
.my-activities-page {
  min-height: 100vh;
  background: #F5F7FA;
}

.page-header {
  position: sticky;
  top: 0;
  z-index: 100;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #f5f5f5;
  border: none;
  color: var(--color-text-primary, #333);
  cursor: pointer;
  flex-shrink: 0;
}
.back-btn svg { width: 22px; height: 22px; }
.header-title {
  flex: 1;
  text-align: center;
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
}
.nav-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  background: #fff7ed;
  cursor: pointer;
  border-radius: 50%;
  flex-shrink: 0;
  transition: background-color 0.2s;
}
.nav-action:hover { background: #ffedd5; }

.content-area { padding-top: 0; }

/* 创建活动弹窗 */
.form-row { display: flex; gap: 12px; }
.form-item-half { flex: 1; min-width: 0; }
.form-item-half :deep(.el-date-editor) { width: 100%; }
.form-hint { margin: 6px 0 0; font-size: 12px; color: #999; }

/* 加载/空状态 */
.loading-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 80px 32px; color: #999;
}
.spinner {
  width: 32px; height: 32px; border: 3px solid #E8ECF0; border-top-color: var(--color-primary-500, #10b981);
  border-radius: 50%; animation: spin 0.8s linear infinite; margin-bottom: 12px;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* 活动列表 */
.activity-list { padding: 12px 16px; }

.activity-item {
  margin-bottom: 10px;
}
.activity-item:active { transform: scale(0.98); }

.activity-item-inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
}

.item-status {
  flex-shrink: 0; padding: 4px 10px; border-radius: 10px;
  font-size: 11px; font-weight: 600; color: #fff; white-space: nowrap;
}
.status-upcoming { background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399)); }
.status-ongoing { background: linear-gradient(135deg, #52c41a, #73d13d); }
.status-past { background: #bbb; }

.item-body { flex: 1; min-width: 0; }
.item-title {
  font-size: 15px; font-weight: 600; color: #333; margin: 0 0 6px;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.item-meta { display: flex; flex-direction: column; gap: 3px; }
.meta-line {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; color: #999;
}

.load-more { text-align: center; padding: 16px; }
.load-more-btn {
  padding: 8px 24px; border: 1px solid #E8ECF0; border-radius: 20px;
  background: #fff; color: #666; font-size: 13px; cursor: pointer;
}
.load-more-btn:active { background: #F5F7FA; }

/* 列表项右侧操作区域 */
.item-right-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.delete-btn {
  padding: 5px 12px;
  border: none;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  white-space: nowrap;
  background-color: #FFF1F0;
  color: #FF4D4F;
  transition: background-color 0.2s;
}

.delete-btn:active {
  background-color: #FFCCC7;
}
</style>
