<template>
  <div class="org-discover-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg></button>
      <span class="nav-title">发现组织</span>
    </header>

    <div class="search-bar">
      <el-input v-model="keyword" @keyup.enter="search" placeholder="搜索组织..." clearable class="org-search">
        <template #prefix>
          <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#999" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/></svg>
        </template>
      </el-input>
    </div>

    <div v-if="loading" class="loading-state">
      <el-skeleton v-for="i in 4" :key="i" animated :rows="3" class="org-skeleton" />
    </div>

    <el-empty v-else-if="orgs.length === 0" description="没有找到相关组织" class="orgs-empty" />

    <main v-else class="org-list">
      <el-card v-for="org in orgs" :key="org.id" class="org-card" shadow="hover">
        <div class="org-logo" :style="{ backgroundColor: randomColor(org.id) }">{{ org.name.charAt(0) }}</div>
        <div class="org-info" @click="$router.push(`/orgs/${org.id}`)">
          <h3>{{ org.name }}</h3>
          <p>{{ org.description || '暂无简介' }}</p>
          <div class="org-meta">
            <span class="org-type">{{ typeLabel(org.orgType) }}</span>
            <span>{{ org.memberCount || 0 }} 成员</span>
            <span>{{ org.joinType === 'INVITE' ? '仅邀请' : '可申请加入' }}</span>
          </div>
        </div>
        <el-button v-if="org.joinType === 'APPLY'" type="primary" round size="small" class="apply-btn" :disabled="applyingIds.has(org.id)" @click.stop="applyToOrg(org.id)">
          {{ applyingIds.has(org.id) ? '申请中' : '申请' }}
        </el-button>
        <svg v-else @click.stop="$router.push(`/orgs/${org.id}`)" viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="#ccc" stroke-width="2"><polyline points="9,18 15,12 9,6"/></svg>
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
const keyword = ref('')
const applyingIds = ref(new Set())
const colors = ['#FF6A00','#1890FF','#52c41a','#722ED1','#EB2F96','#13C2C2','#FADB14','#FA541C','#2F54EB','#A0D911']

onMounted(() => search())

async function search() {
  loading.value = true
  try {
    const params: Record<string, unknown> = { page: 1, size: 50 }
    if (keyword.value) params.keyword = keyword.value
    const res = await organizationApi.getList(params)
    if (res.code === 200) orgs.value = res.data?.list || []
  } catch (e) {
    console.error('OrgDiscover load error:', e)
    orgs.value = []
  } finally { loading.value = false }
}

function typeLabel(t) { return { CLUB: '社团', STUDENT_ORG: '学生组织', BUSINESS: '商业', PERSONAL: '个人' }[t] || t }
function randomColor(id) { return colors[Math.abs(Number(id)) % colors.length] }

async function applyToOrg(orgId) {
  if (applyingIds.value.has(orgId)) return
  applyingIds.value.add(orgId)
  try {
    const res = await organizationApi.applyJoin(orgId, '')
    if (res.code === 200) {
      toast.showToast('申请已提交，等待管理员审核', 'success')
    } else {
      toast.showToast(res.message || '申请失败', 'error')
      applyingIds.value.delete(orgId)
    }
  } catch (e) {
    toast.showToast(e.message || '申请失败，请稍后重试', 'error')
    applyingIds.value.delete(orgId)
  }
}
</script>

<style scoped>
.org-discover-page { min-height: 100vh; background: #F5F7FA; }
.page-nav { position: sticky; top: 0; z-index: 100; display: flex; align-items: center; height: 56px; padding: 0 16px; background: #fff; border-bottom: 1px solid #E8ECF0; }
.nav-back { display: flex; align-items: center; width: 32px; height: 32px; border: none; background: none; color: #333; cursor: pointer; }
.nav-title { flex: 1; text-align: center; font-size: 16px; font-weight: 600; }

.search-bar { margin: 12px 16px; }
.loading-state { padding: 16px; display: flex; flex-direction: column; gap: 12px; }

.org-list { padding: 0 16px 16px; display: flex; flex-direction: column; gap: 10px; }
.org-logo { width: 52px; height: 52px; border-radius: 14px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 22px; font-weight: 700; flex-shrink: 0; }
.org-info { flex: 1; min-width: 0; cursor: pointer; }
.org-info h3 { margin: 0 0 4px; font-size: 16px; font-weight: 600; color: #333; }
.org-info p { margin: 0 0 6px; font-size: 13px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.org-meta { display: flex; gap: 10px; font-size: 12px; color: #999; }
.org-type { padding: 2px 8px; background: #FFF7E6; color: var(--color-primary-500, #10b981); font-size: 11px; border-radius: 4px; }
</style>

<style>
/* el-card / el-input 根节点由 EP 渲染，scoped 无法命中，用全局类兜底 */
.org-discover-page .org-card.el-card {
  border-radius: 14px;
  transition: transform 0.15s;
}

.org-discover-page .org-card.el-card .el-card__body {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
}

.org-discover-page .org-card.el-card:active {
  transform: scale(0.98);
}

.org-discover-page .apply-btn.el-button {
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  border: none;
  font-weight: 600;
}

.org-discover-page .org-search.el-input .el-input__wrapper {
  background: #f5f5f5;
  border-radius: 10px;
  box-shadow: none;
}

.org-discover-page .org-search.el-input .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 1px var(--el-color-primary) inset;
}

.org-discover-page .org-skeleton {
  border-radius: 12px;
}

.org-discover-page .orgs-empty {
  padding: 80px 0;
}
</style>
