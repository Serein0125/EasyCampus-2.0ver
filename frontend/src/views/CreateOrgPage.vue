<template>
  <div class="create-org-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <span class="nav-title">创建组织</span>
    </header>

    <div class="step-indicator">
      <div v-for="s in steps" :key="s.step" class="step-dot" :class="{ active: s.step <= currentStep, done: s.step < currentStep }">
        <span class="step-num">{{ s.step < currentStep ? '✓' : s.step }}</span>
        <span class="step-lbl">{{ s.label }}</span>
      </div>
    </div>

    <main class="form-content">
      <el-form
        v-if="currentStep === 1"
        ref="step1Ref"
        :model="form"
        :rules="step1Rules"
        label-position="top"
        @submit.prevent
      >
        <el-form-item label="组织名称" prop="name">
          <el-input v-model="form.name" maxlength="100" placeholder="给你的组织取一个名字" />
        </el-form-item>
        <el-form-item label="组织类型" prop="orgType" required>
          <div class="type-grid">
            <button v-for="t in orgTypes" :key="t.value" type="button" class="type-btn" :class="{ selected: form.orgType === t.value }" @click="form.orgType = t.value">
              <span class="type-icon">{{ t.icon }}</span><span>{{ t.label }}</span>
            </button>
          </div>
        </el-form-item>
        <el-form-item label="加入方式" prop="joinType">
          <el-radio-group v-model="form.joinType">
            <el-radio value="INVITE">仅邀请</el-radio>
            <el-radio value="APPLY">可申请加入</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template v-if="currentStep === 2">
        <div class="form-group">
          <label>组织简介</label>
          <el-input v-model="form.description" type="textarea" :rows="4" maxlength="1000" placeholder="介绍一下你的组织..." />
        </div>
        <div class="form-group">
          <label>联系邮箱</label>
          <el-input v-model="form.contactEmail" placeholder="example@school.edu" />
        </div>
        <div class="form-group">
          <label>网站/主页</label>
          <el-input v-model="form.websiteUrl" placeholder="https://example.com" />
        </div>
        <div class="form-group">
          <label>所在地/校区</label>
          <el-input v-model="form.location" placeholder="例如: 清华大学" />
        </div>
      </template>

      <template v-if="currentStep === 3">
        <el-card class="preview-card" shadow="never">
          <template #header>
            <h3 class="preview-title">📋 信息确认</h3>
          </template>
          <div class="preview-row"><span>名称</span><strong>{{ form.name || '(未填写)' }}</strong></div>
          <div class="preview-row"><span>类型</span><strong>{{ orgTypeLabel }}</strong></div>
          <div class="preview-row"><span>加入方式</span><strong>{{ form.joinType === 'INVITE' ? '仅邀请' : '可申请加入' }}</strong></div>
          <div class="preview-row"><span>简介</span><strong>{{ form.description || '(未填写)' }}</strong></div>
          <div class="preview-row"><span>邮箱</span><strong>{{ form.contactEmail || '(未填写)' }}</strong></div>
          <div class="preview-row"><span>所在地</span><strong>{{ form.location || '(未填写)' }}</strong></div>
        </el-card>
      </template>
    </main>

    <footer class="step-actions">
      <el-button v-if="currentStep > 1" size="large" @click="currentStep--">上一步</el-button>
      <el-button v-if="currentStep < 3" size="large" type="primary" @click="nextStep">下一步</el-button>
      <el-button v-if="currentStep === 3" size="large" type="primary" :loading="submitting" @click="submitForm">
        {{ submitting ? '提交中...' : '确认创建' }}
      </el-button>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { organizationApi } from '../services/api'
import { useToast } from '../use/useToast'

const router = useRouter()
const toast = useToast()
const currentStep = ref(1)
const submitting = ref(false)
const steps = [{ step: 1, label: '基本信息' }, { step: 2, label: '详细资料' }, { step: 3, label: '确认提交' }]

const orgTypes = [
  { value: 'CLUB', label: '兴趣社团', icon: '🎯' },
  { value: 'STUDENT_ORG', label: '学生组织', icon: '🏛️' },
  { value: 'BUSINESS', label: '商业团队', icon: '💼' },
  { value: 'PERSONAL', label: '个人品牌', icon: '👤' }
]

const form = reactive({ name: '', orgType: 'CLUB', joinType: 'APPLY', description: '', contactEmail: '', websiteUrl: '', location: '' })
const step1Ref = ref()

const step1Rules = {
  name: [
    { required: true, message: '请输入组织名称', trigger: 'blur' }
  ],
  joinType: [
    { required: true, message: '请选择加入方式', trigger: 'change' }
  ]
}

const orgTypeLabel = computed(() => orgTypes.find(t => t.value === form.orgType)?.label || '')

async function nextStep() {
  if (currentStep.value === 1) {
    const valid = await step1Ref.value?.validate().catch(() => false)
    if (!valid) return
  }
  currentStep.value++
}

async function submitForm() {
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await organizationApi.create({ ...form })
    if (res.code === 200) {
      toast.showToast('组织创建成功！等待审核通过后即可使用。', 'success')
      setTimeout(() => router.push('/orgs/my'), 600)
    } else {
      toast.showToast(res.message || '创建失败，请稍后重试', 'error')
    }
  } catch (e) {
    toast.showToast(e.message || '创建失败，请检查网络连接', 'error')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-org-page { min-height: 100vh; background: #F5F7FA; }
.page-nav { position: sticky; top: 0; z-index: 100; display: flex; align-items: center; height: 56px; padding: 0 16px; background: #fff; border-bottom: 1px solid #E8ECF0; }
.nav-back { display: flex; align-items: center; justify-content: center; width: 32px; height: 32px; border: none; background: none; color: #333; cursor: pointer; flex-shrink: 0; }
.nav-title { flex: 1; text-align: center; font-size: 16px; font-weight: 600; margin-right: 32px; }

.step-indicator { display: flex; justify-content: center; gap: 40px; padding: 20px 16px; background: #fff; }
.step-dot { display: flex; flex-direction: column; align-items: center; gap: 4px; opacity: 0.3; transition: opacity 0.3s; }
.step-dot.active { opacity: 1; }
.step-num { width: 28px; height: 28px; border-radius: 50%; background: #E8ECF0; color: #999; display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: 700; }
.step-dot.active .step-num { background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399)); color: #fff; }
.step-dot.done .step-num { background: #52c41a; color: #fff; }
.step-lbl { font-size: 11px; color: #666; }

.form-content { padding: 16px; max-width: 600px; margin: 0 auto; }
.form-group { margin-bottom: 20px; }
.form-group label { display: block; font-size: 14px; font-weight: 600; color: #333; margin-bottom: 8px; }

.type-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; width: 100%; }
.type-btn { flex-direction: column; gap: 4px; padding: 14px 10px; border: 2px solid #E8ECF0; border-radius: 12px; background: #fff; cursor: pointer; font-size: 13px; font-weight: 500; color: #666; transition: all 0.2s; display: flex; align-items: center; justify-content: center; font-family: inherit; }
.type-btn.selected { border-color: var(--color-primary-500, #10b981); background: #FFF7E6; color: var(--color-primary-500, #10b981); }
.type-icon { font-size: 22px; }

.preview-card { border-radius: 12px; }
.preview-title { margin: 0; font-size: 16px; }
.preview-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f5f5f5; font-size: 14px; }
.preview-row span { color: #999; }

.step-actions { display: flex; gap: 12px; padding: 16px; justify-content: center; }
</style>
