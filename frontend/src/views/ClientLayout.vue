<template>
  <el-container class="client-layout">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo" @click="$router.push('/')">
        <span class="brand-mark">云</span>
        <span v-show="!collapsed" class="logo-text">云阅 CloudRead</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :collapse="collapsed"
          :collapse-transition="false"
          router
          :default-active="activeMenu"
          background-color="#ffffff"
          text-color="#303133"
          active-text-color="#409eff"
        >
          <el-menu-item index="/categories">
            <el-icon><Collection /></el-icon>
            <template #title>分类</template>
          </el-menu-item>

          <el-menu-item index="/discussion">
            <el-icon><ChatDotRound /></el-icon>
            <template #title>讨论</template>
          </el-menu-item>
          <el-menu-item index="/upload">
            <el-icon><Upload /></el-icon>
            <template #title>上传图书</template>
          </el-menu-item>
          <el-menu-item index="/profile">
            <el-icon><User /></el-icon>
            <template #title>个人主页</template>
          </el-menu-item>
          <el-menu-item index="/following">
            <el-icon><UserFilled /></el-icon>
            <template #title>我的关注</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
      <div class="collapse-btn" @click="collapsed = !collapsed">
        <el-icon>
          <Expand v-if="collapsed" />
          <Fold v-else />
        </el-icon>
        <span v-show="!collapsed">收起侧边栏</span>
      </div>
    </el-aside>

    <el-container class="right">
      <el-header class="header">
        <div class="search">
          <div class="search-box">
            <el-input v-model="keyword" placeholder="搜索书籍 / 作者 / ISBN" clearable class="search-input"
                      @keyup.enter="search" @clear="search" />
            <el-button class="search-btn" @click="search">
              <el-icon><Search /></el-icon>
            </el-button>
          </div>
        </div>
        <div class="actions">
          <template v-if="auth.isLoggedIn">
            <el-dropdown @command="onCommand">
              <span class="user-name">
                <el-avatar :size="30">{{ (auth.user?.nickname || auth.user?.username || 'U')[0] }}</el-avatar>
                {{ auth.user?.nickname || auth.user?.username }}
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人主页</el-dropdown-item>
                  <el-dropdown-item v-if="auth.isAdmin" command="admin">管理后台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <el-button v-else type="success" @click="$router.push('/login')">登录 / 注册</el-button>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)
const keyword = ref(route.query.keyword || '')

const activeMenu = computed(() => {
  if (route.path.startsWith('/categories')) return '/categories'
  if (route.path.startsWith('/discussion') || route.path.startsWith('/post/')) return '/discussion'
  if (route.path.startsWith('/upload')) return '/upload'
  if (route.path.startsWith('/profile')) return '/profile'
  if (route.path.startsWith('/following')) return '/following'
  return '/'
})

function search() {
  router.push({ path: '/', query: keyword.value ? { keyword: keyword.value } : {} })
}

async function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'admin') router.push('/admin')
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    auth.logout()
    router.push('/login')
  }
}

</script>

<style scoped>
.client-layout {
  height: 100vh;
}
.aside {
  background: #ffffff;
  display: flex;
  flex-direction: column;
  transition: width 0.2s;
  overflow: hidden;
  border-right: 1px solid #e4e7ed;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 14px;
  color: #303133;
  font-size: 17px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}
.brand-mark {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #409eff, #67c23a);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.menu-scroll {
  flex: 1;
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
.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #606266;
  padding: 14px 0;
  cursor: pointer;
  border-top: 1px solid #e4e7ed;
  font-size: 13px;
  white-space: nowrap;
}
.collapse-btn:hover {
  color: #409eff;
}
.right {
  min-width: 0;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 6px rgba(0, 0, 0, 0.08);
  z-index: 10;
  gap: 24px;
}
.search {
  flex: 1;
  max-width: 560px;
}
.search-box {
  display: flex;
  align-items: center;
  height: 40px;
  background: #fff;
  border: 2px solid #dcdfe6;
  border-radius: 20px;
  transition: border-color 0.2s, box-shadow 0.2s;
  overflow: hidden;
}
.search-box:focus-within {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}
.search-input {
  flex: 1;
  min-width: 0;
  height: 100%;
}
.search-input :deep(.el-input__wrapper) {
  box-shadow: none !important;
  background: transparent;
  border-radius: 20px 0 0 20px;
  padding: 0 14px;
}
.search-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  margin-right: 4px;
  padding: 0;
  border-radius: 16px;
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}
.search :deep(.search-btn:hover),
.search :deep(.search-btn:focus) {
  background: #66b1ff;
  border-color: #66b1ff;
  color: #fff;
}
.search-btn :deep(.el-icon) {
  color: #fff;
  font-size: 16px;
}
.actions {
  display: flex;
  align-items: center;
}
.user-name {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #303133;
}
.main {
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
