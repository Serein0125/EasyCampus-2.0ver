import { createRouter, createWebHistory } from 'vue-router'

// 通过「模块扩展（declaration merging）」给 vue-router 的 RouteMeta 增加自定义字段。
// 这样在每个路由的 meta 里写 title/requiresAuth/... 时会有 TS 类型提示与校验。
declare module 'vue-router' {
  interface RouteMeta {
    title?: string       // 页面标题，守卫里拼成 document.title
    requiresAuth?: boolean // 是否需要登录
    showTabBar?: boolean  // 是否显示顶部 AppHeader（主 tab 页为 true）
    transition?: string   // 页面切换动画名（'slide' 为详情页左右滑入）
  }
}

// 所有路由的 component 都写成 () => import(...) 动态导入，
// 这样 Vite 会把它们拆成独立 chunk 按需加载（路由懒加载），显著减小首屏体积。
const routes = [
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { title: '首页', showTabBar: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: () => import('../views/ForgotPassword.vue'),
    meta: { title: '找回密码' }
  },
  {
    path: '/db-test',
    name: 'DatabaseTest',
    component: () => import('../views/DatabaseTest.vue'),
    meta: { title: '数据库测试' }
  },
  {
    path: '/products',
    name: 'Products',
    component: () => import('../views/Products.vue'),
    meta: { title: '商品列表', showTabBar: true }
  },
  {
    path: '/products/:id',
    name: 'ProductDetail',
    component: () => import('../views/ProductDetail.vue'),
    meta: { title: '商品详情', transition: 'slide', showTabBar: false }
  },
  {
    path: '/products/create',
    name: 'CreateProduct',
    component: () => import('../views/CreateProduct.vue'),
    meta: {
      requiresAuth: true,
      title: '发布商品'
    }
  },
  {
    path: '/categories',
    name: 'Categories',
    component: () => import('../views/Categories.vue'),
    meta: { title: '分类浏览', showTabBar: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: () => import('../views/Messages.vue'),
    meta: {
      requiresAuth: true,
      title: '消息',
      showTabBar: true
    }
  },
  // 聊天室页面（核心新功能）
  {
    path: '/chat/:userId',
    name: 'ChatRoom',
    component: () => import('../views/ChatRoom.vue'),
    meta: {
      requiresAuth: true,
      title: '聊天',
      transition: 'slide'
    }
  },
  // ==================== 社区功能路由 (Phase 1) ====================
  {
    path: '/search',
    name: 'Search',
    component: () => import('../views/SearchPage.vue'),
    meta: { title: '搜索' }
  },
  {
    path: '/community',
    name: 'Community',
    component: () => import('../views/CommunityPage.vue'),
    meta: { title: '社区', showTabBar: true }
  },
  {
    path: '/community/posts/create',
    name: 'PostCreate',
    component: () => import('../views/PostCreatePage.vue'),
    meta: { requiresAuth: true, title: '发布帖子' }
  },
  {
    path: '/community/posts/:id',
    name: 'PostDetail',
    component: () => import('../views/PostDetailPage.vue'),
    meta: { title: '帖子详情', transition: 'slide', showTabBar: false }
  },
  {
    path: '/ads/create',
    name: 'AdCreate',
    component: () => import('../views/AdEditPage.vue'),
    meta: { requiresAuth: true, title: '广告编辑' }
  },
  {
    path: '/boards',
    name: 'BoardsDiscover',
    component: () => import('../views/BoardsDiscoverPage.vue'),
    meta: { title: '兴趣圈子', showTabBar: true }
  },
  {
    path: '/boards/:id',
    name: 'BoardDetail',
    component: () => import('../views/BoardPage.vue'),
    meta: { title: '圈子详情' }
  },
  {
    path: '/users/:id',
    name: 'UserProfile',
    component: () => import('../views/UserProfilePage.vue'),
    meta: { title: '用户主页', transition: 'slide' }
  },
  {
    path: '/follows',
    name: 'FollowList',
    component: () => import('../views/FollowList.vue'),
    meta: { title: '关注列表', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('../views/Profile.vue'),
    meta: {
      title: '我的',
      showTabBar: true
    }
  },
  {
    path: '/my-products',
    name: 'MyProducts',
    component: () => import('../views/MyProducts.vue'),
    meta: {
      requiresAuth: true,
      title: '商品与帖子'
    }
  },
  {
    path: '/favorites',
    name: 'Favorites',
    component: () => import('../views/Favorites.vue'),
    meta: {
      requiresAuth: true,
      title: '我的收藏'
    }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: () => import('../views/NotificationsPage.vue'),
    meta: {
      requiresAuth: true,
      title: '通知'
    }
  },
  {
    path: '/my-activities',
    name: 'MyActivities',
    component: () => import('../views/MyActivities.vue'),
    meta: {
      requiresAuth: true,
      title: '我的活动'
    }
  },
  {
    path: '/address',
    name: 'Address',
    component: () => import('../views/Address.vue'),
    meta: {
      requiresAuth: true,
      title: '收货地址'
    }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('../views/Settings.vue'),
    meta: {
      requiresAuth: true,
      title: '设置'
    }
  },
  // 活动页面
  {
    path: '/activities',
    name: 'Activities',
    component: () => import('../views/Activities.vue'),
    meta: { title: '活动', showTabBar: true }
  },
  {
    path: '/activities/:id',
    name: 'ActivityDetail',
    component: () => import('../views/ActivityDetail.vue'),
    meta: { title: '活动详情', transition: 'slide' }
  },
  // 组织页面
  {
    path: '/orgs/create',
    name: 'CreateOrg',
    component: () => import('../views/CreateOrgPage.vue'),
    meta: { requiresAuth: true, title: '创建组织' }
  },
  {
    path: '/orgs/my',
    name: 'MyOrgs',
    component: () => import('../views/MyOrgsPage.vue'),
    meta: { requiresAuth: true, title: '我的组织' }
  },
  {
    path: '/orgs/discover',
    name: 'OrgDiscover',
    component: () => import('../views/OrgDiscoverPage.vue'),
    meta: { title: '发现组织' }
  },
  {
    path: '/orgs/invitations',
    name: 'Invitations',
    component: () => import('../views/InvitationsPage.vue'),
    meta: { requiresAuth: true, title: '我的邀请' }
  },
  {
    path: '/orgs/:id',
    name: 'OrgDetail',
    component: () => import('../views/OrgDetailPage.vue'),
    meta: { title: '组织详情' }
  },
  // 404页面 - 必须放在最后
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('../views/NotFound.vue')
  }
]

const router = createRouter({
  // createWebHistory 用 HTML5 history API，URL 没有 # 号，更干净；
  // 代价是部署时服务器必须把所有路由都回退到 index.html（见 nginx/vercel 配置）
  history: createWebHistory(),
  routes,
  // scrollBehavior 控制路由切换时的滚动位置：
  // - 浏览器前进/后退（savedPosition 有值）时恢复到上次离开的位置
  // - 普通跳转则回到顶部
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    } else {
      return { top: 0 }
    }
  }
})

