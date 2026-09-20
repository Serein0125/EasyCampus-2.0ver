import { ref, type Ref } from 'vue'

// 系统级「减少动效」偏好的全局侦听。
// 做成模块级单例：整个应用只创建一个 matchMedia 监听，避免每个组件各建一份；
// 同时把 change 事件同步进响应式 ref，供各处组合式函数与组件复用。
const QUERY = '(prefers-reduced-motion: reduce)'

const mediaQueryList =
  typeof window !== 'undefined' && typeof window.matchMedia === 'function'
    ? window.matchMedia(QUERY)
    : null

const prefersReducedMotion = ref(mediaQueryList ? mediaQueryList.matches : false)

mediaQueryList?.addEventListener('change', (event) => {
  prefersReducedMotion.value = event.matches
})

/**
 * 是否偏好减少动效。
 * 注意：CSS 侧的动效由媒体查询自行处理，这里主要给 rAF 驱动的效果
 * （Canvas 粒子场、卡片柔光、磁性按钮）使用，CSS 管不到它们。
 */
export function useReducedMotion(): Ref<boolean> {
  return prefersReducedMotion
}