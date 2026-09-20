<template>
  <canvas ref="canvasRef" class="particle-field" aria-hidden="true"></canvas>
</template>

<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { subscribeRaf } from '../use/useRafTicker'

// 极光粒子场：漂浮粒子随指针柔和吸附（光标内核区转为排斥）。
// 关键性能取舍：
// - 禁用 shadowBlur（单次 0.5~3ms，粒子一多就跌破 10fps），辉光改用 32×32 离屏 sprite
// - 粒子数按面积自适应，桌面上限 60、窄屏 24
// - 连线按 alpha 分 4 桶，每桶合并成单条 path 描边，减少 draw call
// - 每帧 setTransform 重置缩放（不能累乘，否则 DPR 翻倍）
interface Particle {
  x: number
  y: number
  vx: number
  vy: number
  r: number
  a: number
}

const props = withDefaults(
  defineProps<{
    /** 每个粒子摊到的面积(px²)，越大粒子越少 */
    density?: number
    /** 连线距离阈值(px) */
    linkDistance?: number
  }>(),
  { density: 14000, linkDistance: 110 }
)

const POINTER_RADIUS = 150
const POINTER_FORCE = 0.6
const CORE_RADIUS = 40 // 该半径内的粒子改为排斥，避免在光标处堆叠
const FRICTION = 0.965
const MAX_SPEED = 0.9
const BUCKET_ALPHAS = [0.3, 0.22, 0.15, 0.08]
// 物理常数都是「每帧」量，用它把真实耗时换算成帧步长，保证不同刷新率下表现一致
const BASE_FRAME_MS = 1000 / 60

const canvasRef = ref<HTMLCanvasElement | null>(null)

let ctx: CanvasRenderingContext2D | null = null
let sprite: HTMLCanvasElement | null = null
let hostEl: HTMLElement | null = null
let resizeObserver: ResizeObserver | null = null
let unsubscribe: (() => void) | null = null
let particles: Particle[] = []
let width = 0
let height = 0
let dpr = 1
let pendingResize = true
let running = false
let lastTime = 0
let hostLeft = 0
let hostTop = 0
// 只记录宽度的显著变化与高度的较大变化，过滤 iOS 地址栏造成的抖动
let lastObservedWidth = 0
let lastObservedHeight = 0
const pointer = { x: 0, y: 0, active: false }

function clamp(value: number, min: number, max: number): number {
  return value < min ? min : value > max ? max : value
}

/** 预渲染的辉光贴图：替代 shadowBlur，drawImage 成本几乎为零 */
function createSprite(): HTMLCanvasElement {
  const size = 32
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const context = canvas.getContext('2d')
  if (context) {
    const gradient = context.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2)
    gradient.addColorStop(0, 'rgba(255, 255, 255, 1)')
    gradient.addColorStop(0.35, 'rgba(209, 250, 229, 0.5)')
    gradient.addColorStop(1, 'rgba(209, 250, 229, 0)')
    context.fillStyle = gradient
    context.fillRect(0, 0, size, size)
  }
  return canvas
}

function targetCount(): number {
  const isCompact = width <= 768
  const minCount = isCompact ? 12 : 28
  const maxCount = isCompact ? 24 : 60
  return Math.round(clamp((width * height) / props.density, minCount, maxCount))
}

function seedParticles(): void {
  particles = Array.from({ length: targetCount() }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.5,
    vy: (Math.random() - 0.5) * 0.5,
    r: 1 + Math.random() * 1.2,
    a: 0.25 + Math.random() * 0.35
  }))
}

function applySize(): void {
  const canvas = canvasRef.value
  const host = hostEl
  if (!canvas || !host) return
  const rect = host.getBoundingClientRect()
  if (!rect.width || !rect.height) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = rect.width
  height = rect.height
  canvas.width = Math.round(width * dpr)
  canvas.height = Math.round(height * dpr)
  hostLeft = rect.left
  hostTop = rect.top
  // 只在「应有粒子数」变化时重新播种，否则仅把越界粒子夹回可视区，
  // 避免每次窗口缩放都把整个粒子场洗牌
  if (particles.length !== targetCount()) {
    seedParticles()
  } else {
    for (const p of particles) {
      p.x = Math.min(p.x, width)
      p.y = Math.min(p.y, height)
    }
  }
}

