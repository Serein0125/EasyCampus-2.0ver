import { onMounted, onScopeDispose, watch, type Ref } from 'vue'
import { canHover, subscribeRaf } from './useRafTicker'
import { useReducedMotion } from './useReducedMotion'

// 卡片边缘柔光跟随：
// 光标在右侧表单区移动时，把「光照方向」映射成两组 CSS 变量写进 .form-stage：
//   --glow-x / --glow-y  → 描边光环的受光点（px，相对卡片）
//   --glow-dx / --glow-dy → 环外环境光的位移（px，±MAX_OFFSET）
// 卡片本身不做任何位移与旋转，表单始终保持像素级静止。
const MAX_OFFSET = 14 // 环外环境光的最大位移半径(px)
const SMOOTHING = 0.14 // 每帧向目标插值的比例，越小越柔和
const SETTLE_EPSILON = 0.08 // 收敛阈值：低于此值直接吸附并停止循环
const ACTIVE_CLASS = 'is-glowing' // 仅在运动期间挂 will-change，避免常驻

interface EdgeGlowOptions {
  /** 指针监听区域（右侧表单区） */
  surfaceRef: Ref<HTMLElement | null>
  /** CSS 变量写入目标，同时也是坐标换算的基准（.form-stage） */
  stageRef: Ref<HTMLElement | null>
}

function clamp(value: number, min: number, max: number): number {
  return value < min ? min : value > max ? max : value
}

export function useEdgeGlow({ surfaceRef, stageRef }: EdgeGlowOptions): void {
  const prefersReducedMotion = useReducedMotion()

  let unsubscribe: (() => void) | null = null
  let frameWidth = 0
  let frameHeight = 0
  let targetX = 0
  let targetY = 0
  let currentX = 0
  let currentY = 0
  let initialized = false

  function writeVars(): void {
    const stage = stageRef.value
    if (!stage) return
    // 归一化偏移：卡片中心为 0、四角为 ±1，再映射到环境光位移半径。
    // 监听区域比卡片宽，指针在卡片外时需 clamp，否则会超出 MAX_OFFSET。
    const nx = frameWidth > 0 ? clamp((currentX / frameWidth - 0.5) * 2, -1, 1) : 0
    const ny = frameHeight > 0 ? clamp((currentY / frameHeight - 0.5) * 2, -1, 1) : 0
    stage.style.setProperty('--glow-x', `${currentX.toFixed(1)}px`)
    stage.style.setProperty('--glow-y', `${currentY.toFixed(1)}px`)
    stage.style.setProperty('--glow-dx', `${(nx * MAX_OFFSET).toFixed(2)}px`)
    stage.style.setProperty('--glow-dy', `${(ny * MAX_OFFSET).toFixed(2)}px`)
  }

  function frame(): void {
    const dx = targetX - currentX
    const dy = targetY - currentY
    // 已收敛：吸附到目标、补最后一次写入并停掉循环，不留空转
    if (Math.abs(dx) < SETTLE_EPSILON && Math.abs(dy) < SETTLE_EPSILON) {
      currentX = targetX
      currentY = targetY
      writeVars()
      stop()
      return
    }
    currentX += dx * SMOOTHING
    currentY += dy * SMOOTHING
    writeVars()
  }

  function start(): void {
    if (unsubscribe) return
    stageRef.value?.classList.add(ACTIVE_CLASS)
    unsubscribe = subscribeRaf(frame)
  }

  function stop(): void {
    unsubscribe?.()
    unsubscribe = null
    stageRef.value?.classList.remove(ACTIVE_CLASS)
  }

  function handlePointerMove(event: PointerEvent): void {
    if (prefersReducedMotion.value) return
    const stage = stageRef.value
    if (!stage) return
    const rect = stage.getBoundingClientRect()
    if (!rect.width || !rect.height) return
    frameWidth = rect.width
    frameHeight = rect.height

    const x = event.clientX - rect.left
    const y = event.clientY - rect.top

    // 首次进入：直接从指针所在位置起步，避免从左上角滑过来
    if (!initialized) {
      initialized = true
      currentX = targetX = x
      currentY = targetY = y
      writeVars()
      return
    }

    targetX = x
    targetY = y
    start()
  }

  function handlePointerLeave(): void {
    if (prefersReducedMotion.value) return
    // 回到中性态：光环缓动到卡片中心
    if (frameWidth > 0 && frameHeight > 0) {
      targetX = frameWidth / 2
      targetY = frameHeight / 2
      start()
    }
  }

  function handleResize(): void {
    initialized = false
    if (frameWidth > 0 && frameHeight > 0) {
      targetX = currentX = frameWidth / 2
      targetY = currentY = frameHeight / 2
      writeVars()
    }
  }

  onMounted(() => {
    // 触摸设备直接不接：柔光是鼠标专属效果
    if (!canHover()) return
    const surface = surfaceRef.value
    if (!surface) return
    surface.addEventListener('pointermove', handlePointerMove, { passive: true })
    surface.addEventListener('pointerleave', handlePointerLeave, { passive: true })
    window.addEventListener('resize', handleResize, { passive: true })
  })

  // 运行时切换「减少动效」：立即停掉循环并把光环收回卡片中心。
  // 监听器保持挂载，偏好关闭后无需重挂即可恢复响应，与 LoginBrandPanel 的响应式 v-if 语义一致。
  watch(prefersReducedMotion, (reduced) => {
    if (!reduced) return
    stop()
    if (frameWidth > 0 && frameHeight > 0) {
      currentX = targetX = frameWidth / 2
      currentY = targetY = frameHeight / 2
      writeVars()
    }
  })

  onScopeDispose(() => {
    const surface = surfaceRef.value
    surface?.removeEventListener('pointermove', handlePointerMove)
    surface?.removeEventListener('pointerleave', handlePointerLeave)
    window.removeEventListener('resize', handleResize)
    stop()
  })
}