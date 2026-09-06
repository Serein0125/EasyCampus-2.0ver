#!/usr/bin/env node

/**
 * 构建环境检查脚本
 * 验证Vercel构建环境是否满足项目要求
 */

import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

console.log('🔍 检查构建环境...\n');

// 检查Node版本
try {
  const nodeVersion = process.version;
  console.log(`📦 Node版本: ${nodeVersion}`);
  
  // 检查是否满足engines要求
  const packageJson = JSON.parse(fs.readFileSync(path.join(__dirname, '../package.json'), 'utf8'));
  const engines = packageJson.engines || {};
  
  if (engines.node) {
    const requiredNode = engines.node;
    console.log(`   package.json要求: ${requiredNode}`);
    
    // 简单的版本检查（实际应该使用semver库）
    const currentMajor = parseInt(nodeVersion.replace('v', '').split('.')[0]);
    const requiredMajor = parseInt(requiredNode.replace('>=', '').split('.')[0]);
    
    if (currentMajor >= requiredMajor) {
      console.log('   ✅ Node版本满足要求');
    } else {
      console.log(`   ❌ Node版本不满足要求，需要 ${requiredNode}`);
    }
  }
} catch (error) {
  console.log(`   ⚠️  无法检查Node版本: ${error.message}`);
}

// 检查npm版本
try {
  const npmVersion = execSync('npm --version', { encoding: 'utf8' }).trim();
  console.log(`📦 NPM版本: ${npmVersion}`);
} catch (error) {
  console.log(`   ⚠️  无法检查NPM版本: ${error.message}`);
}

// 检查Vite是否可用
console.log('\n🚀 检查Vite可用性:');
try {
  const viteVersion = execSync('npx vite --version', { encoding: 'utf8' }).trim();
  console.log(`   ✅ Vite版本: ${viteVersion}`);
} catch (error) {
  console.log(`   ❌ Vite命令不可用: ${error.message}`);
  console.log('   尝试检查本地安装...');
  
  // 检查本地node_modules中是否有vite
  const vitePath = path.join(__dirname, '../node_modules/.bin/vite');
  if (fs.existsSync(vitePath)) {
    console.log('   ✅ 找到本地安装的Vite');
  } else {
    console.log('   ❌ 未找到本地安装的Vite');
  }
}

// 检查依赖安装状态
console.log('\n📚 检查依赖安装状态:');
const nodeModulesPath = path.join(__dirname, '../node_modules');
if (fs.existsSync(nodeModulesPath)) {
  console.log('   ✅ node_modules目录存在');
  
  // 检查关键依赖
  const requiredDeps = ['vite', 'vue', '@vitejs/plugin-vue'];
  requiredDeps.forEach(dep => {
    const depPath = path.join(nodeModulesPath, dep);
    if (fs.existsSync(depPath)) {
      console.log(`   ✅ ${dep} 已安装`);
    } else {
      console.log(`   ❌ ${dep} 未安装`);
    }
  });
} else {
  console.log('   ❌ node_modules目录不存在，需要运行 npm install');
}

// 检查package-lock.json
console.log('\n🔒 检查依赖锁定文件:');
const lockfilePath = path.join(__dirname, '../package-lock.json');
if (fs.existsSync(lockfilePath)) {
  console.log('   ✅ package-lock.json存在');
  
  try {
    const lockfile = JSON.parse(fs.readFileSync(lockfilePath, 'utf8'));
    const lockfileVersion = lockfile.lockfileVersion || '未知';
    console.log(`   lockfile版本: ${lockfileVersion}`);
    
    if (lockfileVersion === 2 || lockfileVersion === 3) {
      console.log('   ✅ lockfile版本兼容npm ci');
    } else {
      console.log(`   ⚠️  lockfile版本${lockfileVersion}可能不兼容npm ci`);
    }
  } catch (error) {
    console.log(`   ⚠️  无法解析package-lock.json: ${error.message}`);
  }
} else {
  console.log('   ❌ package-lock.json不存在，无法使用npm ci');
  console.log('   建议运行: npm install --package-lock-only');
}

// 检查Vercel配置
console.log('\n🌐 检查Vercel配置:');
const vercelJsonPath = path.join(__dirname, '../vercel.json');
if (fs.existsSync(vercelJsonPath)) {
  try {
    const vercelJson = JSON.parse(fs.readFileSync(vercelJsonPath, 'utf8'));
    console.log(`   installCommand: ${vercelJson.installCommand || '未设置'}`);
    console.log(`   buildCommand: ${vercelJson.buildCommand || '未设置'}`);
    console.log(`   framework: ${vercelJson.framework || '未设置'}`);
    
    if (vercelJson.installCommand && vercelJson.installCommand.includes('npm ci')) {
      console.log('   ✅ 使用npm ci安装依赖（推荐）');
    } else if (vercelJson.installCommand && vercelJson.installCommand.includes('npm install')) {
      console.log('   ⚠️  使用npm install安装依赖，建议改为npm ci');
    }
  } catch (error) {
    console.log(`   ⚠️  无法解析vercel.json: ${error.message}`);
  }
}

// 总结
console.log('\n📊 环境检查总结:');

const issues = [];

// 检查问题
if (!fs.existsSync(nodeModulesPath)) {
  issues.push('node_modules目录不存在，需要安装依赖');
}

if (!fs.existsSync(lockfilePath)) {
  issues.push('package-lock.json不存在，无法使用npm ci');
}

if (issues.length === 0) {
  console.log('✅ 环境检查通过！可以开始构建。');
  console.log('\n💡 建议:');
  console.log('   1. 确保Vercel项目设置与vercel.json一致');
  console.log('   2. 清除Vercel构建缓存后重新部署');
  console.log('   3. 监控构建日志确认使用npm ci和vite build');
} else {
  console.log(`⚠️  发现${issues.length}个问题:`);
  issues.forEach((issue, index) => {
    console.log(`   ${index + 1}. ${issue}`);
  });
  
  console.log('\n🔧 修复建议:');
  console.log('   1. 运行: npm ci --prefer-offline');
  console.log('   2. 如果npm ci失败，运行: npm install --package-lock-only');
  console.log('   3. 验证: node scripts/verify-build.js');
}

console.log('\n🎯 环境检查完成！');