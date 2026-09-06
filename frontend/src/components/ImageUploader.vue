<template>
  <div class="image-uploader">
    <div class="image-grid">
      <div
        v-for="(img, idx) in imageList"
        :key="img.id"
        class="image-item"
        :class="{ 'has-error': img.status === 'error' }"
      >
        <img :src="img.preview || img.url" class="preview-img" alt="" />

        <!-- 上传中遮罩 -->
        <div v-if="img.status === 'uploading'" class="uploading-mask">
          <!-- 不定态加载动画（Cloudflare代理时无法获取进度） -->
          <div v-if="img.indeterminate" class="indeterminate-ring">
            <svg viewBox="0 0 36 36">
              <path class="progress-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
              <path class="progress-bar-indeterminate" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831" />
            </svg>
            <span class="progress-text">...</span>
          </div>
          <!-- 确定进度条 -->
          <div v-else class="progress-ring">
            <svg viewBox="0 0 36 36">
              <path class="progress-bg" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
              <path class="progress-bar" :stroke-dasharray="`${img.progress}, 100`" d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831" />
            </svg>
            <span class="progress-text">{{ img.progress }}%</span>
          </div>
        </div>

        <!-- 上传失败遮罩 -->
        <div v-if="img.status === 'error'" class="error-mask">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="#fff" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/>
          </svg>
          <span class="error-text">{{ img.errorMsg || '上传失败' }}</span>
          <button class="retry-btn" @click="retryUpload(idx)">重试</button>
        </div>

        <!-- 上传成功标识 -->
        <div v-if="img.status === 'success'" class="success-badge">
          <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#fff" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
        </div>

        <!-- 删除按钮 -->
        <button class="remove-btn" @click="removeImage(idx)">
          <svg viewBox="0 0 24 24" width="12" height="12" fill="none" stroke="#fff" stroke-width="3"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>

      <!-- 上传触发器 -->
      <label v-if="imageList.length < maxCount" class="upload-trigger" :class="{ disabled: uploading }">
        <input
          type="file"
          :accept="accept"
          multiple
          @change="handleFileSelect"
          hidden
          :disabled="uploading"
        />
        <svg viewBox="0 0 24 24" width="32" height="32" fill="none" stroke="#bbb" stroke-width="2">
          <rect x="3" y="3" width="18" height="18" rx="2"/>
          <circle cx="8.5" cy="8.5" r="1.5"/>
          <polyline points="21,15 16,10 5,21"/>
        </svg>
        <span class="upload-count">{{ imageList.length }}/{{ maxCount }}</span>
      </label>
    </div>
    <p class="upload-tip">支持 JPG/PNG/WEBP，单张不超过{{ maxSize }}MB，最多{{ maxCount }}张</p>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { uploadApi } from '../services/api'

const CHUNK_THRESHOLD = 5 * 1024 * 1024 // 超过5MB启用分片上传
const CHUNK_SIZE = 2 * 1024 * 1024 // 每个分片2MB
const CHUNK_CONCURRENCY = 3 // 分片并发上传数

// 客户端压缩配置（在上传前缩小图片体积，大幅减少上传时间）
const COMPRESS_MAX_WIDTH = 1920 // 压缩后最大宽度
const COMPRESS_MAX_HEIGHT = 1920 // 压缩后最大高度
const COMPRESS_QUALITY = 0.75 // JPEG压缩质量（0-1）
const COMPRESS_MIN_SIZE = 300 * 1024 // 小于此大小的图片不压缩（300KB）

const props = defineProps({
  maxCount: { type: Number, default: 9 },
  maxSize: { type: Number, default: 10 },
  accept: { type: String, default: 'image/jpeg,image/png,image/webp' },
  autoUpload: { type: Boolean, default: true },
  placeholder: { type: String, default: '' }
})

const model = defineModel({ type: Array, default: () => [] })
const emit = defineEmits(['upload-success', 'upload-error', 'all-uploaded'])

const imageList = ref([])
let idCounter = 0

const uploading = computed(() => imageList.value.some(img => img.status === 'uploading'))

// 监听外部 modelValue 变化（初始化时）
watch(model, (urls) => {
  if (urls && urls.length > 0 && imageList.value.length === 0) {
    imageList.value = urls.map(url => ({
      id: ++idCounter,
      file: null,
      url: url,
      preview: url,
      status: 'success',
      progress: 100,
      errorMsg: ''
    }))
  }
}, { immediate: true })

// 图片URL变化时通知父组件
function emitUrls() {
  const urls = imageList.value
    .filter(img => img.status === 'success' && img.url)
    .map(img => img.url)
  model.value = urls
}

