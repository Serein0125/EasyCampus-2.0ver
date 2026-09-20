<template>
  <div class="settings">
    <main class="main-content">
      <div class="container">
        <header class="page-header">
          <button class="back-btn" @click="$router.back()">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
          </button>
          <div class="header-text">
            <h1 class="page-title">设置</h1>
            <p class="page-subtitle">管理您的账户偏好和安全设置</p>
          </div>
        </header>

        <div class="settings-container">
          <!-- 账户安全 -->
          <el-card shadow="never" class="settings-card">
            <template #header>
              <h3 class="card-title">
                <span class="card-icon">🔒</span>
                账户安全
              </h3>
            </template>

            <div class="setting-item">
              <div class="setting-info">
                <h4>修改密码</h4>
                <p>定期更改密码可以保护账户安全</p>
              </div>
              <el-button @click="showPasswordModal = true">修改</el-button>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <h4>两步验证</h4>
                <p>为账户添加额外的安全层（开发中）</p>
              </div>
              <el-button disabled>未启用</el-button>
            </div>
          </el-card>

          <!-- 个人资料 -->
          <el-card shadow="never" class="settings-card">
            <template #header>
              <h3 class="card-title">
                <span class="card-icon">🏫</span>
                个人资料
              </h3>
            </template>
          </el-card>

          <!-- 通知设置 -->
          <el-card shadow="never" class="settings-card">
            <template #header>
              <h3 class="card-title">
                <span class="card-icon">🔔</span>
                通知设置
              </h3>
            </template>

            <div class="setting-item">
              <div class="setting-info">
                <h4>新消息通知</h4>
                <p>当收到新消息时发送通知</p>
              </div>
              <el-switch v-model="notifications.newMessage" />
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <h4>订单状态更新</h4>
                <p>订单状态变更时通知您</p>
              </div>
              <el-switch v-model="notifications.orderUpdate" />
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <h4>促销活动</h4>
                <p>接收平台促销和活动信息</p>
              </div>
              <el-switch v-model="notifications.promotions" />
            </div>
          </el-card>

          <!-- 隐私设置 -->
          <el-card shadow="never" class="settings-card">
            <template #header>
              <h3 class="card-title">
                <span class="card-icon">🛡️</span>
                隐私设置
              </h3>
            </template>

            <div class="setting-item">
              <div class="setting-info">
                <h4>个人资料可见性</h4>
                <p>控制其他用户能否查看您的资料</p>
              </div>
              <el-select v-model="privacy.profileVisibility" style="width: 160px">
                <el-option value="public" label="公开" />
                <el-option value="registered" label="仅注册用户" />
                <el-option value="private" label="私密" />
              </el-select>
            </div>

            <div class="setting-item">
              <div class="setting-info">
                <h4>在线状态显示</h4>
                <p>让其他用户看到您是否在线</p>
              </div>
              <el-switch v-model="privacy.showOnlineStatus" />
            </div>
          </el-card>

          <!-- 危险区域 -->
          <el-card shadow="never" class="settings-card danger-zone">
            <template #header>
              <h3 class="card-title danger-title">
                <span class="card-icon">⚠️</span>
                危险区域
              </h3>
            </template>

            <div class="setting-item">
              <div class="setting-info">
                <h4>注销账户</h4>
                <p>永久删除您的账户和所有数据，此操作不可撤销</p>
              </div>
              <el-button type="danger" @click="confirmDeleteAccount">注销账户</el-button>
            </div>
          </el-card>

          <!-- 保存按钮 -->
          <div class="save-section">
            <el-button type="primary" size="large" :loading="saving" @click="saveSettings">
              {{ saving ? '保存中...' : '保存所有设置' }}
            </el-button>
            <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
          </div>
        </div>
      </div>
    </main>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="showPasswordModal" title="修改密码" width="440px" append-to-body>
      <el-form
        ref="passwordFormRef"
        :model="passwordData"
        :rules="passwordRules"
        label-position="top"
        @submit.prevent="changePassword"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input v-model="passwordData.currentPassword" type="password" show-password />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordData.newPassword" type="password" show-password />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordData.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showPasswordModal = false">取消</el-button>
        <el-button type="primary" :loading="changingPassword" @click="changePassword">
          {{ changingPassword ? '修改中...' : '确认修改' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useToast } from '../use/useToast'
import { userApi } from '../services/api'

const toast = useToast()

const saving = ref(false)
const successMessage = ref('')

// 通知设置
const notifications = ref({
  newMessage: true,
  orderUpdate: true,
  promotions: false
})

// 隐私设置
const privacy = ref({
  profileVisibility: 'public',
  showOnlineStatus: true
})

// 密码修改
const showPasswordModal = ref(false)
const changingPassword = ref(false)
const passwordFormRef = ref()
const passwordData = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const passwordRules = {
  currentPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
      if (value !== passwordData.newPassword) callback(new Error('两次输入的密码不一致'))
      else callback()
    }, trigger: 'blur' }
  ]
}

function saveSettings() {
  saving.value = true

  // TODO: 通知设置和隐私设置暂无对应后端API，当前为模拟保存
  setTimeout(() => {
    successMessage.value = '设置已成功保存！'
    saving.value = false

    setTimeout(() => {
      successMessage.value = ''
    }, 3000)
  }, 1000)
}

async function changePassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  changingPassword.value = true

  try {
    await userApi.changePassword({
      oldPassword: passwordData.currentPassword,
      newPassword: passwordData.newPassword
    })
    toast.showToast('密码修改成功！', 'success')

    setTimeout(() => {
      showPasswordModal.value = false
      passwordData.currentPassword = ''
      passwordData.newPassword = ''
      passwordData.confirmPassword = ''
    }, 1500)
  } catch (error) {
    toast.showToast(error.message || '密码修改失败，请稍后重试', 'error')
  } finally {
    changingPassword.value = false
  }
}

async function confirmDeleteAccount() {
  const ok = await toast.showConfirm('确定要注销账户吗？此操作不可撤销，您的所有数据将被永久删除！')
  if (ok) {
    toast.showToast('账户注销功能需要二次确认，为了安全起见请联系客服处理', 'error')
  }
}
</script>

<style scoped>
.settings {
  min-height: 100vh;
  background-color: #f5f5f5;
}

.main-content {
  padding: 2rem 0;
}

.container {
  max-width: 900px;
  margin: 0 auto;
  padding: 0 1rem;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 2rem;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #fff;
  border: 1px solid var(--color-border-light, #e5e7eb);
  color: #333;
  cursor: pointer;
  flex-shrink: 0;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: var(--color-primary-400, #34d399);
  color: var(--color-primary-600, #059669);
}

.back-btn svg { width: 22px; height: 22px; }

.header-text {
  flex: 1;
}

.page-title {
  font-size: 1.75rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.page-subtitle {
  color: #666;
  font-size: 1rem;
}

.settings-container {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.settings-card {
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.danger-zone {
  border: 2px solid #ff6b6b;
}

.card-title {
  font-size: 1.25rem;
  color: #333;
  margin: 0;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.card-icon {
  font-size: 1.5rem;
}

.danger-title {
  color: #dc3545;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.25rem 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-info h4 {
  font-size: 1rem;
  color: #333;
  margin-bottom: 0.35rem;
}

.setting-info p {
  font-size: 0.9rem;
  color: #666;
  margin: 0;
}

.save-section {
  text-align: center;
  padding: 2rem 0;
}

.success-message {
  margin-top: 1rem;
  color: #28a745;
  font-weight: 500;
}
</style>
