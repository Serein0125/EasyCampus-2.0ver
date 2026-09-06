#!/usr/bin/env node

/**
 * Vercel构建验证脚本
 * 确保项目使用Vite而不是Vue CLI进行构建
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('🔍 验证Vercel构建配置...\n');

// 检查package.json
const packageJsonPath = path.join(__dirname, '../package.json');
const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));

console.log('📦 检查package.json配置:');
console.log(`   项目名称: ${packageJson.name}`);
console.log(`   Node版本: ${process.version}`);

// 检查构建脚本
console.log('\n🚀 检查构建脚本:');
const scripts = packageJson.scripts || {};
console.log(`   dev命令: ${scripts.dev || '未设置'}`);
console.log(`   build命令: ${scripts.build || '未设置'}`);
console.log(`   preview命令: ${scripts.preview || '未设置'}`);

// 验证构建命令
if (scripts.build && scripts.build.includes('vite')) {
  console.log('   ✅ build命令使用Vite');
} else if (scripts.build && scripts.build.includes('vue-cli-service')) {
  console.log('   ❌ build命令使用Vue CLI (需要修复)');
} else {
  console.log('   ⚠️  build命令未明确指定构建工具');
}

// 检查依赖项
console.log('\n📚 检查依赖项:');
const dependencies = packageJson.dependencies || {};
const devDependencies = packageJson.devDependencies || {};

const hasVite = devDependencies.vite || dependencies.vite;
const hasVueCli = devDependencies['@vue/cli-service'] || dependencies['@vue/cli-service'];

if (hasVite) {
  console.log(`   ✅ 已安装Vite: ${hasVite}`);
} else {
  console.log('   ❌ 未安装Vite');
}

if (hasVueCli) {
  console.log(`   ❌ 已安装Vue CLI: ${hasVueCli} (需要移除)`);
} else {
  console.log('   ✅ 未安装Vue CLI');
}

// 检查Vite配置文件
console.log('\n⚙️ 检查配置文件:');
const viteConfigPath = path.join(__dirname, '../vite.config.ts');
const vueConfigPath = path.join(__dirname, '../vue.config.js');

if (fs.existsSync(viteConfigPath)) {
  console.log('   ✅ 找到vite.config.ts');
} else {
  console.log('   ❌ 未找到vite.config.ts');
}

if (fs.existsSync(vueConfigPath)) {
  console.log('   ❌ 找到vue.config.js (Vue CLI配置文件，需要移除)');
} else {
  console.log('   ✅ 未找到vue.config.js');
}

// 检查Vercel配置
console.log('\n🌐 检查Vercel配置:');
const vercelJsonPath = path.join(__dirname, '../vercel.json');
const vercelTomlPath = path.join(__dirname, '../vercel.toml');

if (fs.existsSync(vercelJsonPath)) {
  const vercelJson = JSON.parse(fs.readFileSync(vercelJsonPath, 'utf8'));
  console.log(`   vercel.json框架: ${vercelJson.framework || '未设置'}`);
  if (vercelJson.framework === 'vite') {
    console.log('   ✅ vercel.json正确配置为Vite');
  } else {
    console.log(`   ⚠️  vercel.json框架配置为: ${vercelJson.framework}`);
  }
}

if (fs.existsSync(vercelTomlPath)) {
  const vercelToml = fs.readFileSync(vercelTomlPath, 'utf8');
  if (vercelToml.includes('[framework]') && vercelToml.includes('name = "vite"')) {
    console.log('   ✅ vercel.toml正确配置为Vite');
  } else {
    console.log('   ⚠️  vercel.toml未明确配置框架');
  }
}

// 总结
console.log('\n📊 验证总结:');
const issues = [];

if (!scripts.build || !scripts.build.includes('vite')) {
  issues.push('构建命令未使用Vite');
}

if (hasVueCli) {
  issues.push('项目中包含Vue CLI依赖');
}

if (fs.existsSync(vueConfigPath)) {
  issues.push('存在Vue CLI配置文件');
}

if (issues.length === 0) {
  console.log('✅ 所有检查通过！项目正确配置为Vite项目。');
  console.log('\n💡 建议:');
  console.log('   1. 在Vercel控制台清除构建缓存');
  console.log('   2. 重新部署项目');
  console.log('   3. 验证构建日志显示"vite build"');
} else {
  console.log(`⚠️  发现${issues.length}个问题:`);
  issues.forEach((issue, index) => {
    console.log(`   ${index + 1}. ${issue}`);
  });
  console.log('\n🔧 需要修复的问题:');
  console.log('   1. 确保package.json中的build命令使用"vite build"');
  console.log('   2. 移除Vue CLI相关依赖 (@vue/cli-service等)');
  console.log('   3. 删除vue.config.js文件（如果存在）');
  console.log('   4. 确保vercel.json和vercel.toml正确配置框架为"vite"');
}

console.log('\n🎯 验证完成！');