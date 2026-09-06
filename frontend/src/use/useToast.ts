import { inject } from 'vue'

// 由 ToastProvider 通过 provide 注入的全局提示能力
interface ToastApi {
  showToast: (message: string, type?: string, duration?: number) => void
  showConfirm: (message: string) => Promise<boolean>
}

export function useToast(): ToastApi {
  const toast = inject<ToastApi>('toast', {
    showToast: () => {},
    showConfirm: () => Promise.resolve(false)
  })
  return toast
}