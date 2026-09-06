<template>
  <div class="address-page">
    <header class="page-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h1 class="header-title">收货地址</h1>
      <button @click="showAddForm" class="add-btn">+ 新增</button>
    </header>

    <main class="main-content">
      <div v-if="addresses.length === 0" class="empty-state">
        <div class="empty-icon">📍</div>
        <h3>暂无收货地址</h3>
        <p>添加一个收货地址，方便下单时使用</p>
        <button @click="showAddForm" class="add-first-btn">添加地址</button>
      </div>

      <div v-else class="address-list">
        <div
          v-for="(addr, index) in addresses"
          :key="index"
          class="address-card"
          :class="{ default: addr.isDefault }"
        >
          <div class="address-content" @click="selectAddress(index)">
            <div class="address-header">
              <span class="address-name">{{ addr.name }}</span>
              <span class="address-phone">{{ addr.phone }}</span>
              <span v-if="addr.isDefault" class="default-tag">默认</span>
            </div>
            <div class="address-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}</div>
          </div>
          <div class="address-actions">
            <button @click.stop="setDefault(index)" v-if="!addr.isDefault" class="action-link">设为默认</button>
            <button @click.stop="editAddress(index)" class="action-link">编辑</button>
            <button @click.stop="deleteAddress(index)" class="action-link danger">删除</button>
          </div>
        </div>
      </div>
    </main>

    <div v-if="showForm" class="modal-overlay" @click.self="showForm = false">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ isEditing ? '编辑地址' : '新增地址' }}</h3>
          <button @click="showForm = false" class="close-btn">×</button>
        </div>
        <div class="modal-body">
          <div class="form-row">
            <div class="form-group">
              <label>收货人</label>
              <input v-model="formData.name" placeholder="请输入姓名" class="form-input" />
            </div>
            <div class="form-group">
              <label>手机号</label>
              <input v-model="formData.phone" placeholder="请输入手机号" type="tel" class="form-input" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>省份</label>
              <input v-model="formData.province" placeholder="如：北京市" class="form-input" />
            </div>
            <div class="form-group">
              <label>城市</label>
              <input v-model="formData.city" placeholder="如：北京市" class="form-input" />
            </div>
          </div>
          <div class="form-group">
            <label>区/县</label>
            <input v-model="formData.district" placeholder="如：海淀区" class="form-input" />
          </div>
          <div class="form-group">
            <label>详细地址</label>
            <textarea v-model="formData.detail" placeholder="街道、楼栋、门牌号等" rows="3" class="form-textarea"></textarea>
          </div>
          <div class="form-group">
            <label class="checkbox-label">
              <input type="checkbox" v-model="formData.isDefault" />
              <span>设为默认地址</span>
            </label>
          </div>
          <button @click="saveAddress" class="save-btn">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useToast } from '../use/useToast'

const addresses = ref([])
const showForm = ref(false)
const toast = useToast()
const isEditing = ref(false)
const editIndex = ref(-1)

const formData = ref({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

function loadAddresses() {
  try {
    const stored = localStorage.getItem('addresses')
    if (stored) {
      addresses.value = JSON.parse(stored)
    }
  } catch {
    addresses.value = []
  }
}

function saveToStorage() {
  localStorage.setItem('addresses', JSON.stringify(addresses.value))
}

function showAddForm() {
  isEditing.value = false
  editIndex.value = -1
  formData.value = {
    name: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: addresses.value.length === 0
  }
  showForm.value = true
}

function editAddress(index) {
  isEditing.value = true
  editIndex.value = index
  formData.value = { ...addresses.value[index] }
  showForm.value = true
}

function saveAddress() {
  if (!formData.value.name.trim() || !formData.value.phone.trim() || !formData.value.detail.trim()) {
    toast.showToast('请填写收货人、手机号和详细地址', 'error')
    return
  }

  if (formData.value.isDefault) {
    addresses.value.forEach(a => a.isDefault = false)
  }

  if (isEditing.value) {
    addresses.value[editIndex.value] = { ...formData.value }
  } else {
    addresses.value.push({ ...formData.value })
  }

  saveToStorage()
  showForm.value = false
}

function setDefault(index) {
  addresses.value.forEach(a => a.isDefault = false)
  addresses.value[index].isDefault = true
  saveToStorage()
}

function deleteAddress(index) {
  addresses.value.splice(index, 1)
  if (addresses.value.length > 0 && !addresses.value.some(a => a.isDefault)) {
    addresses.value[0].isDefault = true
  }
  saveToStorage()
}

function selectAddress(index) {
  const addr = addresses.value[index]
  const addrStr = `${addr.province}${addr.city}${addr.district}${addr.detail}`
  localStorage.setItem('lastSelectedAddress', addrStr)
}

onMounted(() => {
  loadAddresses()
})
</script>

<style scoped>
.address-page {
  min-height: 100vh;
  background-color: #f5f5f5;
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
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
  border-radius: 50%;
}

.back-btn svg { width: 22px; height: 22px; }

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
  flex: 1;
}

.add-btn {
  padding: 6px 16px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: white;
  border: none;
  border-radius: 16px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.main-content {
  padding: 12px 16px 80px;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.empty-icon { font-size: 48px; margin-bottom: 12px; }
.empty-state h3 { font-size: 18px; color: #333; margin: 0 0 8px; }
.empty-state p { color: #999; margin-bottom: 20px; }

.add-first-btn {
  padding: 12px 32px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.address-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border-left: 4px solid transparent;
}

.address-card.default {
  border-left-color: var(--color-primary-500, #10b981);
}

.address-content {
  cursor: pointer;
}

.address-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.address-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.address-phone {
  font-size: 14px;
  color: #666;
}

.default-tag {
  padding: 2px 8px;
  background: #FFF7E6;
  color: #FA8C16;
  font-size: 11px;
  font-weight: 600;
  border-radius: 8px;
}

.address-detail {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}

.action-link {
  background: none;
  border: none;
  font-size: 13px;
  color: #1890ff;
  cursor: pointer;
  padding: 0;
}

.action-link.danger {
  color: #FF4D4F;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: flex-end;
  z-index: 1000;
}

.modal-content {
  background: #fff;
  border-radius: 16px 16px 0 0;
  width: 100%;
  max-height: 85vh;
  overflow-y: auto;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from { transform: translateY(100%); }
  to { transform: translateY(0); }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  margin: 0;
  font-size: 17px;
  color: #333;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  color: #999;
  cursor: pointer;
}

.modal-body {
  padding: 20px;
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-group {
  margin-bottom: 16px;
  flex: 1;
}

.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #e8e8e8;
  border-radius: 10px;
  font-size: 15px;
  color: #333;
  outline: none;
  background-color: #fafafa;
  box-sizing: border-box;
}

.form-input:focus,
.form-textarea:focus {
  border-color: var(--color-primary-500, #10b981);
  background-color: #fff;
}

.form-textarea {
  resize: vertical;
  min-height: 80px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.checkbox-label input {
  width: 18px;
  height: 18px;
  accent-color: var(--color-primary-500, #10b981);
}

.save-btn {
  width: 100%;
  padding: 14px;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  margin-top: 8px;
}
</style>
