import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
// Element Plus 基础样式必须最先加载：它自带 :root 的默认 token（如 --el-color-primary:#409eff），
// 后续 tailwind.css 的同特异性 :root 覆盖才能生效，否则品牌绿主题会被 EP 默认蓝盖掉
import 'element-plus/dist/index.css'
import App from './App.vue'
// Tailwind v4 入口：必须在 design-system.css（由 App.vue 引入）之后加载，
// 保证原子类在未分层规则之后注入（详见 tailwind.css 顶部注释）
import './assets/css/tailwind.css'
import router from './router'

const app = createApp(App)

// Pinia 状态管理 + 持久化插件（auth store 的 token/user 自动同步 localStorage）
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.mount('#app')
