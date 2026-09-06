<template>
  <div class="my-activities-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <span class="nav-title">我的活动</span>
      <button class="nav-action" @click="showCreateForm = true">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="#FF6A00" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
      </button>
    </header>

    <!-- 创建活动表单 -->
    <div v-if="showCreateForm" class="create-form-overlay" @click.self="showCreateForm = false">
      <div class="create-form-panel">
        <div class="form-header">
          <h3>创建新活动</h3>
          <button class="form-close" @click="showCreateForm = false">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <form @submit.prevent="handleCreate" class="form-body">
          <div class="form-group">
            <label class="form-label">活动标题 <span class="required">*</span></label>
            <input v-model="form.title" type="text" class="form-input" placeholder="请输入活动标题" maxlength="200" />
            <span v-if="errors.title" class="form-error">{{ errors.title }}</span>
          </div>

          <!-- 活动海报上传 -->
          <div class="form-group">
            <label class="form-label">活动海报</label>
            <ImageUploader v-model="form.imageUrls" :max-count="1" :max-size="5" />
            <p class="form-hint">建议上传 16:9 或 2:1 比例的横版海报</p>
          </div>

          <div class="form-group">
            <label class="form-label">活动介绍 <span class="required">*</span></label>
            <textarea v-model="form.content" class="form-textarea" placeholder="请输入活动详细介绍" rows="4" maxlength="10000"></textarea>
            <span v-if="errors.content" class="form-error">{{ errors.content }}</span>
          </div>

          <div class="form-group">
            <label class="form-label">活动地点 <span class="required">*</span></label>
            <input v-model="form.location" type="text" class="form-input" placeholder="请输入详细活动地址" maxlength="200" />
            <span v-if="errors.location" class="form-error">{{ errors.location }}</span>
          </div>

          <div class="form-row">
            <div class="form-group flex-1">
              <label class="form-label">开始时间 <span class="required">*</span></label>
              <input v-model="form.startTime" type="datetime-local" class="form-input" />
              <span v-if="errors.startTime" class="form-error">{{ errors.startTime }}</span>
            </div>
            <div class="form-group flex-1">
              <label class="form-label">结束时间</label>
              <input v-model="form.endTime" type="datetime-local" class="form-input" />
              <span v-if="errors.endTime" class="form-error">{{ errors.endTime }}</span>
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">负责人联系方式 <span class="required">*</span></label>
            <input v-model="form.contact" type="text" class="form-input" placeholder="手机号码或邮箱" maxlength="200" />
            <span v-if="errors.contact" class="form-error">{{ errors.contact }}</span>
          </div>

          <button type="submit" class="submit-btn" :disabled="submitting">
            {{ submitting ? '创建中...' : '创建活动' }}
          </button>
        </form>
      </div>
    </div>

    <!-- 活动列表 -->
    <div class="content-area">
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="activities.length === 0" class="empty-state">
        <svg viewBox="0 0 24 24" width="64" height="64" fill="none" stroke="#ddd" stroke-width="1.5"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        <p>还没有创建活动</p>
        <button class="create-first-btn" @click="showCreateForm = true">创建第一个活动</button>
      </div>

      <div v-else class="activity-list">
        <div v-for="item in activities" :key="item.id" class="activity-item" @click="goDetail(item.id)">
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
      </div>

      <div v-if="hasMore && !loading" class="load-more">
        <button class="load-more-btn" @click="loadMore" :disabled="loadingMore">
          {{ loadingMore ? '加载中...' : '加载更多' }}
        </button>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <Teleport to="body">
      <div v-if="deleteDialog.visible" class="delete-modal-overlay" @click="cancelDelete">
        <div class="delete-modal" @click.stop>
          <div class="modal-icon">⚠️</div>
          <h3 class="modal-title">确定要删除吗？</h3>
          <p class="modal-desc">确定要删除活动「{{ deleteDialog.title }}」吗？删除后不可恢复。</p>
          <div class="modal-actions">
            <button @click="cancelDelete" class="modal-btn cancel-btn">取消</button>
            <button @click="confirmDeleteActivity" class="modal-btn confirm-delete-btn" :disabled="deleteDialog.loading">
              {{ deleteDialog.loading ? '删除中...' : '确定删除' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
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

// 删除确认弹窗状态
const deleteDialog = reactive({
  visible: false,
  id: null,
  title: '',
  loading: false
})

const form = ref({
  title: '',
  content: '',
  location: '',
  startTime: '',
  endTime: '',
  contact: '',
  imageUrls: []
})

const errors = ref<Record<string, string>>({})

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

function validate() {
  const e: Record<string, string> = {}
  if (!form.value.title.trim()) e.title = '请输入活动标题'
  if (!form.value.content.trim()) e.content = '请输入活动介绍'
  if (!form.value.location.trim()) e.location = '请输入活动地点'
  if (!form.value.startTime) e.startTime = '请选择开始时间'
  if (!form.value.contact.trim()) e.contact = '请输入联系方式'
  else if (!/^1[3-9]\d{9}$/.test(form.value.contact) && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.contact)) {
    e.contact = '请输入有效的手机号码或邮箱'
  }
  if (form.value.endTime && form.value.startTime && new Date(form.value.endTime) <= new Date(form.value.startTime)) {
    e.endTime = '结束时间必须晚于开始时间'
  }
  errors.value = e
  return Object.keys(e).length === 0
}

async function handleCreate() {
  if (!validate()) return
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
      form.value = { title: '', content: '', location: '', startTime: '', endTime: '', contact: '', imageUrls: [] }
      errors.value = {}
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

// 打开删除确认弹窗
function handleDeleteActivity(item) {
  deleteDialog.id = item.id
  deleteDialog.title = item.title
  deleteDialog.loading = false
  deleteDialog.visible = true
}

// 取消删除
function cancelDelete() {
  if (deleteDialog.loading) return
  deleteDialog.visible = false
}

// 确认删除活动
async function confirmDeleteActivity() {
  if (deleteDialog.loading) return
  deleteDialog.loading = true
  try {
    const res = await activityApi.deleteActivity(deleteDialog.id)
    if (res.code === 200) {
      // 从列表中移除已删除的活动
      activities.value = activities.value.filter(a => a.id !== deleteDialog.id)
      toast.showToast('活动已删除', 'success')
    } else {
      toast.showToast(res.message || '删除失败', 'error')
    }
  } catch (e) {
    toast.showToast(e.message || '删除失败，请稍后重试', 'error')
  } finally {
    deleteDialog.visible = false
    deleteDialog.loading = false
  }
}
</script>

<style scoped>
.my-activities-page {
  min-height: 100vh;
  background: #F5F7FA;
}

.page-nav {
  position: fixed; top: 0; left: 0; right: 0; z-index: 100;
  display: flex; align-items: center; height: 56px; padding: 0 16px;
  background: #FFFFFF; border-bottom: 1px solid #E8ECF0;
}
.nav-back { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border: none; background: none; color: #333; cursor: pointer; flex-shrink: 0; }
.nav-title { flex: 1; text-align: center; font-size: 16px; font-weight: 600; color: #333; }
.nav-action { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border: none; background: none; cursor: pointer; border-radius: 4px; }

.content-area { padding-top: 56px; }

/* 创建表单 */
.create-form-overlay {
  position: fixed; inset: 0; z-index: 200;
  background: rgba(0,0,0,0.45);
  display: flex; align-items: flex-end; justify-content: center;
}
.create-form-panel {
  width: 100%; max-height: 90vh;
  background: #FFFFFF; border-radius: 16px 16px 0 0;
  overflow-y: auto; -webkit-overflow-scrolling: touch;
  animation: slideUp 0.3s ease;
}
@keyframes slideUp { from { transform: translateY(100%); } to { transform: translateY(0); } }

.form-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 20px; border-bottom: 1px solid #F0F2F5;
}
.form-header h3 { margin: 0; font-size: 17px; font-weight: 700; color: #333; }
.form-close { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border: none; background: none; color: #999; cursor: pointer; }

.form-body { padding: 20px; }

.form-group { margin-bottom: 16px; }
.form-row { display: flex; gap: 12px; }
.flex-1 { flex: 1; }

.form-label {
  display: block; font-size: 14px; font-weight: 600; color: #333; margin-bottom: 6px;
}
.required { color: #FF4D4F; }

.form-input {
  width: 100%; padding: 10px 12px; border: 1px solid #E8ECF0; border-radius: 8px;
  font-size: 14px; color: #333; background: #FAFBFC; outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.form-input:focus { border-color: var(--color-primary-500, #10b981); background: #fff; }

.form-textarea {
  width: 100%; padding: 10px 12px; border: 1px solid #E8ECF0; border-radius: 8px;
  font-size: 14px; color: #333; background: #FAFBFC; outline: none;
  resize: vertical; min-height: 80px;
  transition: border-color 0.2s;
  box-sizing: border-box;
}
.form-textarea:focus { border-color: var(--color-primary-500, #10b981); background: #fff; }

.form-error { display: block; font-size: 12px; color: #FF4D4F; margin-top: 4px; }

.form-hint { margin: 6px 0 0; font-size: 12px; color: #999; }

.submit-btn {
  width: 100%; padding: 12px; border: none; border-radius: 8px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: #fff; font-size: 16px; font-weight: 700; cursor: pointer;
  transition: opacity 0.2s;
}
.submit-btn:active { opacity: 0.85; }
.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }

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

.empty-state {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  padding: 80px 32px; color: #999;
}
.empty-state p { margin: 16px 0 24px; font-size: 15px; }
.create-first-btn {
  padding: 10px 28px; border: none; border-radius: 20px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: #fff; font-size: 14px; font-weight: 600; cursor: pointer;
}
.create-first-btn:active { transform: scale(0.96); }

/* 活动列表 */
.activity-list { padding: 12px 16px; }

.activity-item {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; background: #FFFFFF; border-radius: 12px;
  margin-bottom: 10px; cursor: pointer;
  transition: transform 0.15s;
}
.activity-item:active { transform: scale(0.98); }

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

@media (min-width: 768px) {
  .create-form-panel { max-width: 480px; margin: 0 auto; border-radius: 16px; }
  .create-form-overlay { align-items: center; }
}

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

/* 删除确认弹窗 */
.delete-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.delete-modal {
  background-color: #fff;
  border-radius: 16px;
  padding: 32px 24px;
  max-width: 320px;
  width: 100%;
  text-align: center;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  animation: slideUp 0.25s ease;
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.modal-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.modal-title {
  font-size: 18px;
  font-weight: 700;
  color: #333;
  margin: 0 0 8px 0;
}

.modal-desc {
  font-size: 14px;
  color: #999;
  margin: 0 0 28px 0;
  line-height: 1.5;
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-btn {
  flex: 1;
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  border: none;
}

.cancel-btn {
  background-color: #f5f5f5;
  color: #666;
}

.cancel-btn:active {
  background-color: #e8e8e8;
  transform: scale(0.98);
}

.confirm-delete-btn {
  background-color: #FF4D4F;
  color: white;
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.25);
}

.confirm-delete-btn:active {
  background-color: #cf1322;
  transform: scale(0.98);
}

.confirm-delete-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
