<template>
  <div class="not-found">
    <main class="main-content">
      <div class="container">
        <el-result icon="warning" title="404" sub-title="页面未找到" class="result-box">
          <template #extra>
            <p class="error-message">
              抱歉，您访问的页面不存在或已被移除。
            </p>
            <div class="error-actions">
              <el-button type="primary" size="large" @click="goHome">返回首页</el-button>
              <el-button size="large" @click="goProducts">浏览商品</el-button>
            </div>
          </template>
        </el-result>

        <div class="help-section">
          <p>您可以尝试：</p>
          <ul>
            <li>检查网址是否输入正确</li>
            <li>返回首页重新导航</li>
            <li>使用搜索功能查找您需要的内容</li>
            <li>如果问题持续存在，请联系客服支持</li>
          </ul>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}

function goProducts() {
  router.push('/products')
}
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.main-content {
  padding: 4rem 0;
  display: flex;
  align-items: center;
  min-height: calc(100vh - 200px);
}

.container {
  max-width: 700px;
  margin: 0 auto;
  padding: 0 1rem;
}

.result-box {
  background: white;
  border-radius: 16px;
  padding: 3rem 2rem;
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
}

.error-message {
  font-size: 1.125rem;
  color: #777;
  margin-bottom: 1.5rem;
}

.error-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-bottom: 0.5rem;
}

.help-section {
  text-align: left;
  background: #f9f9f9;
  padding: 1.5rem;
  border-radius: 8px;
  max-width: 500px;
  margin: 1.5rem auto 0;
}

.help-section p {
  font-weight: 600;
  color: #333;
  margin-bottom: 0.75rem;
}

.help-section ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.help-section li {
  padding: 0.5rem 0;
  color: #666;
  position: relative;
  padding-left: 1.5rem;
}

.help-section li:before {
  content: "→";
  position: absolute;
  left: 0;
  color: #4CAF50;
  font-weight: bold;
}
</style>
