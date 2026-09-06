import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
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
