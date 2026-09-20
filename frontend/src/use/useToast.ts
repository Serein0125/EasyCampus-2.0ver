import { ElMessage, ElMessageBox } from 'element-plus'

// 全局提示 API：内部基于 Element Plus 的 ElMessage / ElMessageBox 实现，
// 对外保持 showToast / showConfirm 签名不变，调用方零改动。
interface ToastApi {
  showToast: (message: string, type?: string, duration?: number) => void
  showConfirm: (message: string) => Promise<boolean>
}

export function useToast(): ToastApi {
  return {
    showToast(message, type = 'info', duration = 3000) {
      ElMessage({ message, type: type as any, duration })
    },
    showConfirm(message) {
      return ElMessageBox.confirm(message, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => true).catch(() => false)
    }
  }
}
