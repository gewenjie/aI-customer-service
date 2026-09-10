<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-logo">🤖</div>
      <div class="login-title">智能客服系统</div>
      <div class="login-sub">管理后台登录</div>
      <el-form class="login-form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" :prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            size="large"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <div class="login-tip">默认账号：admin / admin123（管理员） · agent01 / 123456（客服）</div>
      <div class="login-link">
        <router-link to="/chat">← 返回访客端体验</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

async function handleLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/admin/dashboard')
  } catch (e) {
    /* handled by interceptor */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #4f7cff 0%, #7b5cff 100%);
}

.login-card {
  width: 400px;
  max-width: 92vw;
  background: #fff;
  border-radius: 16px;
  padding: 40px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.25);
  text-align: center;
}
.login-logo {
  font-size: 44px;
}
.login-title {
  font-size: 22px;
  font-weight: 700;
  margin-top: 8px;
}
.login-sub {
  font-size: 14px;
  color: #909399;
  margin: 6px 0 24px;
}
.login-btn {
  width: 100%;
}
.login-tip {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 18px;
}
.login-link {
  margin-top: 14px;
  font-size: 13px;
  color: #4f7cff;
}
</style>
