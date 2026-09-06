<template>
  <div class="invitations-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <span class="nav-title">我的邀请</span>
    </header>

    <div v-if="loading" class="loading-state">
      <div v-for="i in 3" :key="i" class="skeleton-card"></div>
    </div>

    <div v-else-if="invitations.length === 0" class="empty-state">
      <div class="empty-icon">📨</div>
      <p>暂无待处理的邀请</p>
      <button class="discover-btn" @click="$router.push('/orgs/discover')">发现组织</button>
    </div>

    <main v-else class="invitation-list">
      <div v-for="inv in invitations" :key="inv.id" class="invitation-card">
        <div class="inv-org-logo" :style="{ backgroundColor: randomColor(inv.orgId) }">
          {{ (inv.orgName || 'O').charAt(0) }}
        </div>
        <div class="inv-info">
          <h3>{{ inv.orgName || '组织 ' + inv.orgId }}</h3>
          <p class="inv-meta">
            <span>邀请码: {{ inv.inviteCode }}</span>
            <span class="inv-status" :class="inv.status === 'PENDING' ? 'status-pending' : 'status-done'">
              {{ inv.status === 'PENDING' ? '待处理' : inv.status === 'ACCEPTED' ? '已接受' : '已拒绝' }}
            </span>
          </p>
          <p class="inv-date">发送时间: {{ formatTime(inv.createdAt) }}</p>
        </div>
        <div v-if="inv.status === 'PENDING'" class="inv-actions">
          <button class="btn-accept" @click="acceptInvitation(inv)" :disabled="inv._processing">
            {{ inv._processing ? '处理中...' : '接受' }}
          </button>
          <button class="btn-reject" @click="rejectInvitation(inv)" :disabled="inv._processing">拒绝</button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { organizationApi } from '../services/api'
import { useToast } from '../use/useToast'

const toast = useToast()
const invitations = ref([])
const loading = ref(true)

const colors = ['#FF6A00','#1890FF','#52c41a','#722ED1','#EB2F96','#13C2C2','#FADB14','#FA541C','#2F54EB','#A0D911']

onMounted(async () => {
  try {
    const res = await organizationApi.getMyInvitations()
    if (res.code === 200) {
      const rawList = res.data || []
      // 并行获取每个邀请对应的组织名称
      const enriched = await Promise.all(
        rawList.map(async (inv) => {
          try {
            const orgRes = await organizationApi.getDetail(inv.orgId)
            return { ...inv, orgName: orgRes?.data?.name || '', _processing: false }
          } catch {
            return { ...inv, orgName: '', _processing: false }
          }
        })
      )
      invitations.value = enriched
    }
  } catch {
    toast.showToast('加载邀请列表失败', 'error')
  } finally {
    loading.value = false
  }
})

async function acceptInvitation(inv) {
  inv._processing = true
  try {
    const res = await organizationApi.acceptInvite(inv.inviteCode)
    if (res.code === 200) {
      inv.status = 'ACCEPTED'
      toast.showToast('已成功加入组织', 'success')
    } else {
      toast.showToast(res.message || '接受邀请失败', 'error')
    }
  } catch (e) {
    toast.showToast(e?.message || '接受邀请失败', 'error')
  } finally {
    inv._processing = false
  }
}

async function rejectInvitation(inv) {
  const ok = await toast.showConfirm('确定拒绝该邀请？')
  if (!ok) return
  inv._processing = true
  try {
    const res = await organizationApi.rejectInvite(inv.inviteCode)
    if (res.code === 200) {
      inv.status = 'REJECTED'
      toast.showToast('已拒绝邀请')
    } else {
      toast.showToast(res.message || '操作失败', 'error')
    }
  } catch (e) {
    toast.showToast(e?.message || '操作失败', 'error')
  } finally {
    inv._processing = false
  }
}

function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
function randomColor(id) { return colors[Math.abs(Number(id)) % colors.length] }
</script>

<style scoped>
.invitations-page { min-height: 100vh; background: #F5F7FA; }
.page-nav { position: sticky; top: 0; z-index: 100; display: flex; align-items: center; height: 56px; padding: 0 16px; background: #fff; border-bottom: 1px solid #E8ECF0; }
.nav-back { display: flex; align-items: center; width: 32px; height: 32px; border: none; background: none; color: #333; cursor: pointer; }
.nav-title { flex: 1; text-align: center; font-size: 16px; font-weight: 600; }

.loading-state { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.skeleton-card { height: 80px; background: linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: 12px; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

.empty-state { text-align: center; padding: 80px 32px; }
.empty-icon { font-size: 64px; margin-bottom: 16px; opacity: 0.5; }
.empty-state p { color: #999; margin-bottom: 20px; }
.discover-btn { padding: 10px 32px; border-radius: 20px; border: 1px solid #DDE1E6; background: #fff; color: #666; font-size: 14px; cursor: pointer; }

.invitation-list { padding: 12px 16px; display: flex; flex-direction: column; gap: 10px; }
.invitation-card { display: flex; align-items: center; gap: 14px; padding: 16px; background: #fff; border-radius: 12px; }
.inv-org-logo { width: 52px; height: 52px; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; font-weight: 700; flex-shrink: 0; }
.inv-info { flex: 1; min-width: 0; }
.inv-info h3 { margin: 0 0 4px; font-size: 16px; font-weight: 600; color: #333; }
.inv-meta { display: flex; align-items: center; gap: 10px; margin: 0 0 4px; font-size: 12px; color: #999; }
.inv-status { padding: 1px 8px; border-radius: 10px; font-size: 11px; font-weight: 500; }
.status-pending { background: #FFF7E6; color: #FA8C16; }
.status-done { background: #F5F5F5; color: #999; }
.inv-date { margin: 0; font-size: 11px; color: #ccc; }
.inv-actions { display: flex; gap: 8px; flex-shrink: 0; }
.btn-accept { padding: 8px 16px; border: none; border-radius: 16px; background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399)); color: #fff; font-size: 13px; font-weight: 600; cursor: pointer; }
.btn-accept:disabled { opacity: 0.5; }
.btn-reject { padding: 8px 16px; border: 1px solid #FF4D4F; border-radius: 16px; background: #fff; color: #FF4D4F; font-size: 13px; cursor: pointer; }
.btn-reject:disabled { opacity: 0.5; }
</style>
