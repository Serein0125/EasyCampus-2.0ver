<template>
  <div class="my-orgs-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()">
        <svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg>
      </button>
      <span class="nav-title">我的组织</span>
      <el-button type="primary" round size="small" class="nav-create" @click="$router.push('/orgs/create')">+ 创建</el-button>
    </header>

    <div v-if="loading" class="loading-state">
      <el-skeleton v-for="i in 3" :key="i" animated :rows="3" class="org-skeleton" />
    </div>

    <el-empty v-else-if="orgs.length === 0" class="orgs-empty">
      <template #description>
        <p>你还未加入任何组织</p>
      </template>
      <div class="empty-actions">
        <el-button type="primary" round @click="$router.push('/orgs/create')">创建组织</el-button>
        <el-button round @click="$router.push('/orgs/invitations')">我的邀请</el-button>
        <el-button round @click="$router.push('/orgs/discover')">发现组织</el-button>
      </div>
    </el-empty>

    <main v-else class="org-list">
      <el-card v-for="org in orgs" :key="org.id" class="org-card" shadow="hover" @click="$router.push(`/orgs/${org.id}`)">
        <div class="org-logo" :style="{ backgroundColor: randomColor(org.id) }">{{ org.name.charAt(0) }}</div>
        <div class="org-info">
          <h3>{{ org.name }}</h3>
          <p>{{ org.description || '暂无简介' }}</p>
          <div class="org-meta">
            <span class="org-type">{{ typeLabel(org.orgType) }}</span>
            <el-tag :type="org.status === 'PENDING' ? 'warning' : 'success'" size="small" effect="light">{{ org.status === 'APPROVED' ? '已通过' : org.status === 'PENDING' ? '审核中' : org.status }}</el-tag>
            <span class="org-count">{{ org.memberCount || 0 }} 成员</span>
          </div>
        </div>
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9,18 15,12 9,6"/></svg>
      </el-card>

      <!-- el-card 渲染为 div，需显式补上 button 语义与键盘激活能力 -->
      <el-card
        class="invitation-card-entry"
        shadow="hover"
        role="button"
        tabindex="0"
        @click="$router.push('/orgs/invitations')"
        @keydown.enter="$router.push('/orgs/invitations')"
        @keydown.space.prevent="$router.push('/orgs/invitations')"
      >
        <div class="invitation-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#1890FF" stroke-width="2"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><polyline points="22,6 12,13 2,6"/></svg>
        </div>
        <div class="invitation-info">
          <span class="invitation-title">我的邀请</span>
          <span class="invitation-desc">查看和处理组织邀请</span>
        </div>
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9,18 15,12 9,6"/></svg>
      </el-card>

      <!-- el-card 渲染为 div，需显式补上 button 语义与键盘激活能力 -->
      <el-card
        class="discover-card"
        shadow="hover"
        role="button"
        tabindex="0"
        @click="$router.push('/orgs/discover')"
        @keydown.enter="$router.push('/orgs/discover')"
        @keydown.space.prevent="$router.push('/orgs/discover')"
      >
        <div class="discover-icon">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#FF6A00" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
        </div>
        <div class="discover-info">
          <span class="discover-title">发现组织</span>
          <span class="discover-desc">浏览并加入更多组织</span>
        </div>
        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9,18 15,12 9,6"/></svg>
      </el-card>
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
.loading-state { padding: 16px; display: flex; flex-direction: column; gap: 12px; }
.empty-actions { display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; margin-top: 20px; }

.org-list { padding: 12px 16px; display: flex; flex-direction: column; gap: 10px; }
.org-logo { width: 52px; height: 52px; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; font-weight: 700; flex-shrink: 0; }
.org-info { flex: 1; min-width: 0; }
.org-info h3 { margin: 0 0 4px; font-size: 16px; font-weight: 600; color: #333; }
.org-info p { margin: 0 0 6px; font-size: 13px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.org-meta { display: flex; align-items: center; gap: 12px; }
.org-type { padding: 2px 8px; background: #FFF7E6; color: var(--color-primary-500, #10b981); font-size: 11px; border-radius: 4px; }
.org-count { font-size: 12px; color: #999; }

.discover-icon {
  width: 52px; height: 52px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  background: #FFF7E6; flex-shrink: 0;
}
.discover-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.discover-title { font-size: 15px; font-weight: 600; color: #333; }
.discover-desc { font-size: 12px; color: #999; }

.invitation-icon {
  width: 52px; height: 52px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  background: #E6F4FF; flex-shrink: 0;
}
.invitation-info { flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px; }
.invitation-title { font-size: 15px; font-weight: 600; color: #333; }
.invitation-desc { font-size: 12px; color: #999; }
</style>

<style>
/* el-card / el-button 根节点由 EP 渲染，scoped 无法命中，用全局类兜底 */
.my-orgs-page .nav-create.el-button {
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  border: none;
  font-weight: 600;
}

.my-orgs-page .org-card.el-card,
.my-orgs-page .invitation-card-entry.el-card,
.my-orgs-page .discover-card.el-card {
  border-radius: 14px;
  cursor: pointer;
  transition: transform 0.15s;
}

.my-orgs-page .org-card.el-card .el-card__body,
.my-orgs-page .invitation-card-entry.el-card .el-card__body,
.my-orgs-page .discover-card.el-card .el-card__body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
}

.my-orgs-page .org-card.el-card:active,
.my-orgs-page .invitation-card-entry.el-card:active,
.my-orgs-page .discover-card.el-card:active {
  transform: scale(0.98);
}

.my-orgs-page .invitation-card-entry.el-card,
.my-orgs-page .discover-card.el-card {
  border: 1.5px dashed #DDE1E6;
}

.my-orgs-page .invitation-card-entry.el-card:hover {
  border-color: #1890FF;
  background: #F0F7FF;
}

.my-orgs-page .discover-card.el-card:hover {
  border-color: var(--color-primary-500, #10b981);
  background: #FFFBF5;
}

.my-orgs-page .org-skeleton {
  border-radius: 12px;
}

.my-orgs-page .orgs-empty {
  padding: 80px 32px;
}
</style>
