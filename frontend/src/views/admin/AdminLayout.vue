<template>
  <el-container class="admin-layout">
    <el-aside width="210px" class="aside">
      <div class="logo">
        <span class="brand-mark">云</span>
        <span>云阅管理端</span>
      </div>
      <el-menu :default-active="$route.path" router background-color="#ffffff" text-color="#303133"
               active-text-color="#409eff">
        <el-menu-item index="/admin/dashboard"><el-icon><Odometer /></el-icon>仪表盘</el-menu-item>
        <el-menu-item index="/admin/monitor"><el-icon><Monitor /></el-icon>监控中心</el-menu-item>
        <el-menu-item index="/admin/books"><el-icon><Collection /></el-icon>内容审核</el-menu-item>
        <el-menu-item index="/admin/categories"><el-icon><FolderOpened /></el-icon>分类管理</el-menu-item>
        <el-menu-item index="/admin/comments"><el-icon><ChatDotRound /></el-icon>评论管理</el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/admin/config"><el-icon><Setting /></el-icon>系统配置</el-menu-item>
        <el-menu-item index="/admin/user-requests"><el-icon><Message /></el-icon>用户请求</el-menu-item>
        <el-menu-item index="/admin/user-request-logs"><el-icon><Finished /></el-icon>用户请求日志</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">前台首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta?.title || currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <span>{{ auth.user?.nickname || auth.user?.username }}</span>
          <el-button size="small" text @click="auth.logout(); $router.push('/login')">退出</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '../../store/auth'

const auth = useAuthStore()
const route = useRoute()
const titles = {
  '/admin/dashboard': '仪表盘',
  '/admin/monitor': '监控中心',
  '/admin/books': '内容审核',
  '/admin/categories': '分类管理',
  '/admin/comments': '评论管理',
  '/admin/users': '用户管理',
  '/admin/config': '系统配置',
  '/admin/user-requests': '用户请求',
  '/admin/user-request-logs': '用户请求日志'
}
const currentTitle = computed(() => titles[route.path] || '管理后台')
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}
.aside {
  background: #ffffff;
  overflow-y: auto;
  border-right: 1px solid #e4e7ed;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #303133;
  font-size: 17px;
  font-weight: 700;
  padding: 18px 16px;
}
.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  display: flex;
  align-items: center;
  justify-content: center;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.aside :deep(.el-menu-item:hover),
.aside :deep(.el-sub-menu__title:hover) {
  background-color: #ecf5ff !important;
  color: #409eff !important;
}
.aside :deep(.el-menu-item.is-active) {
  background-color: #ecf5ff;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  z-index: 5;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #606266;
}
.main {
  background: #f5f7fa;
}
</style>