// 计算缩放后尺寸（保持宽高比）
function calculateCompressSize(srcWidth, srcHeight) {
  let width = srcWidth
  let height = srcHeight
  if (width > COMPRESS_MAX_WIDTH) {
    height = Math.round(height * COMPRESS_MAX_WIDTH / width)
    width = COMPRESS_MAX_WIDTH
  }
  if (height > COMPRESS_MAX_HEIGHT) {
    width = Math.round(width * COMPRESS_MAX_HEIGHT / height)
    height = COMPRESS_MAX_HEIGHT
  }
  return { width, height }
}

// 客户端 Canvas 压缩图片（缩放 + 质量压缩），减少上传体积
// 返回压缩后的 File 对象，压缩失败时返回原文件
async function compressImageBeforeUpload(file) {
  // 小图片或非图片文件不压缩
  if (file.size < COMPRESS_MIN_SIZE || !file.type.startsWith('image/')) {
    return file
  }

  return new Promise((resolve) => {
    const imageUrl = URL.createObjectURL(file)
    const img = new Image()

    img.onload = () => {
      URL.revokeObjectURL(imageUrl)

      // 原图尺寸已经很小，直接返回原文件
      if (img.width <= COMPRESS_MAX_WIDTH && img.height <= COMPRESS_MAX_HEIGHT && file.size < COMPRESS_MIN_SIZE * 2) {
        resolve(file)
        return
      }

      const { width, height } = calculateCompressSize(img.width, img.height)
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      const ctx = canvas.getContext('2d')
      // 白色背景（处理PNG透明图转为JPEG时背景变黑的问题）
      ctx.fillStyle = '#FFFFFF'
      ctx.fillRect(0, 0, width, height)
      ctx.drawImage(img, 0, 0, width, height)

      canvas.toBlob((blob) => {
        if (!blob) {
          // Canvas 导出失败（极少情况），使用原文件
          resolve(file)
          return
        }
        // 创建新 File 对象，统一转为 JPEG 以获得最大压缩
        const newName = file.name.replace(/\.[^.]+$/, '.jpg')
        const compressedFile = new File([blob], newName, {
          type: 'image/jpeg',
          lastModified: Date.now()
        })
        resolve(compressedFile)
      }, 'image/jpeg', COMPRESS_QUALITY)
    }

    img.onerror = () => {
      URL.revokeObjectURL(imageUrl)
      resolve(file) // 加载失败时使用原文件
    }

    img.src = imageUrl
  })
}

// 文件选择处理
async function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  const allowedTypes = props.accept.split(',')
  const maxSizeBytes = props.maxSize * 1024 * 1024

  for (const file of files) {
    if (imageList.value.length >= props.maxCount) break

    // 格式验证
    if (!allowedTypes.includes(file.type)) {
      const img = createImageItem(file)
      img.status = 'error'
      img.errorMsg = '不支持的格式'
      imageList.value.push(img)
      emit('upload-error', { error: '不支持的格式', index: imageList.value.length - 1 })
      continue
    }

    // 大小验证
    if (file.size > maxSizeBytes) {
      const img = createImageItem(file)
      img.status = 'error'
      img.errorMsg = `超过${props.maxSize}MB`
      imageList.value.push(img)
      emit('upload-error', { error: `文件超过${props.maxSize}MB限制`, index: imageList.value.length - 1 })
      continue
    }

    // 客户端压缩图片，大幅减少上传体积
    const compressedFile = await compressImageBeforeUpload(file)
    const img = createImageItem(compressedFile)
    // 生成本地预览（用压缩后的文件生成 blob URL）
    img.preview = URL.createObjectURL(compressedFile)
    imageList.value.push(img)

    if (props.autoUpload) {
      uploadSingleImage(img, imageList.value.length - 1)
    }
  }

  input.value = ''
}

function createImageItem(file) {
  return {
    id: ++idCounter,
    file: file,
    url: '',
    preview: '',
    status: 'pending',
    progress: 0,
    indeterminate: false,
    errorMsg: ''
  }
}

