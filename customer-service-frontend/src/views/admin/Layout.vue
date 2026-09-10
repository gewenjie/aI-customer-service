<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">🤖 智能客服系统</div>
      <el-menu
        :default-active="$route.path"
        router
        background-color="#001529"
        text-color="#a6adb4"
        active-text-color="#ffffff"
      >
        <el-menu-item index="/admin/dashboard">
          <el-icon><Odometer /></el-icon><span>工作台</span>
        </el-menu-item>
        <el-menu-item index="/admin/knowledge">
          <el-icon><Reading /></el-icon><span>知识库管理</span>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/admin/agents">
          <el-icon><UserFilled /></el-icon><span>客服管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/sessions">
          <el-icon><ChatDotRound /></el-icon><span>会话管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="page-title">{{ $route.meta.title }}</div>
        <div class="header-right">
          <el-avatar :size="30" class="avatar">{{ (user?.nickname || '?')[0] }}</el-avatar>
          <span class="uname">{{ user?.nickname || user?.username }}</span>
          <el-tag size="small" :type="isAdmin ? 'danger' : 'primary'">
            {{ isAdmin ? '管理员' : '客服' }}
          </el-tag>
          <el-button link type="primary" @click="handleLogout">退出登录</el-button>
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
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const user = computed(() => userStore.user)
const isAdmin = computed(() => userStore.user?.role === 'ADMIN')

async function handleLogout() {
  await ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}
.aside {
  background: #001529;
  color: #fff;
}
.brand {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.aside :deep(.el-menu) {
  border-right: none;
}
.aside :deep(.el-menu-item.is-active) {
  background: #4f7cff;
}
.header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.page-title {
  font-size: 16px;
  font-weight: 600;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  background: #4f7cff;
  color: #fff;
}
.uname {
  font-size: 14px;
}
.main {
  padding: 20px;
  background: #f5f7fa;
}
</style>
