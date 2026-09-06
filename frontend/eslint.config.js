import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import vueTsEslintConfig from '@vue/eslint-config-typescript'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

// ESLint Flat Config
// 规则策略：
// - 应用代码（src/）走 Vue + TS 推荐规则，格式化交给 Prettier，避免两者冲突
// - 存量技术债（prop 突变等）先降为 warn 标记，不阻断提交，后续逐步偿还
// - 构建脚本/配置文件为 Node 环境，不纳入前端代码门禁
export default [
  {
    ignores: [
      'dist/**',
      'node_modules/**',
      'public/**',
      'coverage/**',
      'scripts/**',
      'eslint.config.js',
      'vite.config.ts'
    ]
  },
  js.configs.recommended,
  ...pluginVue.configs['flat/essential'],
  ...vueTsEslintConfig(),
  skipFormatting,
  {
    rules: {
      // 允许单词组件名（如 Home.vue / Login.vue 是页面级约定）
      'vue/multi-word-component-names': 'off',
      // 项目存量代码较多 any，先关闭不阻断（后续可逐步补类型）
      '@typescript-eslint/no-explicit-any': 'off',
      // 未使用变量告警，下划线前缀的参数/变量视为有意保留
      '@typescript-eslint/no-unused-vars': [
        'warn',
        { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }
      ],
      // 项目大量使用「有意静默」的空 catch（如统计上报失败不影响主流程），允许空 catch 块
      'no-empty': ['error', { allowEmptyCatch: true }],
      // —— 以下为存量技术债，降为 warn/off，避免一次性大范围改动引入回归 ——
      // 存量组件存在直接修改 prop 的写法，正确做法是 emit 事件由父组件修改，后续逐步重构
      'vue/no-mutating-props': 'warn',
      // <script> 不带 lang 也是合法的 JS 写法
      'vue/block-lang': 'off',
      // ImportMetaEnv 等类型声明中的 {} 是空接口的常见写法
      '@typescript-eslint/no-empty-object-type': 'off',
      // 新规则：要求 re-throw 错误时带 cause，存量代码暂不强制
      'preserve-caught-error': 'off',
      // 存量代码存在少量无效赋值，标记告警即可
      'no-useless-assignment': 'warn'
    }
  }
]