// 上传单张图片
async function uploadSingleImage(img, idx) {
  if (!img.file) return

  img.status = 'uploading'
  img.progress = 0
  img.indeterminate = true  // 默认不定态（Cloudflare代理时total为0）
  img.errorMsg = ''

  try {
    let url
    // 大文件使用分片上传
    if (img.file.size > CHUNK_THRESHOLD) {
      url = await uploadWithChunks(img.file, (progress) => {
        img.progress = progress
        img.indeterminate = false
      })
    } else {
      // 普通上传
      const res = await uploadApi.uploadImage(img.file, (evt) => {
        if (evt.total && evt.total > 0) {
          // total有效时显示真实百分比
          img.progress = Math.round((evt.loaded / evt.total) * 100)
          img.indeterminate = false
        }
        // total为0时保持不定态动画（Cloudflare CDN代理导致）
      })
      if (res.code === 200 && res.data && res.data.url) {
        url = res.data.url
      } else {
        throw new Error(res.message || '上传失败')
      }
    }

    img.url = url
    img.status = 'success'
    img.progress = 100
    img.indeterminate = false
    emit('upload-success', { url, index: idx })
    emitUrls()
  } catch (err) {
    img.status = 'error'
    img.errorMsg = getErrorMessage(err)
    img.indeterminate = false
    emit('upload-error', { error: img.errorMsg, index: idx })
  }

  checkAllUploaded()
}

// 分片上传（并发上传多个分片，加快大文件上传速度）
// 大文件 >5MB 走分片：把文件切成 2MB 的块，3 路并发上传，全部完成后通知后端合并
async function uploadWithChunks(file, onProgress) {
  const fileId = generateFileId()
  const totalChunks = Math.ceil(file.size / CHUNK_SIZE)
  let completedChunks = 0

  // 预先把所有分片切好（File.slice 是惰性的，不立即占内存，仅记录偏移）
  const tasks = []
  for (let i = 0; i < totalChunks; i++) {
    const start = i * CHUNK_SIZE
    const end = Math.min(start + CHUNK_SIZE, file.size)
    const chunk = file.slice(start, end)
    tasks.push({ chunk, index: i })
  }

  // 并发控制：用 Set 当「在途任务池」，size 即当前并发数
  // 思路：每起一个任务就 add 进集合；达到并发上限就 await Promise.race 等任意一个结束；
  // 任务完成时在 finally 里 delete 掉自己，腾出名额给下一个。比手写计数器更稳健。
  const executing = new Set()
  for (const task of tasks) {
    const promise = (async () => {
      try {
        await uploadApi.uploadChunk(task.chunk, fileId, task.index, totalChunks, file.name)
      } catch (err) {
        throw new Error(`分片${task.index + 1}上传失败: ${getErrorMessage(err)}`)
      }
      completedChunks++
      onProgress(Math.round((completedChunks / totalChunks) * 100))
    })()

    executing.add(promise)
    // finally 保证无论成功/失败都从池中移除，避免池子只增不减
    promise.finally(() => executing.delete(promise))

    // 达到并发上限时，等待任一任务完成后再继续投递下一个
    if (executing.size >= CHUNK_CONCURRENCY) {
      await Promise.race(executing)
    }
  }

  // 等待剩余任务全部完成（含 race 之后还留在池里的）
  await Promise.all(executing)

  // 全部分片上传完成后，请求后端合并分片并返回最终 URL
  const mergeRes = await uploadApi.mergeChunks(fileId, file.name, totalChunks)
  if (mergeRes.code === 200 && mergeRes.data && mergeRes.data.url) {
    return mergeRes.data.url
  }
  throw new Error(mergeRes.message || '合并分片失败')
}

// 重试上传
function retryUpload(idx) {
  const img = imageList.value[idx]
  if (img && img.file) {
    uploadSingleImage(img, idx)
  }
}

// 删除图片
function removeImage(idx) {
  const img = imageList.value[idx]
  if (img.preview && img.preview.startsWith('blob:')) {
    URL.revokeObjectURL(img.preview)
  }
  imageList.value.splice(idx, 1)
  emitUrls()
}

// 检查是否全部上传完成
function checkAllUploaded() {
  const allDone = imageList.value.every(img => img.status === 'success' || img.status === 'error')
  if (allDone && imageList.value.length > 0) {
    emit('all-uploaded')
  }
}

// 生成文件唯一ID
function generateFileId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2, 9)
}

// 友好的错误消息（将后端返回的技术性错误转为用户可理解的提示）
function getErrorMessage(err) {
  if (!err) return '上传失败'
  if (typeof err === 'string') return err

  if (err.message) {
    // 后端返回的具体错误消息（优先透传，这些已经是中文且对用户友好）
    if (err.message.includes('无写入权限')) return '服务器存储异常，请联系管理员'
    if (err.message.includes('无写权限')) return '服务器存储异常，请联系管理员'
    if (err.message.includes('磁盘空间')) return '服务器磁盘空间不足，请联系管理员'
    if (err.message.includes('超过') || err.message.includes('大小')) return err.message
    if (err.message.includes('格式') || err.message.includes('不支持')) return err.message
    if (err.message.includes('timeout') || err.code === 'TIMEOUT') return '上传超时，请重试'
    if (err.message.includes('Network') || err.code === 'NETWORK_ERROR') return '网络异常，请检查连接'
    // 后端返回的用户友好消息直接展示
    if (err.message.startsWith('上传失败:') || err.message.startsWith('头像上传失败:') ||
        err.message.startsWith('分片上传失败:') || err.message.startsWith('合并分片失败:') ||
        err.message.startsWith('文件存储失败:')) {
      return err.message
    }
    return err.message
  }
  return '上传失败，请重试'
}

