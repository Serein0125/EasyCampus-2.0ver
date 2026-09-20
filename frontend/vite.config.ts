import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vitejs.dev/config/
// 注意：前端源码中大量使用了 `import.meta.env.VITE_*` 读取环境变量（见 src/services/api.ts），
//       如果需要在不同环境切换 API/WS/图片地址，不要改这里，应该通过 `.env.development`
//       或 `.env.production` 文件注入 VITE_ 开头的变量。
export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    tailwindcss(),
    // Element Plus 按需引入：模板中的 el-* 组件自动解析并只打包用到的，
    // 自动生成的 auto-imports.d.ts / components.d.ts 已加入 tsconfig include
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] })
  ],
  server: {
    // 监听所有网卡（0.0.0.0），让同一 WiFi 下的手机 / 其他设备可以通过
    // `http://本机局域网IP:3000` 访问前端页面；不加时默认只监听 127.0.0.1，
    // 外部设备无法建立 TCP 连接，`npm run dev` 会提示 "Network: use --host to expose"。
    host: true,
    // 固定端口 3000，与 Vite 代理转发到 Spring Boot 的 CORS 白名单保持一致
    port: 3000,
    // 开发环境代理：把前端发起的 /api 与 /uploads 请求转发到本地 Spring Boot (8080)
    // 生产环境不使用 Vite，所以只在 mode === 'development' 时注册代理，
    // 生产部署改为由 Nginx 把同样的路径反代到后端实例。
    proxy: mode === 'development' ? {
      '/api': {
        // 这里显式写 localhost:8080 而不要写 "http://<局域网IP>:8080"，
        // 因为「代理请求」是 Vite 自己在本机作为客户端发出去的，走电脑本地回环地址即可。
        // 写虚拟网卡/局域网 IP 反而可能因为 JWT 的 Host 校验、后端 CORS origin
        // 或 localhost 判定逻辑不一致而产生莫名其妙的 401/403。
        target: 'http://localhost:8080',
        // 代理请求的 Host 头改为 target 的主机名，避免 Spring Security 的
        // CSRF / Host 白名单 / 会话域校验因为 origin 不一致而被拒绝。
        changeOrigin: true
      },
      '/uploads': {
        // 上传文件统一由后端 FileUploadController 保存，并通过本地文件映射访问，
        // 手机端查看图片时，实际请求路径是 http://本机IP:3000/uploads/xxx，
        // 由 Vite 转发给 8080，对前端完全透明。
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    } : undefined
  },
  // 生产环境构建配置
  build: {
    outDir: 'dist',
    // 开发模式生成 sourcemap，便于在浏览器 DevTools 里调试 TS 源码；
    // 生产构建关闭，减小包体积并隐藏源码（生产环境 .map 可能泄露业务代码）。
    sourcemap: mode === 'development',
    rollupOptions: {
      output: {
        // 把框架层（vue / vue-router / axios）单独打成一个 vendor chunk，
        // 这些包版本基本不变，长期缓存可以显著加速二次访问（生产部署尤其重要）。
        // element-plus 体积较大且被多页共享，单独成包便于缓存与分包控制。
        manualChunks: {
          vendor: ['vue', 'vue-router', 'axios'],
          'element-plus': ['element-plus']
        }
      }
    }
  },
  // 部署基础路径：如果将来要挂到子路径（例如 https://domain.com/app/），
  // 把这里改成 '/app/' 即可，Vite 会自动帮你修正所有静态资源引用路径。
  base: '/'
}))