/** 指针坐标换算基准：滚动/缩放时刷新，避免每次 pointermove 都做布局读取 */
function refreshHostRect(): void {
  if (!hostEl) return
  const rect = hostEl.getBoundingClientRect()
  hostLeft = rect.left
  hostTop = rect.top
}

/**
 * 窗口尺寸变化：刷新坐标基准，并置位重设分辨率。
 * 跨不同缩放比例的显示器时 devicePixelRatio 会变，必须重新读取，否则 canvas 会发虚。
 */
function handleWindowResize(): void {
  refreshHostRect()
  pendingResize = true
}

function stepPhysics(step: number): void {
  // 摩擦按时间步长缩放，保证不同刷新率下衰减一致
  const damping = Math.pow(FRICTION, step)

  for (const p of particles) {
    if (pointer.active) {
      const dx = pointer.x - p.x
      const dy = pointer.y - p.y
      const distance = Math.max(Math.hypot(dx, dy), 0.001)
      if (distance < POINTER_RADIUS) {
        const force = POINTER_FORCE * (1 - distance / POINTER_RADIUS) * step
        const nx = dx / distance
        const ny = dy / distance
        if (distance < CORE_RADIUS) {
          p.vx -= nx * force * 2.2
          p.vy -= ny * force * 2.2
        } else {
          p.vx += nx * force
          p.vy += ny * force
        }
      }
    }

    // 微小随机扰动：否则摩擦会让粒子在约 2 秒后完全静止，"漂浮"名存实亡
    p.vx += (Math.random() - 0.5) * 0.12 * step
    p.vy += (Math.random() - 0.5) * 0.12 * step

    p.vx *= damping
    p.vy *= damping

    const speed = Math.hypot(p.vx, p.vy)
    if (speed > MAX_SPEED) {
      p.vx = (p.vx / speed) * MAX_SPEED
      p.vy = (p.vy / speed) * MAX_SPEED
    }

    p.x += p.vx * step
    p.y += p.vy * step

    // 边界反弹（不用回绕：回绕会让连线横穿整个画面）
    if (p.x < 0) {
      p.x = 0
      p.vx = Math.abs(p.vx)
    } else if (p.x > width) {
      p.x = width
      p.vx = -Math.abs(p.vx)
    }
    if (p.y < 0) {
      p.y = 0
      p.vy = Math.abs(p.vy)
    } else if (p.y > height) {
      p.y = height
      p.vy = -Math.abs(p.vy)
    }
  }
}

function draw(context: CanvasRenderingContext2D): void {
  context.setTransform(dpr, 0, 0, dpr, 0, 0)
  context.clearRect(0, 0, width, height)

  const linkDistance = width <= 768 ? Math.min(props.linkDistance, 90) : props.linkDistance
  const maxDistanceSq = linkDistance * linkDistance

  // 连线：先比较距离平方，仅在确定要画时才开方；再按 alpha 分桶合并描边
  const buckets: number[][] = BUCKET_ALPHAS.map(() => [])
  for (let i = 0; i < particles.length; i++) {
    const a = particles[i]
    for (let j = i + 1; j < particles.length; j++) {
      const b = particles[j]
      const dx = a.x - b.x
      const dy = a.y - b.y
      const distanceSq = dx * dx + dy * dy
      if (distanceSq >= maxDistanceSq) continue
      const alpha = (1 - Math.sqrt(distanceSq) / linkDistance) * 0.35
      if (alpha < 0.05) continue
      const bucketIndex = alpha > 0.26 ? 0 : alpha > 0.18 ? 1 : alpha > 0.11 ? 2 : 3
      buckets[bucketIndex].push(a.x, a.y, b.x, b.y)
    }
  }

  context.lineWidth = 1
  for (let i = 0; i < buckets.length; i++) {
    const points = buckets[i]
    if (!points.length) continue
    context.strokeStyle = `rgba(236, 254, 255, ${BUCKET_ALPHAS[i]})`
    context.beginPath()
    for (let k = 0; k < points.length; k += 4) {
      context.moveTo(points[k], points[k + 1])
      context.lineTo(points[k + 2], points[k + 3])
    }
    context.stroke()
  }

  if (!sprite) return
  for (const p of particles) {
    const size = p.r * 6
    context.globalAlpha = p.a
    context.drawImage(sprite, p.x - size / 2, p.y - size / 2, size, size)
  }
  context.globalAlpha = 1
}

