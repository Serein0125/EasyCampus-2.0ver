<template>
  <div class="db-test-page">
    <div class="page-header">
      <h1>数据库连接测试</h1>
      <p class="subtitle">Database Connection Tester</p>
    </div>

    <div class="test-container">
      <!-- 连接状态卡片 -->
      <div class="card status-card" :class="statusClass">
        <div class="card-header">
          <span class="icon">{{ dbConnected ? '🟢' : '🔴' }}</span>
          <h2>连接状态</h2>
        </div>
        <div class="card-body">
          <p class="status-text">{{ dbStatusMessage }}</p>
          <button @click="fetchDbStatus" :disabled="loading" class="btn btn-primary">
            {{ loading ? '检测中...' : '重新检测' }}
          </button>
        </div>
      </div>

      <!-- 数据库信息 -->
      <div v-if="dbInfo" class="card info-card">
        <div class="card-header">
          <span class="icon">📊</span>
          <h2>数据库信息</h2>
        </div>
        <div class="card-body">
          <table class="info-table">
            <tr><td class="label">数据库类型</td><td>{{ dbInfo.databaseProductName || '-' }}</td></tr>
            <tr><td class="label">数据库版本</td><td>{{ dbInfo.databaseProductVersion || '-' }}</td></tr>
            <tr><td class="label">驱动名称</td><td>{{ dbInfo.driverName || '-' }}</td></tr>
            <tr><td class="label">驱动版本</td><td>{{ dbInfo.driverVersion || '-' }}</td></tr>
            <tr><td class="label">连接URL</td><td class="mono">{{ dbInfo.url || '-' }}</td></tr>
            <tr><td class="label">连接用户</td><td>{{ dbInfo.username || '-' }}</td></tr>
          </table>
        </div>
      </div>

      <!-- 表数据量 -->
      <div v-if="tableCounts" class="card table-card">
        <div class="card-header">
          <span class="icon">📋</span>
          <h2>表数据量</h2>
        </div>
        <div class="card-body">
          <div class="table-grid">
            <div v-for="(count, table) in tableCounts" :key="table" class="table-item">
              <span class="table-name">{{ table }}</span>
              <span class="table-count" :class="count === '0' ? 'zero' : ''">{{ count }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 错误信息 -->
      <div v-if="errorInfo" class="card error-card">
        <div class="card-header">
          <span class="icon">❌</span>
          <h2>错误详情</h2>
        </div>
        <div class="card-body">
          <pre class="error-text">{{ errorInfo }}</pre>
        </div>
      </div>

      <!-- 查询测试 -->
      <div class="card query-card">
        <div class="card-header">
          <span class="icon">🔍</span>
          <h2>数据查询测试</h2>
        </div>
        <div class="card-body">
          <div class="query-form">
            <select v-model="queryTable" class="select">
              <option value="users">users</option>
              <option value="products">products</option>
              <option value="categories">categories</option>
              <option value="orders">orders</option>
            </select>
            <input v-model.number="queryLimit" type="number" min="1" max="20" placeholder="限制条数" class="input-limit" />
            <button @click="fetchQuery" :disabled="loading" class="btn btn-primary">查询</button>
          </div>

          <div v-if="queryResult" class="query-result">
            <p class="result-info">找到 {{ queryResult.rowCount }} 条记录</p>
            <div class="result-table-wrapper">
              <table class="result-table" v-if="queryResult.data && queryResult.data.length > 0">
                <thead>
                  <tr>
                    <th v-for="key in queryResult.data[0] ? Object.keys(queryResult.data[0]) : []" :key="key">{{ key }}</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(row, idx) in queryResult.data" :key="idx">
                    <td v-for="key in Object.keys(row)" :key="key">
                      <span :title="String(row[key])">{{ formatCell(row[key]) }}</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <p v-else class="no-data">暂无数据</p>
            </div>
          </div>

          <div v-if="queryError" class="query-error">
            <pre>{{ queryError }}</pre>
          </div>
        </div>
      </div>

      <!-- 操作栏 -->
      <div class="actions">
        <button @click="fetchAll" :disabled="loading" class="btn btn-primary btn-large">
          {{ loading ? '检测中...' : '全部检测' }}
        </button>
        <a href="/" class="btn btn-secondary">返回首页</a>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import axios from 'axios'

const loading = ref(false)
const dbConnected = ref(false)
const dbStatusMessage = ref('')
const dbInfo = ref(null)
const tableCounts = ref(null)
const errorInfo = ref(null)

const queryTable = ref('users')
const queryLimit = ref(5)
const queryResult = ref(null)
const queryError = ref(null)

const statusClass = computed(() => ({
  'status-connected': dbConnected.value,
  'status-disconnected': !dbConnected.value
}))

function formatCell(value) {
  if (value === null || value === undefined) return '<NULL>'
  const str = String(value)
  return str.length > 30 ? str.substring(0, 27) + '...' : str
}

async function fetchDbStatus() {
  loading.value = true
  errorInfo.value = null

  try {
    const res = await axios.get('/api/test/db/status')
    const data = res.data

    if (data.code === 200 && data.data) {
      dbConnected.value = data.data.connected
      dbStatusMessage.value = data.data.message
      dbInfo.value = data.data
      tableCounts.value = data.data.tableCounts
      errorInfo.value = data.data.error || null
    } else {
      dbConnected.value = false
      dbStatusMessage.value = data.message || '检测失败'
      errorInfo.value = data.message || '未知错误'
    }
  } catch (err) {
    dbConnected.value = false
    dbStatusMessage.value = '请求失败'
    errorInfo.value = err.message || '网络连接错误'
  } finally {
    loading.value = false
  }
}

async function fetchQuery() {
  loading.value = true
  queryError.value = null
  queryResult.value = null

  try {
    const res = await axios.get('/api/test/db/query', {
      params: { table: queryTable.value, limit: queryLimit.value }
    })
    const data = res.data
    if (data.code === 200) {
      queryResult.value = data.data
    } else {
      queryError.value = data.message
    }
  } catch (err) {
    queryError.value = err.message || '查询失败'
  } finally {
    loading.value = false
  }
}

async function fetchAll() {
  await fetchDbStatus()
  if (dbConnected.value) {
    await fetchQuery()
  }
}

// Auto-fetch on mount
fetchAll()
</script>

<style scoped>
.db-test-page {
  min-height: 100vh;
  background: linear-gradient(180deg, var(--color-primary-500, #10b981) 0%, var(--color-primary-400, #34d399) 35%, #f5f5f5 35%);
  padding-bottom: 40px;
}

.page-header {
  text-align: center;
  padding: 24px 20px 16px;
  color: white;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 700;
  margin: 0 0 4px;
}

.subtitle {
  font-size: 13px;
  opacity: 0.8;
  margin: 0;
}

.test-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 0 16px;
}

.card {
  background: #fff;
  border-radius: 16px;
  margin-bottom: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
}

.card-header .icon {
  font-size: 22px;
}

.card-header h2 {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  color: #333;
}

.card-body {
  padding: 20px;
}

.status-card.status-connected {
  border-left: 4px solid #52c41a;
}

.status-card.status-disconnected {
  border-left: 4px solid #ff4d4f;
}

.status-text {
  font-size: 16px;
  color: #333;
  margin: 0 0 16px;
}

.info-table {
  width: 100%;
  border-collapse: collapse;
}

.info-table td {
  padding: 8px 0;
  font-size: 14px;
  border-bottom: 1px solid #f5f5f5;
}

.info-table .label {
  color: #999;
  width: 100px;
}

.info-table .mono {
  font-family: monospace;
  font-size: 12px;
  word-break: break-all;
  color: #666;
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.table-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 14px;
  background: #fafafa;
  border-radius: 10px;
}

.table-name {
  font-size: 14px;
  color: #666;
}

.table-count {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary-500, #10b981);
}

.table-count.zero {
  color: #ccc;
}

.error-card {
  border-left: 4px solid #ff4d4f;
}

.error-text {
  font-size: 12px;
  color: #ff4d4f;
  background: #fff1f0;
  padding: 12px;
  border-radius: 8px;
  word-break: break-all;
  margin: 0;
  white-space: pre-wrap;
}

.query-form {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.select,
.input-limit {
  padding: 10px 14px;
  border: 2px solid #e8e8e8;
  border-radius: 10px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}

.select:focus,
.input-limit:focus {
  border-color: var(--color-primary-500, #10b981);
}

.input-limit {
  width: 80px;
  text-align: center;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background: linear-gradient(135deg, var(--color-primary-500, #10b981), var(--color-primary-400, #34d399));
  color: white;
}

.btn-primary:active {
  transform: scale(0.97);
}

.btn-secondary {
  background: #f0f0f0;
  color: #666;
}

.btn-large {
  padding: 14px 32px;
  font-size: 16px;
  border-radius: 14px;
}

.query-result {
  margin-top: 12px;
}

.result-info {
  font-size: 13px;
  color: #999;
  margin: 0 0 12px;
}

.result-table-wrapper {
  overflow-x: auto;
  border-radius: 10px;
  border: 1px solid #f0f0f0;
}

.result-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.result-table th {
  background: #fafafa;
  padding: 8px 10px;
  text-align: left;
  color: #666;
  font-weight: 600;
  white-space: nowrap;
  border-bottom: 1px solid #e8e8e8;
}

.result-table td {
  padding: 8px 10px;
  border-bottom: 1px solid #f5f5f5;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-table td span {
  cursor: help;
}

.no-data {
  text-align: center;
  color: #ccc;
  padding: 24px;
  font-size: 14px;
}

.query-error {
  margin-top: 12px;
}

.query-error pre {
  font-size: 12px;
  color: #ff4d4f;
  background: #fff1f0;
  padding: 12px;
  border-radius: 8px;
  word-break: break-all;
  margin: 0;
  white-space: pre-wrap;
}

.actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 20px;
}
</style>
