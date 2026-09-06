#!/usr/bin/env node

/**
 * Vercel配置验证脚本
 * 验证项目根目录配置是否正确，确保Vercel能正确部署
 */

import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.join(__dirname, '..');

console.log('🔍 验证Vercel配置...\n');
console.log(`项目根目录: ${projectRoot}`);

// 检查根目录package.json
console.log('📦 检查根目录package.json:');
const rootPackageJsonPath = path.join(projectRoot, 'package.json');
if (fs.existsSync(rootPackageJsonPath)) {
  try {
    const rootPackageJson = JSON.parse(fs.readFileSync(rootPackageJsonPath, 'utf8'));
    console.log(`   ✅ 存在: ${rootPackageJson.name}@${rootPackageJson.version}`);
    
    // 检查deploy:frontend脚本
    const scripts = rootPackageJson.scripts || {};
    if (scripts['deploy:frontend']) {
      console.log(`   ✅ deploy:frontend脚本: ${scripts['deploy:frontend']}`);
    } else {
      console.log('   ❌ 缺少deploy:frontend脚本');
    }
  } catch (error) {
    console.log(`   ❌ 无法解析package.json: ${error.message}`);
  }
} else {
  console.log('   ❌ 根目录package.json不存在');
}

// 检查前端项目package.json
console.log('\n📦 检查前端项目package.json:');
const frontendPackageJsonPath = path.join(projectRoot, 'env-setup/frontend/package.json');
if (fs.existsSync(frontendPackageJsonPath)) {
  try {
    const frontendPackageJson = JSON.parse(fs.readFileSync(frontendPackageJsonPath, 'utf8'));
    console.log(`   ✅ 存在: ${frontendPackageJson.name}`);
    
    // 检查构建脚本
    const scripts = frontendPackageJson.scripts || {};
    if (scripts.build && scripts.build.includes('vite')) {
      console.log(`   ✅ build脚本使用Vite: ${scripts.build}`);
    } else {
      console.log(`   ❌ build脚本未使用Vite: ${scripts.build || '未设置'}`);
    }
  } catch (error) {
    console.log(`   ❌ 无法解析前端package.json: ${error.message}`);
  }
} else {
  console.log('   ❌ 前端package.json不存在');
}

// 检查Vercel配置文件
console.log('\n🌐 检查Vercel配置文件:');

// 检查根目录vercel.json
const rootVercelJsonPath = path.join(projectRoot, 'vercel.json');
if (fs.existsSync(rootVercelJsonPath)) {
  try {
    const vercelJson = JSON.parse(fs.readFileSync(rootVercelJsonPath, 'utf8'));
    console.log('   ✅ 根目录vercel.json存在');
    
    // 检查关键配置
    const checks = [
      { key: 'buildCommand', expected: 'npm run deploy:frontend' },
      { key: 'outputDirectory', expected: 'env-setup/frontend/dist' },
      { key: 'framework', expected: 'vite' }
    ];
    
    checks.forEach(check => {
      const value = vercelJson[check.key];
      if (value === check.expected) {
        console.log(`   ✅ ${check.key}: ${value}`);
      } else {
        console.log(`   ❌ ${check.key}: ${value || '未设置'} (期望: ${check.expected})`);
      }
    });
  } catch (error) {
    console.log(`   ❌ 无法解析vercel.json: ${error.message}`);
  }
} else {
  console.log('   ❌ 根目录vercel.json不存在');
}

// 检查根目录vercel.toml
const rootVercelTomlPath = path.join(projectRoot, 'vercel.toml');
if (fs.existsSync(rootVercelTomlPath)) {
  const vercelToml = fs.readFileSync(rootVercelTomlPath, 'utf8');
  console.log('   ✅ 根目录vercel.toml存在');
  
  // 检查关键配置
  if (vercelToml.includes('command = "npm run deploy:frontend"')) {
    console.log('   ✅ build命令正确配置');
  } else {
    console.log('   ❌ build命令配置不正确');
  }
  
  if (vercelToml.includes('output = "env-setup/frontend/dist"')) {
    console.log('   ✅ 输出目录正确配置');
  } else {
    console.log('   ❌ 输出目录配置不正确');
  }
  
  if (vercelToml.includes('[framework]') && vercelToml.includes('name = "vite"')) {
    console.log('   ✅ 框架正确配置为Vite');
  } else {
    console.log('   ❌ 框架未正确配置为Vite');
  }
} else {
  console.log('   ❌ 根目录vercel.toml不存在');
}

// 检查前端目录的Vercel配置（应该被忽略）
console.log('\n⚠️  检查前端目录Vercel配置（应该被忽略）:');
const frontendVercelJsonPath = path.join(projectRoot, 'env-setup/frontend/vercel.json');
if (fs.existsSync(frontendVercelJsonPath)) {
  console.log('   ⚠️  前端目录存在vercel.json，Vercel可能会优先使用根目录配置');
} else {
  console.log('   ✅ 前端目录没有vercel.json');
}

// 检查构建输出目录
console.log('\n📁 检查构建输出目录:');
const distPath = path.join(projectRoot, 'env-setup/frontend/dist');
if (fs.existsSync(distPath)) {
  console.log('   ✅ dist目录存在');
  
  // 检查dist目录内容
  const files = fs.readdirSync(distPath);
  const hasIndexHtml = files.includes('index.html');
  const hasAssetsDir = fs.existsSync(path.join(distPath, 'assets'));
  
  if (hasIndexHtml) {
    console.log('   ✅ 包含index.html');
  } else {
    console.log('   ❌ 缺少index.html');
  }
  
  if (hasAssetsDir) {
    console.log('   ✅ 包含assets目录');
  } else {
    console.log('   ❌ 缺少assets目录');
  }
} else {
  console.log('   ⚠️  dist目录不存在，需要先构建');
}

// 总结
console.log('\n📊 配置验证总结:');

const issues = [];

// 检查问题
if (!fs.existsSync(rootPackageJsonPath)) {
  issues.push('根目录缺少package.json文件');
}

if (!fs.existsSync(rootVercelJsonPath) && !fs.existsSync(rootVercelTomlPath)) {
  issues.push('根目录缺少Vercel配置文件');
}

if (!fs.existsSync(frontendPackageJsonPath)) {
  issues.push('前端项目缺少package.json文件');
}

if (issues.length === 0) {
  console.log('✅ 所有配置检查通过！Vercel应该能正确部署。');
  console.log('\n💡 部署步骤:');
  console.log('   1. 推送代码到GitHub: git add . && git commit -m "fix: add root package.json for Vercel" && git push');
  console.log('   2. 在Vercel控制台清除构建缓存');
  console.log('   3. 重新部署项目');
  console.log('   4. 监控构建日志，确认执行npm run deploy:frontend');
} else {
  console.log(`⚠️  发现${issues.length}个问题:`);
  issues.forEach((issue, index) => {
    console.log(`   ${index + 1}. ${issue}`);
  });
  
  console.log('\n🔧 修复建议:');
  console.log('   1. 确保根目录有package.json文件');
  console.log('   2. 确保根目录有vercel.json或vercel.toml文件');
  console.log('   3. 确保前端项目目录结构正确');
}

console.log('\n🎯 验证完成！');