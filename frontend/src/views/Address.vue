<template>
  <div class="address-page">
    <header class="page-header">
      <button @click="$router.back()" class="back-btn">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <polyline points="15,18 9,12 15,6"/>
        </svg>
      </button>
      <h1 class="header-title">收货地址</h1>
      <el-button type="primary" size="small" round @click="showAddForm">+ 新增</el-button>
    </header>

    <main class="main-content">
      <el-empty
        v-if="addresses.length === 0"
        description="暂无收货地址，添加一个方便下单时使用"
      >
        <el-button type="primary" @click="showAddForm">添加地址</el-button>
      </el-empty>

      <div v-else class="address-list">
        <el-card
          v-for="(addr, index) in addresses"
          :key="index"
          shadow="hover"
          class="address-card"
          :class="{ default: addr.isDefault }"
        >
          <div class="address-content" @click="selectAddress(index)">
            <div class="address-header">
              <span class="address-name">{{ addr.name }}</span>
              <span class="address-phone">{{ addr.phone }}</span>
              <el-tag v-if="addr.isDefault" type="warning" size="small" effect="light">默认</el-tag>
            </div>
            <div class="address-detail">{{ addr.province }}{{ addr.city }}{{ addr.district }}{{ addr.detail }}</div>
          </div>
          <div class="address-actions">
            <el-button v-if="!addr.isDefault" link type="primary" size="small" @click.stop="setDefault(index)">设为默认</el-button>
            <el-button link type="primary" size="small" @click.stop="editAddress(index)">编辑</el-button>
            <el-button link type="danger" size="small" @click.stop="deleteAddress(index)">删除</el-button>
          </div>
        </el-card>
      </div>
    </main>

    <el-dialog
      v-model="showForm"
      :title="isEditing ? '编辑地址' : '新增地址'"
      width="480px"
      append-to-body
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-position="top"
        @submit.prevent
      >
        <div class="form-row">
          <el-form-item label="收货人" prop="name" class="form-item-half">
            <el-input v-model="formData.name" placeholder="请输入姓名" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone" class="form-item-half">
            <el-input v-model="formData.phone" placeholder="请输入手机号" maxlength="11" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="省份" prop="province" class="form-item-half">
            <el-input v-model="formData.province" placeholder="如：北京市" />
          </el-form-item>
          <el-form-item label="城市" prop="city" class="form-item-half">
            <el-input v-model="formData.city" placeholder="如：北京市" />
          </el-form-item>
        </div>
        <el-form-item label="区/县" prop="district">
          <el-input v-model="formData.district" placeholder="如：海淀区" />
        </el-form-item>
        <el-form-item label="详细地址" prop="detail">
          <el-input v-model="formData.detail" type="textarea" :rows="3" placeholder="街道、楼栋、门牌号等" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="formData.isDefault">设为默认地址</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showForm = false">取消</el-button>
        <el-button type="primary" @click="saveAddress">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useToast } from '../use/useToast'

const addresses = ref([])
const showForm = ref(false)
const toast = useToast()
const isEditing = ref(false)
const editIndex = ref(-1)
const formRef = ref()

const formData = reactive({
  name: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false
})

const formRules = {
  name: [{ required: true, message: '请输入收货人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
  ],
  detail: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

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
  Object.assign(formData, {
    name: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: addresses.value.length === 0
  })
  formRef.value?.clearValidate()
  showForm.value = true
}

function editAddress(index) {
  isEditing.value = true
  editIndex.value = index
  // 先重置为默认值再合并：Object.assign 只覆盖源对象已有的键，
  // 旧数据若缺某个字段（如 isDefault）会沿用上一条地址的残留值
  Object.assign(
    formData,
    { name: '', phone: '', province: '', city: '', district: '', detail: '', isDefault: false },
    addresses.value[index]
  )
  formRef.value?.clearValidate()
  showForm.value = true
}

async function saveAddress() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (formData.isDefault) {
    addresses.value.forEach(a => a.isDefault = false)
  }

  if (isEditing.value) {
    addresses.value[editIndex.value] = { ...formData }
  } else {
    addresses.value.push({ ...formData })
  }

  saveToStorage()
  showForm.value = false
  toast.showToast(isEditing.value ? '地址已更新' : '地址已添加', 'success')
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
  toast.showToast('地址已删除', 'success')
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
  border: none;
  background: none;
  border-radius: 50%;
  cursor: pointer;
}

.back-btn svg { width: 22px; height: 22px; }

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0;
  flex: 1;
}

.main-content {
  padding: 12px 16px 80px;
}

.address-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 720px;
  margin: 0 auto;
}

.address-card {
  border-radius: 12px;
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

.address-detail {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.address-actions {
  display: flex;
  gap: 4px;
  margin-top: 12px;
  padding-top: 8px;
  border-top: 1px solid #f5f5f5;
}

.form-row {
  display: flex;
  gap: 12px;
}

.form-item-half {
  flex: 1;
}
</style>
