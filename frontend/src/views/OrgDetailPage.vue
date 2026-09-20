<template>
  <div class="org-detail-page">
    <header class="page-nav">
      <button class="nav-back" @click="$router.back()"><svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="15,18 9,12 15,6"/></svg></button>
      <span class="nav-title">{{ org?.name || '组织详情' }}</span>
    </header>

    <div v-if="loading" class="loading-state">
      <el-skeleton animated :rows="6" class="org-detail-skeleton" />
    </div>

    <main v-else-if="org" class="org-content">
      <el-card shadow="never" class="org-header-card">
        <div class="org-logo" :style="{ backgroundColor: 'var(--color-primary-500, #10b981)' }">{{ org.name.charAt(0) }}</div>
        <h1>{{ org.name }}</h1>
        <p>{{ org.description || '暂无简介' }}</p>
        <div class="org-stats">
          <span>{{ org.memberCount || 0 }} 成员</span>
          <span>·</span>
          <span>{{ typeLabel(org.orgType) }}</span>
          <span>·</span>
          <span>{{ org.joinType === 'INVITE' ? '仅邀请' : '可申请加入' }}</span>
        </div>

        <div class="org-actions" v-if="myRole">
          <el-tag type="primary" round>{{ roleLabel(myRole.role) }}</el-tag>
          <el-button v-if="myRole.role === 'ADMIN' || myRole.role === 'MODERATOR'" @click="showManage = !showManage">⚙ 管理</el-button>
          <el-button v-if="myRole.role !== 'ADMIN'" type="danger" plain @click="leaveOrg" :disabled="leaving">{{ leaving ? '退出中...' : '退出组织' }}</el-button>
        </div>
        <div class="org-actions" v-else>
          <el-button v-if="org.joinType === 'APPLY' && !hasApplied" type="primary" round @click="applyJoin" :disabled="applying">{{ applying ? '提交中...' : '申请加入' }}</el-button>
          <el-tag v-else-if="hasApplied" type="warning" round>已申请，等待审核</el-tag>
        </div>
      </el-card>

      <el-card v-if="showManage && (myRole?.role === 'ADMIN' || myRole?.role === 'MODERATOR')" shadow="never" class="manage-panel-card">
        <el-tabs v-model="manageTab" stretch>
          <el-tab-pane label="申请列表" name="requests">
            <div v-if="manageTab === 'requests'" class="panel-body">
              <el-empty v-if="pendingRequests.length === 0" description="暂无待审批申请" :image-size="80" />
              <div v-for="req in pendingRequests" :key="req.id" class="request-item">
                <img :src="req.userAvatar || defaultAvatar" class="member-avatar" @error="(e) => ((e.target as HTMLImageElement).src = defaultAvatar)" />
                <span class="req-user">{{ req.userName || '用户' + req.userId }} 申请加入</span>
                <span class="req-msg" v-if="req.message">{{ req.message }}</span>
                <div class="req-actions">
                  <el-button type="primary" plain size="small" @click="approveReq(req.id)">通过</el-button>
                  <el-button type="danger" plain size="small" @click="rejectReq(req.id)">拒绝</el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="成员管理" name="members">
            <div v-if="manageTab === 'members'" class="panel-body">
              <el-empty v-if="members.length === 0" description="暂无成员" :image-size="80" />
              <div v-for="m in members" :key="m.id" class="member-item">
                <img :src="m.userAvatar || defaultAvatar" class="member-avatar" @error="(e) => ((e.target as HTMLImageElement).src = defaultAvatar)" />
                <span class="member-name">{{ m.userName || '用户' + m.userId }}</span>
                <el-tag size="small" effect="plain">{{ roleLabel(m.role) }}</el-tag>
                <div v-if="(myRole.role === 'ADMIN' || myRole.role === 'MODERATOR') && m.role !== 'ADMIN'" class="member-actions">
                  <el-button size="small" @click="changeRole(m.userId, 'MODERATOR')" v-if="m.role === 'MEMBER'">升为管理</el-button>
                  <el-button size="small" @click="changeRole(m.userId, 'MEMBER')" v-if="m.role === 'MODERATOR'">降为成员</el-button>
                  <el-button type="danger" plain size="small" @click="removeMem(m.userId)">移出</el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <el-tab-pane label="邀请成员" name="invite">
            <div v-if="manageTab === 'invite'" class="panel-body">
              <div class="invite-form">
                <div class="search-dropdown">
                  <el-input
                    v-model="inviteSearchKeyword"
                    type="text"
                    placeholder="输入用户昵称、账号或ID搜索"
                    clearable
                    @input="onInviteSearchInput"
                    @focus="onInviteSearchFocus"
                  />
                  <div v-if="inviteSearchResults.length > 0" class="dropdown-list">
                    <div
                      v-for="user in inviteSearchResults"
                      :key="user.id"
                      class="dropdown-item"
                      @click="selectInviteUser(user)"
                    >
                      <span class="user-name">{{ user.nickname || user.username }}</span>
                      <span class="user-id">ID: {{ user.id }}</span>
                    </div>
                  </div>
                </div>
                <el-button type="primary" @click="doInvite" :disabled="!inviteUserId">发送邀请</el-button>
              </div>
              <p v-if="invitedUserName" class="invite-target">正在邀请: {{ invitedUserName }}</p>
            </div>
          </el-tab-pane>

          <el-tab-pane label="操作日志" name="audit">
            <div v-if="manageTab === 'audit'" class="panel-body">
              <el-empty v-if="auditLogs.length === 0" description="暂无操作日志" :image-size="80" />
              <div v-for="log in auditLogs" :key="log.id" class="log-item">
                <span class="log-action">{{ log.action }}</span>
                <span class="log-time">{{ formatTime(log.createdAt) }}</span>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { organizationApi, userApi } from '../services/api'
