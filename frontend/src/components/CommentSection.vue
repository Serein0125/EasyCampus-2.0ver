<template>
  <div class="comment-section">
    <h4 class="section-title">评论 ({{ totalCount }})</h4>

    <div v-if="loading" class="loading">
      <div class="skeleton" v-for="i in 3" :key="i" style="height:60px;margin-bottom:12px;border-radius:8px;background:#f0f0f0;"></div>
    </div>

    <div v-else-if="comments.length === 0" class="empty">暂无评论，来说两句吧</div>

    <div v-else class="comment-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item" :class="{ 'has-replies': comment.replies?.length }">
        <div class="comment-main">
          <el-avatar :size="32" :src="comment.userAvatar || undefined" class="comment-avatar">
            <svg viewBox="0 0 40 40" width="20" height="20"><circle cx="20" cy="20" r="20" fill="#e5e7eb"/><circle cx="20" cy="15" r="8" fill="#d1d5db"/><ellipse cx="20" cy="35" rx="12" ry="8" fill="#d1d5db"/></svg>
          </el-avatar>
          <div class="comment-body">
            <div class="comment-header">
              <span class="comment-user">{{ comment.userName || '匿名用户' }}</span>
              <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
            <div class="comment-actions">
              <LikeButton :is-liked="comment.isLiked" :count="comment.likeCount" target-type="COMMENT" :target-id="comment.id" @toggled="(l,c) => onCommentLikeToggled(comment, l, c)" />
              <button class="reply-btn" @click="startReply(comment)">回复</button>
              <button v-if="isOwnComment(comment)" class="delete-btn" @click="confirmDelete(comment.id)">删除</button>
            </div>
          </div>
        </div>

        <div v-if="comment.replies?.length" class="replies">
          <div v-for="reply in comment.replies" :key="reply.id" class="reply-item">
            <el-avatar :size="28" :src="reply.userAvatar || undefined" class="reply-avatar">
              <svg viewBox="0 0 40 40" width="18" height="18"><circle cx="20" cy="20" r="20" fill="#e5e7eb"/><circle cx="20" cy="15" r="8" fill="#d1d5db"/><ellipse cx="20" cy="35" rx="12" ry="8" fill="#d1d5db"/></svg>
            </el-avatar>
            <div class="reply-body">
              <div class="reply-header">
                <span class="reply-user">{{ reply.userName || '匿名用户' }}</span>
                <span v-if="reply.parentId" class="reply-to">回复 {{ getReplyTargetName(reply) }}</span>
                <span class="reply-time">{{ formatTime(reply.createdAt) }}</span>
              </div>
              <p class="reply-content">{{ reply.content }}</p>
              <div class="reply-actions">
                <button v-if="isOwnComment(reply)" class="delete-btn" @click="confirmDelete(reply.id, comment)">删除</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="isAuthenticated" class="comment-input-bar">
      <el-input
        v-model="newComment"
        :placeholder="replyTo ? '回复 ' + replyTo.userName + '...' : '写下你的评论...'"
        maxlength="500"
        @keyup.enter="submitComment"
        class="comment-input"
      />
      <el-button type="primary" round :disabled="!newComment.trim()" @click="submitComment">发送</el-button>
      <el-button v-if="replyTo" round @click="cancelReply">取消</el-button>
    </div>
    <div v-else class="login-hint">
      <router-link to="/login">登录</router-link> 后参与评论
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { postApi, productCommentApi } from '../services/api'
import { useAuthStore } from '../store/auth'
import { useToast } from '../use/useToast'
import LikeButton from './LikeButton.vue'

const props = defineProps({
  targetId: { type: [Number, String], required: true },
  targetType: { type: String, default: 'post' },
  initialComments: { type: Array, default: () => [] },
  /* 内容作者ID，内容作者可删除自己内容下的他人评论 */
  authorId: { type: [Number, String], default: null }
})

const { isAuthenticated, currentUser } = useAuthStore()
const toast = useToast()
const comments = ref<any[]>(props.initialComments || [])
const totalCount = ref(0)
const loading = ref(false)
const newComment = ref('')
const replyTo = ref<any>(null)

const currentUserId = computed(() => currentUser.value?.id)

function isOwnComment(comment) {
  if (!currentUserId.value) return false
  const commentUid = comment.userId
  /* 自己的评论可以删除；内容作者也可以删除自己内容下的他人评论 */
  if (String(commentUid) === String(currentUserId.value)) return true
  if (props.authorId && String(props.authorId) === String(currentUserId.value)) return true
  return false
}

