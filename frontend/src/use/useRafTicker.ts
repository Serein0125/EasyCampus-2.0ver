// 模块级共享的 requestAnimationFrame 调度器。
// 粒子场与卡片柔光共用同一个 rAF 循环，
// 避免多个独立循环在同一帧内互相竞争、重复调度。
type RafCallback = (now: number) => void

const callbacks = new Set<RafCallback>()
let rafId: number | null = null
// tick 执行期间 rafId 为 null，需要单独标记，避免回调内再次订阅时启动第二个循环
let isTicking = false

function tick(now: number): void {
  rafId = null
  isTicking = true
  try {
    // 复制一份再遍历：回调内部可能自行注销，避免遍历过程中集合被修改
    for (const callback of [...callbacks]) {
      if (callbacks.has(callback)) callback(now)
    }
  } finally {
    // 无论回调是否抛异常都要续上循环：否则共享循环会永久死亡且无法自愈
    isTicking = false
    if (callbacks.size > 0) {
      rafId = requestAnimationFrame(tick)
    }
  }
}

/**
 * 订阅共享 rAF 循环，返回取消订阅函数。
 * 首个订阅者会启动循环，最后一个订阅者退出时自动停止，不留空转。
 */
export function subscribeRaf(callback: RafCallback): () => void {
  callbacks.add(callback)
  if (rafId === null && !isTicking) {
    rafId = requestAnimationFrame(tick)
  }
  return () => {
    callbacks.delete(callback)
    if (callbacks.size === 0 && rafId !== null) {
      cancelAnimationFrame(rafId)
      rafId = null
    }
  }
}

/** 是否具备精细指针 + 悬停能力（触摸设备返回 false，用于关闭鼠标类动效） */
export function canHover(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false
  return window.matchMedia('(hover: hover) and (pointer: fine)').matches
}