import { useToast } from '../use/useToast'

const route = useRoute()
const toast = useToast()
const org = ref(null)
const loading = ref(true)
const myRole = ref(null)
const showManage = ref(false)
const manageTab = ref('requests')
const pendingRequests = ref([])
const members = ref([])
const auditLogs = ref([])
const inviteUserId = ref('')
const inviteSearchKeyword = ref('')
const invitedUserName = ref('')
const inviteSearchResults = ref([])
let inviteSearchTimer = null
const applying = ref(false)
const leaving = ref(false)
const hasApplied = ref(false)
const defaultAvatar = 'data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><rect fill="%23eee" width="100" height="100"/><text x="50" y="54" text-anchor="middle" font-size="36" fill="%23999" font-family="sans-serif">?</text></svg>'

onMounted(async () => {
  const orgId = String(route.params.id)
  try {
    const [orgRes, roleRes] = await Promise.allSettled([
      organizationApi.getDetail(orgId),
      organizationApi.getMyRole(orgId)
    ])
    if (orgRes.status === 'fulfilled' && orgRes.value && orgRes.value.code === 200) {
      org.value = orgRes.value.data || null
    }
    if (roleRes.status === 'fulfilled' && roleRes.value && roleRes.value.code === 200) {
      myRole.value = roleRes.value.data || null
    }
    if (myRole.value && (myRole.value.role === 'ADMIN' || myRole.value.role === 'MODERATOR')) {
      loadManageData(orgId)
    }
  } catch (e) {
    console.error('OrgDetail load error:', e)
  } finally {
    loading.value = false
  }
})

async function loadManageData(orgId) {
  try {
    const [reqsRes, membersRes, logsRes] = await Promise.all([
      organizationApi.getPendingRequests(orgId),
      organizationApi.getMembers(orgId, { page: 1, size: 50 }),
      organizationApi.getAuditLogs(orgId, 20)
    ])
    if (reqsRes.code === 200) pendingRequests.value = reqsRes.data || []
    if (membersRes.code === 200) members.value = membersRes.data || []
    if (logsRes.code === 200) auditLogs.value = logsRes.data || []
  } catch {}
}

async function applyJoin() {
  applying.value = true
  try {
    const res = await organizationApi.applyJoin(String(route.params.id), '')
    if (res.code === 200) {
      toast.showToast('申请已提交，等待管理员审核', 'success')
      hasApplied.value = true
    } else {
      toast.showToast(res.message || '申请失败', 'error')
    }
  } catch (e) {
    toast.showToast(e.message || '申请失败，请稍后重试', 'error')
  } finally {
    applying.value = false
  }
}
async function approveReq(id) { await organizationApi.approveRequest(id); loadManageData(String(route.params.id)) }
async function rejectReq(id) { await organizationApi.rejectRequest(id); loadManageData(String(route.params.id)) }
async function changeRole(uid, role) { await organizationApi.changeRole(String(route.params.id), uid, role); loadManageData(String(route.params.id)) }
async function removeMem(uid) {
  const ok = await toast.showConfirm('确定移出该成员?')
  if (!ok) return
  try {
    await organizationApi.removeMember(String(route.params.id), uid)
    toast.showToast('已移除成员')
    loadManageData(String(route.params.id))
  } catch (e) {
    toast.showToast(e?.response?.data?.message || e?.message || '移除失败')
  }
}
async function doInvite() {
  if (!inviteUserId.value) return
  try {
    await organizationApi.invite(String(route.params.id), Number(inviteUserId.value))
    toast.showToast('邀请已发送', 'success')
    inviteUserId.value = ''
    inviteSearchKeyword.value = ''
    invitedUserName.value = ''
    inviteSearchResults.value = []
  } catch (e) { toast.showToast(e?.message || '邀请失败', 'error') }
}

// 用户搜索：输入防抖300ms后查询
function onInviteSearchInput() {
  clearTimeout(inviteSearchTimer)
  const keyword = inviteSearchKeyword.value.trim()
  if (!keyword) {
    inviteSearchResults.value = []
    return
  }
  inviteSearchTimer = setTimeout(async () => {
    try {
      const res = await userApi.searchUsers(keyword)
      if (res.code === 200 && res.data) {
        inviteSearchResults.value = res.data
      }
    } catch { /* 搜索失败静默处理 */ }
  }, 300)
}

