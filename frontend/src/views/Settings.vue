<template>
  <div class="settings">
    <NavBar />
    
    <main class="main-content">
      <div class="container">
        <div class="page-header">
          <h1 class="page-title">设置</h1>
          <p class="page-subtitle">管理您的账户偏好和安全设置</p>
        </div>
        
        <div class="settings-container">
          <!-- 账户安全 -->
          <div class="settings-card">
            <h3 class="card-title">
              <span class="card-icon">🔒</span>
              账户安全
            </h3>
            
            <div class="setting-item">
              <div class="setting-info">
                <h4>修改密码</h4>
                <p>定期更改密码可以保护账户安全</p>
              </div>
              <button @click="showPasswordModal = true" class="btn btn-outline">修改</button>
            </div>
            
            <div class="setting-item">
              <div class="setting-info">
                <h4>两步验证</h4>
                <p>为账户添加额外的安全层（开发中）</p>
              </div>
              <button class="btn btn-outline" disabled>未启用</button>
            </div>
          </div>

          <!-- 个人资料 -->
          <div class="settings-card">
            <h3 class="card-title">
              <span class="card-icon">🏫</span>
              个人资料
            </h3>

          </div>

          <!-- 通知设置 -->
          <div class="settings-card">
            <h3 class="card-title">
              <span class="card-icon">🔔</span>
              通知设置
            </h3>
            
            <div class="setting-item">
              <div class="setting-info">
                <h4>新消息通知</h4>
                <p>当收到新消息时发送通知</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="notifications.newMessage" />
                <span class="slider"></span>
              </label>
            </div>
            
            <div class="setting-item">
              <div class="setting-info">
                <h4>订单状态更新</h4>
                <p>订单状态变更时通知您</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="notifications.orderUpdate" checked />
                <span class="slider"></span>
              </label>
            </div>
            
            <div class="setting-item">
              <div class="setting-info">
                <h4>促销活动</h4>
                <p>接收平台促销和活动信息</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="notifications.promotions" />
                <span class="slider"></span>
              </label>
            </div>
          </div>
          
          <!-- 隐私设置 -->
          <div class="settings-card">
            <h3 class="card-title">
              <span class="card-icon">🛡️</span>
              隐私设置
            </h3>
            
            <div class="setting-item">
              <div class="setting-info">
                <h4>个人资料可见性</h4>
                <p>控制其他用户能否查看您的资料</p>
              </div>
              <select v-model="privacy.profileVisibility" class="form-select">
                <option value="public">公开</option>
                <option value="registered">仅注册用户</option>
                <option value="private">私密</option>
              </select>
            </div>
            
            <div class="setting-item">
              <div class="setting-info">
                <h4>在线状态显示</h4>
                <p>让其他用户看到您是否在线</p>
              </div>
              <label class="toggle-switch">
                <input type="checkbox" v-model="privacy.showOnlineStatus" checked />
                <span class="slider"></span>
              </label>
            </div>
          </div>
          
          <!-- 危险区域 -->
          <div class="settings-card danger-zone">
            <h3 class="card-title danger-title">
              <span class="card-icon">⚠️</span>
              危险区域
            </h3>
            
            <div class="setting-item">
              <div class="setting-info">
                <h4>注销账户</h4>
                <p>永久删除您的账户和所有数据，此操作不可撤销</p>
              </div>
              <button @click="confirmDeleteAccount" class="btn btn-danger">注销账户</button>
            </div>
          </div>
          
          <!-- 保存按钮 -->
          <div class="save-section">
            <button @click="saveSettings" class="btn btn-primary btn-lg" :disabled="saving">
              {{ saving ? '保存中...' : '保存所有设置' }}
            </button>
            <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
          </div>
        </div>
      </div>
    </main>
    
    <!-- 修改密码弹窗 -->
    <div v-if="showPasswordModal" class="modal-overlay" @click.self="showPasswordModal = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>修改密码</h3>
          <button @click="showPasswordModal = false" class="close-btn">×</button>
        </div>
        
        <form @submit.prevent="changePassword" class="modal-body">
          <div class="form-group">
            <label>当前密码</label>
            <input type="password" v-model="passwordData.currentPassword" required />
          </div>
          
          <div class="form-group">
            <label>新密码</label>
            <input type="password" v-model="passwordData.newPassword" required minlength="6" />
          </div>
          
          <div class="form-group">
            <label>确认新密码</label>
            <input type="password" v-model="passwordData.confirmPassword" required />
          </div>
          
          <div v-if="passwordError" class="alert alert-error">{{ passwordError }}</div>
          <div v-if="passwordSuccess" class="alert alert-success">{{ passwordSuccess }}</div>
          
          <div class="modal-actions">
            <button type="button" @click="showPasswordModal = false" class="btn btn-outline">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="changingPassword">
              {{ changingPassword ? '修改中...' : '确认修改' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useToast } from '../use/useToast'
import { userApi } from '../services/api'
import NavBar from '../components/NavBar.vue'

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
const passwordError = ref('')
const passwordSuccess = ref('')
const passwordData = ref({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

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
  passwordError.value = ''
  passwordSuccess.value = ''

  if (passwordData.value.newPassword !== passwordData.value.confirmPassword) {
    passwordError.value = '两次输入的密码不一致'
    return
  }

  if (passwordData.value.newPassword.length < 6) {
    passwordError.value = '密码长度至少6位'
    return
  }

  changingPassword.value = true

  try {
    await userApi.changePassword({
      oldPassword: passwordData.value.currentPassword,
      newPassword: passwordData.value.newPassword
    })
    toast.showToast('密码修改成功！', 'success')

    setTimeout(() => {
      showPasswordModal.value = false
      passwordSuccess.value = ''
      passwordData.value = { currentPassword: '', newPassword: '', confirmPassword: '' }
    }, 1500)
  } catch (error) {
    passwordError.value = error.message || '密码修改失败，请稍后重试'
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
  text-align: center;
  margin-bottom: 2rem;
}

.page-title {
  font-size: 2rem;
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
  background: white;
  border-radius: 12px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.danger-zone {
  border: 2px solid #ff6b6b;
}

.card-title {
  font-size: 1.25rem;
  color: #333;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #f0f0f0;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.card-icon {
  font-size: 1.5rem;
}

.danger-title {
  color: #dc3545;
  border-bottom-color: #ffcdd2;
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

.btn {
  padding: 0.6rem 1.25rem;
  border-radius: 8px;
  font-size: 0.95rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-outline {
  background: white;
  border: 1px solid #ddd;
  color: #555;
}

.btn-outline:hover:not(:disabled) {
  border-color: #4CAF50;
  color: #4CAF50;
}

.btn-primary {
  background: #4CAF50;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #45a049;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-lg {
  padding: 0.875rem 2.5rem;
  font-size: 1rem;
  font-weight: 600;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 26px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: 0.3s;
  border-radius: 26px;
}

.slider:before {
  position: absolute;
  content: "";
  height: 20px;
  width: 20px;
  left: 3px;
  bottom: 3px;
  background-color: white;
  transition: 0.3s;
  border-radius: 50%;
}

input:checked + .slider {
  background-color: #4CAF50;
}

input:checked + .slider:before {
  transform: translateX(24px);
}

.form-select {
  padding: 0.6rem 1rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.95rem;
  min-width: 140px;
}

.form-select:focus {
  outline: none;
  border-color: #4CAF50;
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

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  padding: 1.5rem;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
  line-height: 1;
}

.modal-body {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #555;
}

.form-group input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 1rem;
}

.form-group input:focus {
  outline: none;
  border-color: #4CAF50;
}

.alert {
  padding: 0.75rem;
  border-radius: 8px;
  font-size: 0.9rem;
}

.alert-error {
  background: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

.alert-success {
  background: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.modal-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1rem;
}
</style>
