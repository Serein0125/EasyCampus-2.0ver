<template>
  <div class="invitations-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <span class="nav-title">我的邀请</span>
    </header>

    <div v-if="loading" class="loading-state">
      <el-skeleton v-for="i in 3" :key="i" animated :rows="3" class="inv-skeleton" />
    </div>

    <el-empty v-else-if="invitations.length === 0" class="invitations-empty">
      <template #description>
        <p>暂无待处理的邀请</p>
      </template>
      <el-button type="primary" round @click="$router.push('/orgs/discover')">发现组织</el-button>
    </el-empty>

    <main v-else class="invitation-list">
      <el-card v-for="inv in invitations" :key="inv.id" class="invitation-card" shadow="hover">
        <div class="inv-org-logo" :style="{ backgroundColor: randomColor(inv.orgId) }">
          {{ (inv.orgName || 'O').charAt(0) }}
        </div>
        <div class="inv-info">
          <h3>{{ inv.orgName || '组织 ' + inv.orgId }}</h3>
          <p class="inv-meta">
            <span>邀请码: {{ inv.inviteCode }}</span>
            <el-tag :type="inv.status === 'PENDING' ? 'warning' : inv.status === 'ACCEPTED' ? 'success' : 'info'" size="small" effect="light" class="inv-status">
              {{ inv.status === 'PENDING' ? '待处理' : inv.status === 'ACCEPTED' ? '已接受' : '已拒绝' }}
            </el-tag>
          </p>
          <p class="inv-date">发送时间: {{ formatTime(inv.createdAt) }}</p>
        </div>
        <div v-if="inv.status === 'PENDING'" class="inv-actions">
          <el-button type="primary" round size="small" @click="acceptInvitation(inv)" :disabled="inv._processing">
            {{ inv._processing ? '处理中...' : '接受' }}
          </el-button>
          <el-button type="danger" plain round size="small" @click="rejectInvitation(inv)" :disabled="inv._processing">拒绝</el-button>
        </div>
      </el-card>
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

.invitation-list { padding: 12px 16px; display: flex; flex-direction: column; gap: 10px; }
.inv-org-logo { width: 52px; height: 52px; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; font-weight: 700; flex-shrink: 0; }
.inv-info { flex: 1; min-width: 0; }
.inv-info h3 { margin: 0 0 4px; font-size: 16px; font-weight: 600; color: #333; }
.inv-meta { display: flex; align-items: center; gap: 10px; margin: 0 0 4px; font-size: 12px; color: #999; }
.inv-date { margin: 0; font-size: 11px; color: #ccc; }
.inv-actions { display: flex; gap: 8px; flex-shrink: 0; }
</style>

<style>
/* el-card 根节点由 EP 渲染，scoped 无法命中，用全局类兜底 */
.invitations-page .invitation-card.el-card {
  border-radius: 14px;
}

.invitations-page .invitation-card.el-card .el-card__body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
}

.invitations-page .inv-skeleton {
  border-radius: 12px;
}

.invitations-page .invitations-empty {
  padding: 80px 32px;
}
</style>
