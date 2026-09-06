<template>
  <!-- 最外层只承担「插槽 + 提供者」职责，本身不渲染样式 -->
  <div class="toast-provider">
    <!-- slot 承载应用真正的内容（App.vue 把整棵组件树传进来） -->
    <slot />

    <!-- teleport 把浮层传送到 <body> 下，避免被父级的 transform/overflow/z-index 裁剪
        （很多移动端容器会 overflow:hidden 或 transform 创建新层叠上下文，导致弹窗被遮挡） -->
    <teleport to="body">
      <!-- transition-group 用于「列表增删动画」，每个 toast 进出都有滑动/淡出效果。
          与单元素 <transition> 不同，它需要 :key 来跟踪列表项身份 -->
      <transition-group name="toast-slide" tag="div" class="toast-stack">
        <div v-for="t in toasts" :key="t.id" class="toast-item" :class="t.type">
          <span>{{ t.message }}</span>
        </div>
      </transition-group>

      <transition name="confirm-fade">
        <!-- @click.self 只在点击遮罩自身（而非内部 .confirm-box）时触发取消，
            是常见的「点遮罩关闭弹窗」交互 -->
        <div v-if="confirmState.visible" class="confirm-overlay" @click.self="cancelConfirm">
          <div class="confirm-box">
            <p class="confirm-msg">{{ confirmState.message }}</p>
            <div class="confirm-actions">
              <button class="confirm-btn cancel" @click="cancelConfirm">取消</button>
              <button class="confirm-btn ok" @click="okConfirm">确定</button>
            </div>
          </div>
        </div>
      </transition>
    </teleport>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, provide } from 'vue'

// 模块级自增 id：保证每次 toast 的 :key 唯一，transition-group 才能正确识别增删
let toastId = 0
const toasts = ref([])

// confirmState 用 reactive 集中管理确认弹窗的三要素：显隐、文案、resolve 回调
// 把 resolve 函数存进 state 是「命令式弹窗 Promise 化」的关键（见 showConfirm 注释）
const confirmState = reactive({ visible: false, message: '', resolve: null })

// 显示一条 toast：入栈后用 setTimeout 自动出栈，duration 到期即移除
function showToast(message, type = 'info', duration = 2000) {
  const id = ++toastId
  toasts.value.push({ id, message, type })
  setTimeout(() => {
    const idx = toasts.value.findIndex(t => t.id === id)
    if (idx > -1) toasts.value.splice(idx, 1)
  }, duration)
}

// 把「点按钮才返回结果」的命令式弹窗包装成 await-able 的 Promise：
// 1) 调用方 await showConfirm(msg) 暂停执行
// 2) 把 resolve 存进 confirmState.resolve，弹窗显示出来
// 3) 用户点击确定/取消时，okConfirm/cancelConfirm 调用 resolve(true/false)
// 4) Promise resolve，await 处拿到布尔值继续往下走
// 这样调用方代码可以写成同步风格，避免回调地狱
function showConfirm(message) {
  return new Promise((resolve) => {
    Object.assign(confirmState, { visible: true, message, resolve })
  })
}

function okConfirm() {
  confirmState.visible = false
  if (confirmState.resolve) confirmState.resolve(true)
}

function cancelConfirm() {
  confirmState.visible = false
  // resolve(false) 而非 reject：让调用方用普通的 if/else 处理「取消」，无需 try/catch
  if (confirmState.resolve) confirmState.resolve(false)
}

// provide 把 toast API 注入到后代组件树，子组件用 useToast() 的 inject 即可拿到，
// 实现「全局弹窗」而无需借助事件总线或状态管理库
provide('toast', { showToast, showConfirm })
</script>

<style scoped>
.toast-stack {
  position: fixed; top: 80px; left: 50%; transform: translateX(-50%); z-index: 9999;
  display: flex; flex-direction: column; align-items: center; gap: 8px; pointer-events: none;
}
.toast-item {
  padding: 10px 24px; border-radius: 8px; font-size: 14px; font-weight: 500; color: #fff;
  background: rgba(0,0,0,0.78); backdrop-filter: blur(8px);
  pointer-events: auto; max-width: 80vw; text-align: center;
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}
.toast-item.error { background: rgba(255,77,79,0.9); }
.toast-item.success { background: rgba(82,196,26,0.9); }

.toast-slide-enter-active { transition: all 0.3s ease; }
.toast-slide-leave-active { transition: all 0.25s ease; }
.toast-slide-enter-from { opacity: 0; transform: translateY(-16px); }
.toast-slide-leave-to { opacity: 0; transform: translateY(-8px); }

.confirm-overlay {
  position: fixed; inset: 0; z-index: 10000;
  display: flex; align-items: center; justify-content: center;
  background: rgba(0,0,0,0.45); padding: 16px;
}
.confirm-box {
  background: #fff; border-radius: 16px; width: 100%; max-width: 300px;
  padding: 28px 24px 20px; box-shadow: 0 8px 32px rgba(0,0,0,0.18);
  text-align: center;
}
.confirm-msg { font-size: 16px; color: #333; margin: 0 0 24px; line-height: 1.5; }
.confirm-actions { display: flex; gap: 12px; }
.confirm-btn { flex: 1; padding: 12px 0; border-radius: 10px; border: none; font-size: 15px; font-weight: 600; cursor: pointer; transition: transform 0.15s; }
.confirm-btn:active { transform: scale(0.96); }
.confirm-btn.cancel { background: #f5f5f5; color: #666; }
.confirm-btn.ok { background: linear-gradient(135deg,var(--color-primary-500, #10b981),var(--color-primary-400, #34d399)); color: #fff; }

.confirm-fade-enter-active { transition: opacity 0.2s ease; }
.confirm-fade-leave-active { transition: opacity 0.15s ease; }
.confirm-fade-enter-from, .confirm-fade-leave-to { opacity: 0; }
</style>
