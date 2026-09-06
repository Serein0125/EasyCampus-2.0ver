<template>
  <div class="my-orgs-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <span class="nav-title">我的组织</span>
      <button class="nav-create" @click="$router.push('/orgs/create')">+ 创建</button>
    </header>

    <div v-if="loading" class="loading-state"><div v-for="i in 3" :key="i" class="skeleton-card"></div></div>
    <div v-else-if="orgs.length === 0" class="empty-state">
      <div class="empty-icon">🏛️</div>
      <p>你还未加入任何组织</p>
      <div class="empty-actions">
        <button class="create-btn" @click="$router.push('/orgs/create')">创建组织</button>
        <button class="invite-btn" @click="$router.push('/orgs/invitations')">我的邀请</button>
        <button class="discover-btn" @click="$router.push('/orgs/discover')">发现组织</button>
      </div>
    </div>
    <main v-else class="org-list">
      <div v-for="org in orgs" :key="org.id" class="org-card" @click="$router.push(`/orgs/${org.id}`)">
        <div class="org-logo" :style="{ backgroundColor: randomColor(org.id) }">{{ org.name.charAt(0) }}</div>
        <div class="org-info">
          <h3>{{ org.name }}</h3>
          <p>{{ org.description || '暂无简介' }}</p>
          <div class="org-meta">
            <span class="org-type">{{ typeLabel(org.orgType) }}</span>
            <span :class="['org-status', org.status === 'PENDING' ? 'status-pending' : 'status-ok']">{{ org.status === 'APPROVED' ? '已通过' : org.status === 'PENDING' ? '审核中' : org.status }}</span>
            <span class="org-count">{{ org.memberCount || 0 }} 成员</span>
          </div>
        </div>
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9,18 15,12 9,6"/></svg>
      </div>
      <button class="invitation-card-entry" @click="$router.push('/orgs/invitations')">
        <div class="invitation-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#1890FF" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
        </div>
        <div class="invitation-info">
          <span class="invitation-title">我的邀请</span>
          <span class="invitation-desc">查看和处理组织邀请</span>
        </div>
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9,18 15,12 9,6"/></svg>
      </button>
      <button class="discover-card" @click="$router.push('/orgs/discover')">
        <div class="discover-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#FF6A00" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
        </div>
        <div class="discover-info">
          <span class="discover-title">发现组织</span>
          <span class="discover-desc">浏览并加入更多组织</span>
        </div>
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9,18 15,12 9,6"/></svg>
      </button>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { organizationApi } from '../services/api'
import { useToast } from '../use/useToast'

const toast = useToast()
const orgs = ref([])
const loading = ref(true)

const colors = ['#FF6A00','#1890FF','#52c41a','#722ED1','#EB2F96','#13C2C2','#FADB14','#FA541C','#2F54EB','#A0D911']

onMounted(async () => {
  try {
    const res = await organizationApi.getMyOrgs()
    if (res.code === 200) orgs.value = res.data || []
  } catch {
    toast.showToast('加载组织列表失败，请稍后重试', 'error')
    orgs.value = []
  } finally { loading.value = false }
})

function typeLabel(t) { return { CLUB: '社团', STUDENT_ORG: '学生组织', BUSINESS: '商业', PERSONAL: '个人' }[t] || t }
function randomColor(id) { return colors[Math.abs(Number(id)) % colors.length] }
</script>

<style scoped>
.my-orgs-page { min-height: 100vh; background: #F5F7FA; }
.page-nav { position: sticky; top: 0; z-index: 100; display: flex; align-items: center; height: 56px; padding: 0 16px; background: #fff; border-bottom: 1px solid #E8ECF0; }
.nav-back { display: flex; align-items: center; width: 32px; height: 32px; border: none; background: none; color: #333; cursor: pointer; }
.nav-title { flex: 1; text-align: center; font-size: 16px; font-weight: 600; }
.nav-create { padding: 6px 14px; border-radius: 16px; border: none; background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399)); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; }
.loading-state { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.skeleton-card { height: 80px; background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: 12px; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
.empty-state { text-align: center; padding: 80px 32px; }
.empty-icon { font-size: 64px; margin-bottom: 16px; opacity: 0.5; }
.empty-actions { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; margin-top: 20px; }
.create-btn { padding: 12px 32px; border-radius: 20px; border: none; background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399)); color: #fff; font-size: 15px; font-weight: 600; cursor: pointer; }
.invite-btn { padding: 10px 28px; border-radius: 20px; border: 1px solid #1890FF; background: #fff; color: #1890FF; font-size: 14px; cursor: pointer; }
.discover-btn { padding: 10px 28px; border-radius: 20px; border: 1px solid #DDE1E6; background: #fff; color: #666; font-size: 14px; cursor: pointer; }
.org-list { padding: 12px 16px; display: flex; flex-direction: column; gap: 10px; }
.org-card { display: flex; align-items: center; gap: 14px; padding: 16px; background: #fff; border-radius: 12px; cursor: pointer; transition: transform 0.15s; }
.org-card:active { transform: scale(0.98); }
.org-logo { width: 52px; height: 52px; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; font-weight: 700; flex-shrink: 0; }
.org-info { flex: 1; min-width: 0; }
.org-info h3 { margin: 0 0 4px; font-size: 16px; font-weight: 600; color: #333; }
.org-info p { margin: 0 0 6px; font-size: 13px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.org-meta { display: flex; gap: 12px; }
.org-type { padding: 2px 8px; background: #FFF7E6; color: var(--color-primary-500, #10b981); font-size: 11px; border-radius: 4px; }
.org-status { padding: 2px 8px; font-size: 11px; border-radius: 4px; }
.status-pending { background: #FFF7E6; color: #FA8C16; }
.status-ok { background: #F6FFED; color: #52C41A; }
.org-count { font-size: 12px; color: #999; }

.discover-card {
  display: flex; align-items: center; gap: 14px; padding: 16px;
  background: #fff; border-radius: 12px; cursor: pointer;
  border: 1.5px dashed #DDE1E6; transition: all 0.15s; width: 100%;
  text-align: left;
}
.discover-card:active { transform: scale(0.98); }
.discover-card:hover { border-color: var(--color-primary-500, #10b981); background: #FFFBF5; }
.discover-icon {
  width: 52px; height: 52px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  background: #FFF7E6; flex-shrink: 0;
}
.discover-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.discover-title { font-size: 15px; font-weight: 600; color: #333; }
.discover-desc { font-size: 12px; color: #999; }

.invitation-card-entry {
  display: flex; align-items: center; gap: 14px; padding: 16px;
  background: #fff; border-radius: 12px; cursor: pointer;
  border: 1.5px dashed #DDE1E6; transition: all 0.15s; width: 100%;
  text-align: left; font-family: inherit;
}
.invitation-card-entry:active { transform: scale(0.98); }
.invitation-card-entry:hover { border-color: #1890FF; background: #F0F7FF; }
.invitation-icon {
  width: 52px; height: 52px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  background: #E6F4FF; flex-shrink: 0;
}
.invitation-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.invitation-title { font-size: 15px; font-weight: 600; color: #333; }
.invitation-desc { font-size: 12px; color: #999; }
</style>