const commentApi = props.targetType === 'product' ? productCommentApi : postApi

onMounted(() => { loadComments() })

function formatTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return date.toLocaleDateString()
}

function startReply(comment) {
  replyTo.value = comment
  newComment.value = ''
}

function cancelReply() {
  replyTo.value = null
  newComment.value = ''
}

function getReplyTargetName(reply) {
  // 在replies中查找parentId对应的评论用户名
  if (!reply.parentId) return ''
  const parentComment = comments.value.find(c => c.id === reply.parentId)
  if (parentComment) return parentComment.userName || '匿名用户'
  // 也可能在同一父评论的replies中
  for (const c of comments.value) {
    if (c.replies) {
      const parent = c.replies.find(r => r.id === reply.parentId)
      if (parent) return parent.userName || '匿名用户'
    }
  }
  return ''
}

async function submitComment() {
  if (!newComment.value.trim()) return
  try {
    const data: any = { content: newComment.value.trim() }
    if (replyTo.value) data.parentId = replyTo.value.id

    const res = await commentApi.addComment(props.targetId, data)
    if (res.code === 200) {
      if (replyTo.value) {
        if (!replyTo.value.replies) replyTo.value.replies = []
        replyTo.value.replies.push(res.data)
        cancelReply()
      } else {
        comments.value.unshift(res.data)
      }
      totalCount.value++
      newComment.value = ''
    }
  } catch {
    toast.showToast('评论发送失败', 'error')
  }
}

async function deleteComment(commentId, parentComment) {
  try {
    await commentApi.deleteComment(commentId)
    if (parentComment) {
      if (parentComment.replies) {
        const deletedReply = parentComment.replies.find(r => r.id === commentId)
        parentComment.replies = parentComment.replies.filter(r => r.id !== commentId)
        if (deletedReply) totalCount.value = Math.max(0, totalCount.value - 1)
      }
    } else {
      const deletedComment = comments.value.find(c => c.id === commentId)
      comments.value = comments.value.filter(c => c.id !== commentId)
      if (deletedComment) {
        const replyCount = deletedComment.replies?.length || 0
        totalCount.value = Math.max(0, totalCount.value - 1 - replyCount)
      }
    }
  } catch {
    toast.showToast('删除失败', 'error')
  }
}

async function confirmDelete(commentId, parentComment?) {
  const ok = await toast.showConfirm('确定要删除这条评论吗？删除后不可恢复。')
  if (ok) {
    deleteComment(commentId, parentComment)
  }
}

function onCommentLikeToggled(comment, isLiked, count) {
  comment.isLiked = isLiked
  comment.likeCount = count
}

async function loadComments() {
  loading.value = true
  try {
    const res = await commentApi.getComments(props.targetId)
    if (res.code === 200) {
      const list = res.data || []
      comments.value = Array.isArray(list) ? list : (list.list || list.records || [])
      totalCount.value = list.length || list.total || 0
    }
  } catch {} finally {
    loading.value = false
  }
}
</script>

<style scoped>
.comment-section {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
  color: #333;
}
.empty {
  text-align: center;
  color: #999;
  padding: 30px 0;
  font-size: 14px;
}
.comment-item {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f5f5f5;
}
.comment-main {
  display: flex;
  gap: 10px;
}
.comment-avatar, .reply-avatar {
  background: #eee;
  flex-shrink: 0;
}
.comment-body { flex: 1; }
.comment-header, .reply-header {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.comment-user, .reply-user { font-size: 13px; font-weight: 600; color: #333; }
.comment-time, .reply-time { font-size: 12px; color: #999; }
.reply-to { font-size: 12px; color: var(--color-primary-500, #10b981); }
.comment-content, .reply-content {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin-bottom: 6px;
}
.comment-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}
.reply-btn, .delete-btn {
  font-size: 12px;
  color: #999;
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
}
.reply-btn:hover { color: var(--color-primary-500, #10b981); }
.delete-btn:hover { color: #ff4757; }

.replies {
  margin-left: 42px;
  margin-top: 10px;
  padding-left: 12px;
  border-left: 2px solid #f0f0f0;
}
.reply-item {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}
.reply-body { flex: 1; }
.reply-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 2px;
}

.comment-input-bar {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}
.comment-input {
  flex: 1;
}
.login-hint {
  text-align: center;
  padding: 16px;
  color: #999;
  font-size: 14px;
}
.login-hint a { color: var(--color-primary-500, #10b981); }
.loading { padding: 20px 0; }
</style>
