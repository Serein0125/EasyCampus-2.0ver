<template>
  <aside class="brand-panel">
    <!-- 极光层：多个大尺寸 radial-gradient 缓慢漂移 + 一层极慢旋转的 conic 丝带 -->
    <div class="aurora" aria-hidden="true">
      <span class="aurora-blob blob-1"></span>
      <span class="aurora-blob blob-2"></span>
      <span class="aurora-blob blob-3"></span>
      <span class="aurora-blob blob-4"></span>
      <span class="aurora-ribbon"></span>
    </div>

    <!-- 噪点质感：内联 SVG feTurbulence，用 opacity 而非 mix-blend-mode（后者会与 canvas 每帧重算合成） -->
    <div class="noise" aria-hidden="true"></div>

    <!-- Canvas 粒子场：减少动效偏好下完全不挂载，而不是画一帧还留着循环 -->
    <ParticleField v-if="!prefersReducedMotion" />

    <!-- 集市层：漂浮的「闲置物品」芯片，纯装饰 -->
    <div class="market-layer" aria-hidden="true">
      <div
        v-for="chip in marketChips"
        :key="chip.label"
        class="market-chip"
        :class="chip.posClass"
        :style="{
          '--float-duration': chip.duration,
          '--float-delay': chip.delay,
          '--chip-rotate': chip.rotate
        }"
      >
        <div class="chip-inner">
          <svg
            class="chip-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path v-for="d in chip.icon" :key="d" :d="d" />
          </svg>
          <span class="chip-label">{{ chip.label }}</span>
        </div>
      </div>
    </div>

    <div class="brand-content">
      <h1 class="brand-title">易校EasyCampus</h1>
      <p class="brand-subtitle">EASYCAMPUS</p>
      <p class="brand-slogan">让闲置流动起来，让校园更精彩</p>
    </div>
  </aside>
</template>

<script setup lang="ts">
import ParticleField from './ParticleField.vue'
import { useReducedMotion } from '../use/useReducedMotion'

// 左侧品牌区：极光底 + 噪点 + 集市芯片层 + 粒子场。
// 所有漂浮/漂移/入场动画都写在 @media (prefers-reduced-motion: no-preference) 内，
// 基础样式一律是「完全可见的终态」，避免 animation: none 时元素永久不可见。
const prefersReducedMotion = useReducedMotion()

interface MarketChip {
  label: string
  /** 内联 SVG 的 path d 列表（统一 24×24 线描风格） */
  icon: string[]
  posClass: string
  duration: string
  delay: string
  rotate: string
}

const marketChips: MarketChip[] = [
  {
    label: '九成新',
    icon: [
      'M4 19.5A2.5 2.5 0 0 1 6.5 17H20',
      'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z'
    ],
    posClass: 'chip-pos-1',
    duration: '7.5s',
    delay: '260ms',
    rotate: '-8deg'
  },
  {
    label: '急出',
    icon: [
      'M3 18v-6a9 9 0 0 1 18 0v6',
      'M21 19a2 2 0 0 1-2 2h-1a2 2 0 0 1-2-2v-3a2 2 0 0 1 2-2h3zM3 19a2 2 0 0 0 2 2h1a2 2 0 0 0 2-2v-3a2 2 0 0 0-2-2H3z'
    ],
    posClass: 'chip-pos-2',
    duration: '6.2s',
    delay: '380ms',
    rotate: '6deg'
  },
  {
    label: '仅拆封',
    icon: [
      'M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z',
      'M8 13a4 4 0 1 0 8 0 4 4 0 1 0-8 0'
    ],
    posClass: 'chip-pos-3',
    duration: '8.4s',
    delay: '500ms',
    rotate: '-5deg'
  },
  {
    label: '自提',
    icon: ['M9 2h6l2 8H7z', 'M12 10v9', 'M8 21h8'],
    posClass: 'chip-pos-4',
    duration: '9.1s',
    delay: '180ms',
    rotate: '7deg'
  },
  {
    label: '可小刀',
    icon: ['M3 6h18v12H3z', 'M6 9h1M10 9h1M14 9h1M18 9h1M6 12h1M10 12h1M14 12h1M18 12h1M8 15h8'],
    posClass: 'chip-pos-5',
    duration: '7.1s',
    delay: '620ms',
    rotate: '-6deg'
  },
  {
    label: '同校面交',
    icon: ['M4 12a8 8 0 1 0 16 0 8 8 0 1 0-16 0', 'M4 12h16', 'M12 4v16'],
    posClass: 'chip-pos-6',
    duration: '8s',
    delay: '440ms',
    rotate: '5deg'
  },
  {
    label: '支持验货',
    icon: [
      'M8 12a4 4 0 1 0 8 0 4 4 0 1 0-8 0',
      'M9.5 8 9 3h6l-.5 5M9.5 16 9 21h6l-.5-5',
      'M12 10v2.5l1.5 1'
    ],
    posClass: 'chip-pos-7',
    duration: '6.8s',
    delay: '320ms',
    rotate: '4deg'
  },
  {
    label: '包邮',
    icon: [
      'M4 8h12v9a3 3 0 0 1-3 3H7a3 3 0 0 1-3-3z',
      'M16 10h2a2.5 2.5 0 0 1 0 5h-2',
      'M7 3v2M10 2.5v2.5M13 3v2'
    ],
    posClass: 'chip-pos-8',
    duration: '7.8s',
    delay: '700ms',
    rotate: '-7deg'
  }
]
</script>