function onInviteSearchFocus() {
  if (inviteSearchKeyword.value.trim()) {
    onInviteSearchInput()
  }
}

function selectInviteUser(user) {
  inviteUserId.value = String(user.id)
  invitedUserName.value = user.nickname || user.username || '用户' + user.id
  inviteSearchKeyword.value = invitedUserName.value
  inviteSearchResults.value = []
}

async function leaveOrg() {
  const ok = await toast.showConfirm('确定退出该组织？退出后需要重新申请加入')
  if (!ok) return
  leaving.value = true
  try {
    await organizationApi.leaveOrg(String(route.params.id))
    toast.showToast('已退出组织')
    // 退出后刷新页面状态：myRole清空，回到非成员视图
    myRole.value = null
    showManage.value = false
  } catch (e) {
    toast.showToast(e?.message || '退出失败，请重试')
  } finally {
    leaving.value = false
  }
}

function typeLabel(t) { return { CLUB: '社团', STUDENT_ORG: '学生组织', BUSINESS: '商业', PERSONAL: '个人' }[t] || t }
function roleLabel(r) { return { ADMIN: '创建者', MODERATOR: '管理员', MEMBER: '成员' }[r] || r }
function formatTime(t) { return t ? new Date(t).toLocaleString('zh-CN') : '' }
</script>

<style scoped>
.org-detail-page { min-height: 100vh; background: #F5F7FA; }
.page-nav { position: sticky; top: 0; z-index: 100; display: flex; align-items: center; height: 56px; padding: 0 16px; background: #fff; border-bottom: 1px solid #E8ECF0; }
.nav-back { display: flex; align-items: center; width: 32px; height: 32px; border: none; background: none; color: #333; cursor: pointer; }
.nav-title { flex: 1; text-align: center; font-size: 16px; font-weight: 600; }

.loading-state { padding: 40px; display: flex; flex-direction: column; align-items: center; gap: 16px; }

.org-content { padding: 16px; max-width: 750px; margin: 0 auto; }
.org-logo { width: 72px; height: 72px; border-radius: 18px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 32px; font-weight: 700; margin: 0 auto 14px; }
.org-header-card h1 { font-size: 22px; font-weight: 700; margin: 0 0 8px; }
.org-header-card p { font-size: 14px; color: #999; margin: 0 0 12px; }
.org-stats { font-size: 13px; color: #999; margin-bottom: 16px; }
.org-actions { display: flex; gap: 10px; justify-content: center; align-items: center; flex-wrap: wrap; }

.panel-body { padding: 12px 16px; }

.request-item, .member-item { display: flex; align-items: center; gap: 8px; padding: 10px 0; border-bottom: 1px solid #f5f5f5; font-size: 14px; flex-wrap: wrap; }
.req-user { flex: 1; min-width: 0; }
.member-avatar { width: 32px; height: 32px; border-radius: 50%; object-fit: cover; background: #eee; flex-shrink: 0; }
.member-name { font-weight: 500; color: #333; }
.req-msg { color: #999; font-size: 12px; flex-basis: 100%; }
.req-actions, .member-actions { margin-left: auto; display: flex; gap: 6px; }

.invite-form { display: flex; gap: 8px; }
.search-dropdown { position: relative; flex: 1; }
.dropdown-list { position: absolute; top: 100%; left: 0; right: 0; background: #fff; border: 1px solid #E8ECF0; border-radius: 8px; max-height: 200px; overflow-y: auto; z-index: 200; box-shadow: 0 4px 12px rgba(0,0,0,0.1); margin-top: 4px; }
.dropdown-item { display: flex; justify-content: space-between; align-items: center; padding: 10px 12px; cursor: pointer; font-size: 13px; }
.dropdown-item:hover { background: #F5F7FA; }
.dropdown-item .user-name { color: #333; font-weight: 500; }
.dropdown-item .user-id { color: #999; font-size: 12px; }
.invite-target { margin: 8px 0 0; font-size: 13px; color: #1890FF; }
.log-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f5f5f5; font-size: 13px; }
.log-action { color: #333; } .log-time { color: #ccc; font-size: 11px; }
</style>

<style>
/* el-card / el-tabs 根节点由 EP 渲染，scoped 无法命中，用全局类兜底 */
.org-detail-page .org-header-card.el-card {
  border-radius: 16px;
  margin-bottom: 16px;
}

.org-detail-page .org-header-card.el-card .el-card__body {
  padding: 28px 20px;
  text-align: center;
}

.org-detail-page .manage-panel-card.el-card {
  border-radius: 16px;
  overflow: hidden;
}

.org-detail-page .manage-panel-card .el-tabs__header {
  margin: 0;
}

.org-detail-page .org-detail-skeleton {
  width: 100%;
}
</style>
