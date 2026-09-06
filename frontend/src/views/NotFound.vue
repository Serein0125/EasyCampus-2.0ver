<template>
  <div class="not-found">
    <NavBar />
    
    <main class="main-content">
      <div class="container">
        <div class="error-container">
          <div class="error-icon">🔍</div>
          <h1 class="error-title">404</h1>
          <h2 class="error-subtitle">页面未找到</h2>
          <p class="error-message">
            抱歉，您访问的页面不存在或已被移除。
          </p>
          
          <div class="error-actions">
            <router-link to="/" class="btn btn-primary">
              <span>🏠</span> 返回首页
            </router-link>
            <router-link to="/products" class="btn btn-outline">
              <span>📦</span> 浏览商品
            </router-link>
          </div>
          
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
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import NavBar from '../components/NavBar.vue'
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

.error-container {
  background: white;
  border-radius: 16px;
  padding: 4rem 3rem;
  text-align: center;
  box-shadow: 0 10px 40px rgba(0,0,0,0.1);
}

.error-icon {
  font-size: 5rem;
  margin-bottom: 1.5rem;
  animation: bounce 2s infinite;
}

@keyframes bounce {
  0%, 20%, 50%, 80%, 100% {
    transform: translateY(0);
  }
  40% {
    transform: translateY(-20px);
  }
  60% {
    transform: translateY(-10px);
  }
}

.error-title {
  font-size: 6rem;
  font-weight: 800;
  color: #e74c3c;
  margin: 0;
  line-height: 1;
}

.error-subtitle {
  font-size: 2rem;
  color: #555;
  margin: 1rem 0 0.5rem;
}

.error-message {
  font-size: 1.125rem;
  color: #777;
  margin-bottom: 2.5rem;
}

.error-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  margin-bottom: 3rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.875rem 2rem;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  text-decoration: none;
  transition: all 0.3s;
}

.btn-primary {
  background: #4CAF50;
  color: white;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.btn-primary:hover {
  background: #45a049;
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.4);
}

.btn-outline {
  background: white;
  border: 2px solid #ddd;
  color: #555;
}

.btn-outline:hover {
  border-color: #4CAF50;
  color: #4CAF50;
  transform: translateY(-2px);
}

.help-section {
  text-align: left;
  background: #f9f9f9;
  padding: 1.5rem;
  border-radius: 8px;
  max-width: 500px;
  margin: 0 auto;
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