<style scoped>
.brand-panel {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  min-height: 100vh;
  min-height: 100dvh;
  background: linear-gradient(165deg, #04241c 0%, #064e3b 38%, #047857 72%, #059669 100%);
}

/* ---------- 极光层 ---------- */
.aurora {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.aurora-blob {
  position: absolute;
  border-radius: 50%;
  will-change: transform;
}

.blob-1 {
  width: 62%;
  aspect-ratio: 1;
  left: -14%;
  top: -18%;
  background: radial-gradient(closest-side, rgba(52, 211, 153, 0.85), transparent);
}

.blob-2 {
  width: 54%;
  aspect-ratio: 1;
  right: -12%;
  top: 4%;
  background: radial-gradient(closest-side, rgba(94, 234, 212, 0.72), transparent);
}

.blob-3 {
  width: 72%;
  aspect-ratio: 1;
  left: 6%;
  bottom: -28%;
  background: radial-gradient(closest-side, rgba(16, 185, 129, 0.78), transparent);
}

.blob-4 {
  width: 40%;
  aspect-ratio: 1;
  right: 8%;
  bottom: 6%;
  background: radial-gradient(closest-side, rgba(167, 243, 208, 0.6), transparent);
}

/* 极光丝带：conic-gradient 缓慢旋转 */
.aurora-ribbon {
  position: absolute;
  inset: -30%;
  background: conic-gradient(
    from 180deg at 50% 50%,
    transparent 0deg,
    rgba(110, 231, 183, 0.18) 60deg,
    transparent 130deg,
    rgba(94, 234, 212, 0.14) 200deg,
    transparent 265deg,
    rgba(167, 243, 208, 0.16) 320deg,
    transparent 360deg
  );
  transform-origin: center;
  will-change: transform;
}

/* ---------- 噪点层 ---------- */
.noise {
  position: absolute;
  inset: 0;
  opacity: 0.05;
  pointer-events: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='180' height='180'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='180' height='180' filter='url(%23n)'/%3E%3C/svg%3E");
  background-size: 180px 180px;
}

/* ---------- 品牌文案 ---------- */
.brand-content {
  position: relative;
  z-index: 4;
  text-align: center;
  color: var(--color-text-inverse, #fff);
  padding: var(--space-8);
}

.brand-title {
  font-size: 3.25rem;
  font-weight: var(--font-extrabold, 800);
  letter-spacing: 6px;
  margin: 0 0 var(--space-2) 0;
  text-shadow: 0 2px 24px rgba(2, 44, 34, 0.35);
}

.brand-subtitle {
  font-size: var(--text-lg, 1rem);
  font-weight: var(--font-medium, 500);
  letter-spacing: 8px;
  opacity: 0.7;
  margin: 0 0 var(--space-6) 0;
}

.brand-slogan {
  font-size: var(--text-base, 0.9375rem);
  opacity: 0.72;
  letter-spacing: 1px;
  margin: 0;
}

/* ---------- 集市芯片层 ---------- */
.market-layer {
  position: absolute;
  inset: 0;
  z-index: 3;
  pointer-events: none;
}

.market-chip {
  position: absolute;
  /* 图层整体不拦截指针，但芯片本身要可悬停（否则 hover 缩放永远不会触发） */
  pointer-events: auto;
  /* 用独立的 rotate 属性承载倾角：与动画的 transform 互不干扰 */
  rotate: var(--chip-rotate, 0deg);
  will-change: transform;
}

.chip-inner {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: var(--radius-full, 999px);
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.22);
  color: rgba(236, 254, 255, 0.92);
  font-size: 12px;
  font-weight: var(--font-medium, 500);
  white-space: nowrap;
  box-shadow: 0 6px 18px rgba(2, 44, 34, 0.28);
  /* hover 缩放放在内层：外层正在跑漂浮动画，同一元素上两个 transform 会互相覆盖 */
  transition:
    transform var(--duration-normal, 200ms) var(--ease-out, ease),
    background-color var(--duration-normal, 200ms) var(--ease-out, ease),
    border-color var(--duration-normal, 200ms) var(--ease-out, ease);
}

.market-chip:hover .chip-inner {
  transform: scale(1.08);
  background: rgba(255, 255, 255, 0.18);
  border-color: rgba(167, 243, 208, 0.55);
}

.chip-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.chip-label {
  line-height: 1;
}

.chip-pos-1 {
  left: 8%;
  top: 17%;
}

.chip-pos-2 {
  right: 9%;
  top: 13%;
}

.chip-pos-3 {
  right: 5%;
  top: 46%;
}

.chip-pos-4 {
  left: 5%;
  top: 61%;
}

.chip-pos-5 {
  right: 11%;
  top: 76%;
}

.chip-pos-6 {
  left: 15%;
  top: 83%;
}

.chip-pos-7 {
  right: 4%;
  top: 29%;
}

.chip-pos-8 {
  left: 33%;
  top: 8%;
}

/* ---------- 动效：仅在「允许动效」时启用，基础样式保持完全可见 ---------- */
@media (prefers-reduced-motion: no-preference) {
  .aurora-blob {
    animation: aurora-drift 22s var(--ease-in-out, ease-in-out) infinite;
  }

  .blob-2 {
    animation-duration: 18s;
    animation-delay: -4s;
  }

  .blob-3 {
    animation-duration: 27s;
    animation-delay: -9s;
  }

  .blob-4 {
    animation-duration: 30s;
    animation-delay: -2s;
  }

  .aurora-ribbon {
    animation: aurora-spin 26s linear infinite;
  }

  .market-chip {
    animation-name: chip-fade-in, chip-float;
    animation-duration: 560ms, var(--float-duration, 7.5s);
    animation-timing-function: var(--ease-out, ease-out), var(--ease-in-out, ease-in-out);
    animation-delay: var(--float-delay, 0s), 0s;
    animation-iteration-count: 1, infinite;
    animation-fill-mode: both, none;
  }

  .brand-content > * {
    animation: brand-in 620ms var(--ease-out, ease-out) both;
  }

  .brand-subtitle {
    animation-delay: 90ms;
  }

  .brand-slogan {
    animation-delay: 180ms;
  }
}

@keyframes aurora-drift {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(3%, -4%, 0) scale(1.08);
  }
}

@keyframes aurora-spin {
  to {
    transform: rotate(360deg);
  }
}

@keyframes chip-fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes chip-float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-14px);
  }
}

@keyframes brand-in {
  from {
    opacity: 0;
    transform: translateY(14px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* ---------- 窄屏：品牌区被压缩成矮条，集市芯片层隐藏，避免与居中文案叠字 ---------- */
@media (max-width: 768px) {
  .brand-panel {
    flex: none;
    min-height: 0;
    padding: var(--space-10, 2.5rem) var(--space-5, 1.25rem) var(--space-8, 2rem);
  }

  .brand-title {
    font-size: 2.1rem;
    letter-spacing: 4px;
  }

  .brand-subtitle {
    font-size: var(--text-sm, 0.8125rem);
    letter-spacing: 6px;
  }

  .brand-slogan {
    font-size: var(--text-sm, 0.8125rem);
  }

  .market-layer {
    display: none;
  }
}
</style>