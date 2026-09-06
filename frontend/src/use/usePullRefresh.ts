import { ref, onMounted, onUnmounted } from 'vue'

interface PullRefreshOptions {
  threshold?: number
}

/**
 * 下拉刷新组合式函数
 * 监听触摸事件实现下拉刷新交互
 * @param onRefresh - 刷新回调，需返回 Promise
 * @param options - 配置项
 * @param options.threshold - 触发刷新的下拉距离（默认 60px）
 */
export function usePullRefresh(onRefresh: () => Promise<void> | void, options: PullRefreshOptions = {}) {
  const threshold = options.threshold || 60

  const isPulling = ref(false)       // 正在下拉中
  const isRefreshing = ref(false)    // 正在刷新中
  const pullDistance = ref(0)        // 当前下拉距离
  const canTrigger = ref(false)      // 是否达到触发阈值

  let startY = 0
  let scrollContainer: HTMLElement | null = null

  /** 判断滚动容器是否在顶部 */
  function isAtTop(): boolean {
    if (!scrollContainer) return true
    return scrollContainer.scrollTop <= 0
  }

  function onTouchStart(e: TouchEvent) {
    // 只有在页面顶部时才启用下拉
    if (!isAtTop() || isRefreshing.value) return
    startY = e.touches[0].clientY
    isPulling.value = true
  }

  function onTouchMove(e: TouchEvent) {
    if (!isPulling.value || isRefreshing.value) return

    const currentY = e.touches[0].clientY
    const diff = currentY - startY

    // 只处理向下拉
    if (diff <= 0) {
      pullDistance.value = 0
      canTrigger.value = false
      return
    }

    // 添加阻尼效果：越拉越难拉
    pullDistance.value = Math.min(diff * 0.5, threshold * 2)
    canTrigger.value = pullDistance.value >= threshold

    // 阻止默认行为避免页面弹跳
    if (isAtTop()) {
      e.preventDefault()
    }
  }

  async function onTouchEnd() {
    if (!isPulling.value) return
    isPulling.value = false

    // 达到阈值则触发刷新
    if (canTrigger.value && !isRefreshing.value) {
      isRefreshing.value = true
      pullDistance.value = threshold // 固定在阈值位置

      try {
        await onRefresh()
      } finally {
        isRefreshing.value = false
        pullDistance.value = 0
        canTrigger.value = false
      }
    } else {
      // 未达到阈值，回弹
      pullDistance.value = 0
      canTrigger.value = false
    }
  }

  /**
   * 绑定滚动容器
   * @param el - 滚动容器元素（默认为 document.scrollingElement）
   */
  function bindContainer(el?: HTMLElement | null) {
    scrollContainer = el || (document.scrollingElement as HTMLElement | null)
    if (scrollContainer) {
      scrollContainer.addEventListener('touchstart', onTouchStart, { passive: true })
      scrollContainer.addEventListener('touchmove', onTouchMove, { passive: false })
      scrollContainer.addEventListener('touchend', onTouchEnd, { passive: true })
    }
  }

  function unbindContainer() {
    if (scrollContainer) {
      scrollContainer.removeEventListener('touchstart', onTouchStart)
      scrollContainer.removeEventListener('touchmove', onTouchMove)
      scrollContainer.removeEventListener('touchend', onTouchEnd)
    }
  }

  onMounted(() => {
    // 默认绑定到 window（适用于页面级滚动）
    scrollContainer = (document.scrollingElement as HTMLElement | null) || document.documentElement
    window.addEventListener('touchstart', onTouchStart, { passive: true })
    window.addEventListener('touchmove', onTouchMove, { passive: false })
    window.addEventListener('touchend', onTouchEnd, { passive: true })
  })

  onUnmounted(() => {
    window.removeEventListener('touchstart', onTouchStart)
    window.removeEventListener('touchmove', onTouchMove)
    window.removeEventListener('touchend', onTouchEnd)
    unbindContainer()
  })

  return {
    isPulling,
    isRefreshing,
    pullDistance,
    canTrigger,
    bindContainer
  }
}