// 白名单路径（无需登录即可访问）。注意：带 :id 的动态路径用 :param 写法，
// 下面的匹配逻辑会把它转成正则来匹配实际请求路径
const publicPaths = ['/', '/login', '/register', '/forgot-password', '/db-test', '/products', '/products/:id', '/categories',
  '/community', '/community/posts/:id', '/boards', '/boards/:id', '/users/:id', '/activities', '/activities/:id', '/profile', '/search',
  '/orgs/discover', '/orgs/:id']

// 全局前置守卫：每次路由跳转前都会执行，是做鉴权与页面标题统一设置的入口
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title
    ? `${to.meta.title} - 易校EasyCampus`
    : '易校EasyCampus'

  // 检查当前路径是否在白名单中
  // 动态路径（含 :param）无法直接全等比较，故用正则：把 :id 替换为 [^/]+ 后整段匹配
  const isPublicPath = publicPaths.some(path => {
    if (path.includes(':')) {
      const regex = new RegExp('^' + path.replace(/:\w+/g, '[^/]+') + '$')
      return regex.test(to.path)
    }
    return to.path === path
  })

  // 公开路径直接放行
  if (isPublicPath) {
    const token = localStorage.getItem('token')

    // 已登录用户访问登录/注册页，重定向到首页（避免重复登录）
    if ((to.path === '/login' || to.path === '/register') && token && isValidToken(token)) {
      return next({ path: '/' })
    }
    return next()
  }

  // 需要认证的页面（不在白名单的路径默认需要认证）
  const token = localStorage.getItem('token')

  if (!token || !isValidToken(token)) {
    // 无效token或未登录
    if (token && !isValidToken(token)) {
      // 存了但无效的 token 顺手清理，避免后续每次都走「无效」分支
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }

    // 未登录跳登录页，并把目标地址带上 redirect 参数，登录成功后能跳回原页面
    return next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  }

  next()
})

// 验证token有效性的辅助函数
// 注意这里只做「前端格式校验」，真正的有效性由后端校验 JWT；
// 前端这层主要用于：① 拦截演示模式假 token ② 避免明显无效 token 触发请求
function isValidToken(token: string | null): boolean {
  if (!token) return false

  // 拒绝演示模式的假token
  if (token.startsWith('demo_token_') || token.includes('demo')) {
    return false
  }

  // JWT token基本格式验证
  return token.length > 10
}

export default router