// 暴露方法给父组件
defineExpose({
  // 手动触发上传
  async startUpload() {
    for (let i = 0; i < imageList.value.length; i++) {
      const img = imageList.value[i]
      if (img.status === 'pending' && img.file) {
        await uploadSingleImage(img, i)
      }
    }
  },
  // 获取已上传的URL列表
  getUploadedUrls() {
    return imageList.value
      .filter(img => img.status === 'success' && img.url)
      .map(img => img.url)
  },
  // 是否正在上传
  isUploading() {
    return uploading.value
  },
  // 是否有上传失败的
  hasErrors() {
    return imageList.value.some(img => img.status === 'error')
  }
})
</script>

<style scoped>
.image-uploader { width: 100%; }
.image-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
.image-item {
  position: relative; aspect-ratio: 1; border-radius: 8px;
  overflow: hidden; background: #f5f5f5;
}
.image-item.has-error { border: 2px solid #ff4d4f; }
.preview-img { width: 100%; height: 100%; object-fit: cover; }

/* 上传中遮罩 */
.uploading-mask {
  position: absolute; inset: 0; background: rgba(0,0,0,0.5);
  display: flex; align-items: center; justify-content: center;
}
.progress-ring { position: relative; width: 48px; height: 48px; }
.progress-ring svg { width: 48px; height: 48px; transform: rotate(-90deg); }
.progress-bg { fill: none; stroke: rgba(255,255,255,0.2); stroke-width: 3; }
.progress-bar { fill: none; stroke: #fff; stroke-width: 3; stroke-linecap: round; transition: stroke-dasharray 0.3s; }
.progress-text {
  position: absolute; inset: 0; display: flex; align-items: center;
  justify-content: center; color: #fff; font-size: 11px; font-weight: 700;
}

/* 不定态加载动画 */
.indeterminate-ring { position: relative; width: 48px; height: 48px; }
.indeterminate-ring svg { width: 48px; height: 48px; animation: spin 1.5s linear infinite; }
.progress-bar-indeterminate {
  fill: none; stroke: #fff; stroke-width: 3; stroke-linecap: round;
  stroke-dasharray: 25, 75; animation: dash 1.8s ease-in-out infinite;
}
@keyframes spin { 100% { transform: rotate(360deg); } }
@keyframes dash {
  0% { stroke-dashoffset: 100; }
  50% { stroke-dashoffset: 0; }
  100% { stroke-dashoffset: -100; }
}

/* 上传失败遮罩 */
.error-mask {
  position: absolute; inset: 0; background: rgba(255,77,79,0.75);
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; gap: 4px; padding: 8px;
}
.error-text { color: #fff; font-size: 10px; text-align: center; line-height: 1.3; }
.retry-btn {
  padding: 3px 12px; border-radius: 10px; border: 1px solid rgba(255,255,255,0.6);
  background: rgba(255,255,255,0.2); color: #fff; font-size: 11px; cursor: pointer;
}
.retry-btn:active { background: rgba(255,255,255,0.4); }

/* 上传成功标识 */
.success-badge {
  position: absolute; bottom: 4px; right: 4px; width: 20px; height: 20px;
  background: #52c41a; border-radius: 50%; display: flex;
  align-items: center; justify-content: center;
}

/* 删除按钮 */
.remove-btn {
  position: absolute; top: 4px; right: 4px; width: 22px; height: 22px;
  background: rgba(0,0,0,0.5); border-radius: 50%; display: flex;
  align-items: center; justify-content: center; cursor: pointer; border: none;
  z-index: 2;
}
.remove-btn:active { background: rgba(255,77,79,0.8); }

/* 上传触发器 */
.upload-trigger {
  aspect-ratio: 1; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 4px;
  background: #fafafa; border: 2px dashed #ddd; border-radius: 8px; cursor: pointer;
  transition: all 0.2s;
}
.upload-trigger:active { background: #FFF7E6; border-color: #FFD591; }
.upload-trigger.disabled { opacity: 0.5; cursor: not-allowed; }
.upload-count { font-size: 12px; color: #bbb; }
.upload-tip { margin-top: 8px; font-size: 12px; color: #999; }
</style>
