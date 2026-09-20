<template>
  <div class="ad-edit-page">
    <div class="header">
      <button class="back-btn" @click="$router.back()">取消</button>
      <span class="header-title">广告编辑</span>
      <el-button type="primary" size="small" round :disabled="!canSubmit || submitting" @click="submitAd">
        {{ submitting ? '发布中...' : '发布' }}
      </el-button>
    </div>

    <div class="form-area">
      <section class="upload-section">
        <div class="section-label">广告图片（选填）</div>
        <ImageUploader v-model="imageUrls" :max-count="9" :max-size="10" />
      </section>

      <el-card shadow="never" class="form-card">
        <el-input v-model="title" placeholder="请输入广告标题（2-200字）" maxlength="200" class="title-input" />

        <!-- 内容类型选择 -->
        <div class="type-section">
          <div class="section-label">内容类型</div>
          <el-radio-group v-model="postType">
            <el-radio-button value="SHOWCASE">帖子展示</el-radio-button>
            <el-radio-button value="ACTIVITY">活动推广</el-radio-button>
          </el-radio-group>
        </div>

        <el-input
          v-model="content"
          type="textarea"
          :rows="8"
          maxlength="10000"
          show-word-limit
          class="content-input"
          :placeholder="postType === 'ACTIVITY' ? '描述你的活动内容，包括时间、地点、参与方式...' : '描述你的广告内容...'"
        />

        <div class="tag-section">
          <div class="section-label">标签（选填，最多 5 个）</div>
          <TagInput v-model="tags" :preset-tags="presetTags" :max-tags="5" placeholder="输入标签后按回车添加..." />
        </div>
      </el-card>

      <!-- 推流套餐选择 -->
      <section class="package-section">
        <div class="section-label">推流套餐</div>
        <div class="package-list">
          <div
            v-for="pkg in packages"
            :key="pkg.id"
            class="package-card"
            :class="{ active: selectedPackage === pkg.id, recommended: pkg.id === 'basic' }"
            @click="selectedPackage = pkg.id"
          >
            <div class="package-header">
              <span class="package-name">{{ pkg.name }}</span>
              <span class="package-badge" :class="pkg.id">{{ pkg.badge }}</span>
            </div>
            <div class="package-desc">{{ pkg.description }}</div>
            <div class="package-meta-row">
              <div class="package-price">
                <span class="price-symbol">¥</span>
                <span class="price-value">{{ pkg.price }}</span>
              </div>
              <span class="package-duration">{{ pkg.durationDays }}天</span>
            </div>
            <div class="package-detail-grid">
              <div class="detail-item">
                <span class="detail-label">推送间隔</span>
                <span class="detail-value highlight">每{{ pkg.interval }}条</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">首页Banner</span>
                <span class="detail-value" :class="{ highlight: pkg.hasBanner }">{{ pkg.hasBanner ? '有' : '无' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">推荐流</span>
                <span class="detail-value" :class="{ highlight: pkg.hasRecommendation }">{{ pkg.hasRecommendation ? '有' : '无' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">预计曝光</span>
                <span class="detail-value">{{ pkg.estimatedReach }}</span>
              </div>
            </div>
            <div class="package-scenario">
              <span class="scenario-label">适用场景</span>
              <span class="scenario-text">{{ pkg.scenario }}</span>
            </div>
            <div class="package-terms">
              <span class="terms-label">服务条款</span>
              <span class="terms-text">{{ pkg.terms }}</span>
            </div>
            <div v-if="selectedPackage === pkg.id" class="package-check">
              <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#fff" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
            </div>
          </div>
        </div>
      </section>

      <el-alert v-if="error" :title="error" type="error" :closable="false" show-icon class="error-alert" />
    </div>

    <!-- 付费确认弹窗 -->
    <el-dialog v-model="showPayModal" title="确认支付" width="400px" append-to-body>
      <div class="pay-modal-info">
        <div class="pay-info-row">
          <span>套餐</span>
          <span>{{ selectedPackageInfo?.name }}</span>
        </div>
        <div class="pay-info-row">
          <span>推送间隔</span>
          <span>每 {{ selectedPackageInfo?.interval }} 条内容出现1次</span>
        </div>
        <div class="pay-info-row">
          <span>首页Banner</span>
          <span>{{ selectedPackageInfo?.hasBanner ? '有' : '无' }}</span>
        </div>
        <div class="pay-info-row">
          <span>推荐流</span>
          <span>{{ selectedPackageInfo?.hasRecommendation ? '有' : '无' }}</span>
        </div>
        <div class="pay-info-row">
          <span>预计曝光</span>
          <span>{{ selectedPackageInfo?.estimatedReach }}</span>
        </div>
        <div class="pay-info-row">
          <span>有效期</span>
          <span>{{ selectedPackageInfo?.durationDays }}天</span>
        </div>
        <div class="pay-info-row total">
          <span>支付金额</span>
          <span class="pay-amount">¥{{ selectedPackageInfo?.price }}</span>
        </div>
      </div>
      <div class="pay-modal-notice">
        <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="#fa8c16" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <span>此为模拟支付，不会产生真实扣费</span>
      </div>
      <template #footer>
        <el-button @click="showPayModal = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="confirmPay">
          {{ paying ? '支付中...' : '确认支付' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adApi } from '../services/api'
import TagInput from '../components/TagInput.vue'
import ImageUploader from '../components/ImageUploader.vue'

const router = useRouter()
const title = ref('')
const content = ref('')
const error = ref(null)
const submitting = ref(false)
const imageUrls = ref([])
const tags = ref([])
const selectedPackage = ref('basic')
const postType = ref('SHOWCASE')
const showPayModal = ref(false)
const paying = ref(false)
const createdPostId = ref(null)

const presetTags = [
  '数码', '书籍', '生活', '运动', '美食', '旅行', '摄影',
  '音乐', '电影', '游戏', '学习', '求职', '考研', '留学',
  '二手', '闲置', '求助', '分享', '经验', '推广'
]

// 本地默认套餐，确保即使 API 不可用也能正常显示
const defaultPackages = [
  {
    id: 'trial',
    name: '体验推流',
    description: '首次推流体验，覆盖本校区活跃用户，适合试水推广',
    price: '1.90',
    exposureBoost: 2,
    durationDays: 1,
    interval: 5,
    hasBanner: false,
    hasRecommendation: false,
    badge: '体验',
    estimatedReach: '约 200-500 次曝光',
    scenario: '适合首次使用推流的用户，推广闲置物品、二手教材等',
    terms: '每人限购1次；曝光量受内容质量和发布时段影响；不支持退款'
  },
  {
    id: 'basic',
    name: '基础推流',
    description: '覆盖本校区及关联校区，持续3天稳定曝光',
    price: '4.90',
    exposureBoost: 3,
    durationDays: 3,
    interval: 4,
    hasBanner: true,
    hasRecommendation: false,
    badge: '推荐',
    estimatedReach: '约 800-2000 次曝光',
    scenario: '适合推广二手数码、社团活动、兼职招聘等个人发布',
    terms: '同一内容不可重复购买同套餐；曝光量为预估值，实际受内容质量影响；到期后自动停止推流'
  },
  {
    id: 'standard',
    name: '热门推流',
    description: '全平台推荐位优先展示，覆盖多个校区，持续7天',
    price: '14.90',
    exposureBoost: 5,
    durationDays: 7,
    interval: 3,
    hasBanner: true,
    hasRecommendation: true,
    badge: '热门',
    estimatedReach: '约 3000-8000 次曝光',
    scenario: '适合推广高价值商品、考研资料、校园周边服务等',
    terms: '可叠加使用（最多2次）；享有首页推荐位优先排序；到期后自动停止推流；内容违规将终止推流且不退款'
  },
  {
    id: 'premium',
    name: '校园爆款',
    description: '全平台置顶推荐，覆盖所有校区，持续14天长效曝光',
    price: '29.90',
    exposureBoost: 10,
    durationDays: 14,
    interval: 2,
    hasBanner: true,
    hasRecommendation: true,
    badge: '爆款',
    estimatedReach: '约 10000-30000 次曝光',
    scenario: '适合社团招新、商家入驻推广、大型活动宣传、毕业季清仓等',
    terms: '可叠加使用（最多2次）；享有首页置顶推荐位；专属客服支持；到期后自动停止推流；内容违规将终止推流且不退款'
  }
]

const packages = ref(defaultPackages)

const canSubmit = computed(() => title.value.trim().length >= 2 && content.value.trim().length > 0 && !submitting.value)

const selectedPackageInfo = computed(() => packages.value.find(p => p.id === selectedPackage.value))

onMounted(async () => {
  try {
    const res = await adApi.getPackages()
    if (res.code === 200 && res.data && res.data.length > 0) {
      packages.value = res.data
    }
  } catch {
    // API 不可用时使用本地默认套餐，已通过 defaultPackages 初始化
  }
})


async function submitAd() {
  if (!canSubmit.value) return
  error.value = null
  submitting.value = true
  try {
    const res = await adApi.createAd({
      title: title.value.trim(),
      content: content.value.trim(),
      imageUrls: imageUrls.value.length > 0 ? imageUrls.value : null,
      coverImage: imageUrls.value.length > 0 ? imageUrls.value[0] : null,
      tags: tags.value.length > 0 ? tags.value.join(',') : null,
      packageId: selectedPackage.value,
      postType: postType.value
    })
    if (res.code === 200) {
      createdPostId.value = res.data.id
      showPayModal.value = true
    } else {
      error.value = res.message || '发布失败'
    }
  } catch (e) {
    // 优先显示 API 返回的具体错误消息，兜底显示网络连接提示
    error.value = e?.message || '发布失败，请检查网络连接'
  } finally {
    submitting.value = false
  }
}

async function confirmPay() {
  if (!createdPostId.value) return
  paying.value = true
  try {
    const res = await adApi.simulatePayment(createdPostId.value, selectedPackage.value)
      if (res.code === 200) {
        showPayModal.value = false
        if (postType.value === 'ACTIVITY') {
          router.push(`/activities/${createdPostId.value}`)
        } else {
          router.push(`/community/posts/${createdPostId.value}`)
        }
    } else {
      error.value = res.message || '支付失败'
    }
  } catch (e) {
    error.value = e?.message || '支付失败，请重试'
  } finally {
    paying.value = false
  }
}
</script>

<style scoped>
.ad-edit-page { min-height: 100vh; background: #f5f5f5; padding-bottom: 40px; }
.header {
  display: flex; align-items: center; padding: 12px 16px;
  background: #fff; border-bottom: 1px solid #f0f0f0;
  position: sticky; top: 0; z-index: 10;
}
.back-btn { background: none; border: none; font-size: 15px; color: #666; cursor: pointer; }
.header-title { flex: 1; text-align: center; font-size: 16px; font-weight: 600; }
.form-area { padding: 16px; max-width: 760px; margin: 0 auto; }
.section-label { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 10px; }

.upload-section { background: #fff; border-radius: 12px; padding: 16px; margin-bottom: 16px; }

.form-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.title-input :deep(.el-input__wrapper) {
  box-shadow: none;
  padding-left: 0;
  font-size: 18px;
  font-weight: 600;
}

.type-section { margin-bottom: 16px; }

.content-input :deep(.el-textarea__inner) {
  font-size: 15px;
  line-height: 1.8;
}

.tag-section { margin-top: 16px; }

/* 推流套餐 */
.package-section { background: #fff; border-radius: 12px; padding: 16px; margin-top: 16px; }
.package-list { display: flex; flex-direction: column; gap: 12px; }
.package-card {
  position: relative; padding: 16px; border: 2px solid #e8e8e8; border-radius: 12px;
  cursor: pointer; transition: all 0.2s ease; background: #fafafa;
}
.package-card.active {
  border-color: var(--color-primary-500, #10b981); background: #fff8f2;
  box-shadow: 0 2px 12px rgba(16, 185, 129, 0.15);
}
.package-card.recommended:not(.active) {
  border-color: #ffd591;
}
.package-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.package-name { font-size: 16px; font-weight: 700; color: #333; }
.package-badge {
  font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; color: #fff;
}
.package-badge.trial { background: #8c8c8c; }
.package-badge.basic { background: linear-gradient(135deg, #1890ff, #40a9ff); }
.package-badge.standard { background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399)); }
.package-badge.premium { background: linear-gradient(135deg, #f5222d, #ff4d4f); }
.package-desc { font-size: 13px; color: #666; margin-bottom: 10px; line-height: 1.5; }
.package-meta-row { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 10px; }
.package-price { display: flex; align-items: baseline; gap: 2px; }
.price-symbol { font-size: 14px; color: var(--color-primary-500, #10b981); font-weight: 600; }
.price-value { font-size: 24px; color: var(--color-primary-500, #10b981); font-weight: 700; }
.package-duration { font-size: 13px; color: #999; }
.package-detail-grid {
  display: grid; grid-template-columns: 1fr 1fr; gap: 8px;
  padding: 10px 0; border-top: 1px solid #f0f0f0; border-bottom: 1px solid #f0f0f0;
  margin-bottom: 10px;
}
.detail-item { display: flex; flex-direction: column; gap: 2px; }
.detail-label { font-size: 11px; color: #999; }
.detail-value { font-size: 13px; color: #333; font-weight: 500; }
.detail-value.highlight { color: var(--color-primary-500, #10b981); font-weight: 700; }
.package-scenario { margin-bottom: 6px; }
.scenario-label { font-size: 11px; color: #999; margin-right: 6px; }
.scenario-text { font-size: 12px; color: #666; line-height: 1.5; }
.terms-label { font-size: 11px; color: #bbb; margin-right: 6px; }
.terms-text { font-size: 11px; color: #999; line-height: 1.5; }
.package-check {
  position: absolute; top: 12px; right: 12px; width: 24px; height: 24px;
  background: var(--color-primary-500, #10b981); border-radius: 50%; display: flex;
  align-items: center; justify-content: center;
}

/* 付费弹窗 */
.pay-modal-info { margin-bottom: 16px; }
.pay-info-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 0; border-bottom: 1px solid #f5f5f5; font-size: 14px; color: #666;
}
.pay-info-row.total { border-bottom: none; padding-top: 14px; }
.pay-amount { font-size: 22px; font-weight: 700; color: var(--color-primary-500, #10b981); }
.pay-modal-notice {
  display: flex; align-items: center; gap: 6px;
  padding: 10px 12px; background: #fffbe6; border-radius: 8px;
  margin-bottom: 16px; font-size: 12px; color: #fa8c16;
}

.error-alert { margin-top: 16px; }
</style>