function frame(now: number): void {
  if (pendingResize) {
    pendingResize = false
    applySize()
  }
  if (!ctx || !width || !height) return

  const elapsed = lastTime ? now - lastTime : BASE_FRAME_MS
  lastTime = now
  // 换算成与刷新率无关的步长：不换算的话 120Hz 会跑成约 2 倍速，
  // 30Hz 则因耗时超过阈值而完全不执行物理、粒子静止不动。
  // 单帧上限 32ms，避免切回标签页时粒子瞬移。
  const step = Math.min(elapsed, 32) / BASE_FRAME_MS
  stepPhysics(step)
  draw(ctx)
}

function start(): void {
  if (running) return
  running = true
  lastTime = 0
  unsubscribe = subscribeRaf(frame)
}

function stop(): void {
  if (!running) return
  running = false
  unsubscribe?.()
  unsubscribe = null
}

function handlePointerMove(event: PointerEvent): void {
  pointer.x = event.clientX - hostLeft
  pointer.y = event.clientY - hostTop
  pointer.active = true
}

function handlePointerLeave(): void {
  pointer.active = false
}

function handleVisibilityChange(): void {
  if (document.hidden) stop()
  else start()
}

function handleResize(entries: ResizeObserverEntry[]): void {
  const entry = entries[0]
  if (!entry) return
  const box = entry.contentRect
  // 观察的是父容器而非 canvas 自身（观察自身会自激）；真正改尺寸放到 rAF 内合并执行
  if (
    Math.abs(box.width - lastObservedWidth) < 1 &&
    Math.abs(box.height - lastObservedHeight) < 80
  ) {
    return
  }
  lastObservedWidth = box.width
  lastObservedHeight = box.height
  pendingResize = true
}

onMounted(() => {
  const canvas = canvasRef.value
  const host = canvas?.parentElement
  if (!canvas || !host) return
  ctx = canvas.getContext('2d')
  if (!ctx) return
  hostEl = host
  sprite = createSprite()
  applySize()

  host.addEventListener('pointermove', handlePointerMove, { passive: true })
  host.addEventListener('pointerleave', handlePointerLeave, { passive: true })
  window.addEventListener('scroll', refreshHostRect, { passive: true })
  window.addEventListener('resize', handleWindowResize, { passive: true })
  document.addEventListener('visibilitychange', handleVisibilityChange)

  resizeObserver = new ResizeObserver(handleResize)
  resizeObserver.observe(host)

  start()
})

onBeforeUnmount(() => {
  // 路由 out-in 转场期间组件仍会存活约 250ms，必须在这里彻底清理
  stop()
  resizeObserver?.disconnect()
  resizeObserver = null
  hostEl?.removeEventListener('pointermove', handlePointerMove)
  hostEl?.removeEventListener('pointerleave', handlePointerLeave)
  window.removeEventListener('scroll', refreshHostRect)
  window.removeEventListener('resize', handleWindowResize)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
  hostEl = null
  ctx = null
  sprite = null
  particles = []
})
</script>

<style scoped>
.particle-field {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}
</style>