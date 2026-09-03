<template>
  <div class="login-page">
    <div class="login-card">
      <div class="logo">云阅 CloudRead</div>
      <div class="subtitle">电子书共享与管理平台 · 登录即注册</div>
      <template v-if="mode === 'login'">
        <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter="submit">
          <el-form-item prop="username">
            <el-input v-model="form.username" placeholder="用户名（唯一标识）" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input v-model="form.password" type="password" placeholder="密码（至少 6 位）" size="large"
                      show-password :prefix-icon="Lock" />
          </el-form-item>
          <div class="forgot-row">
            <span class="forgot-link" @click="mode = 'reset'">忘记密码？</span>
          </div>
          <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">
            登录 / 自动注册
          </el-button>
        </el-form>
        <div class="hint">演示账号：admin / admin123（管理员），demo / demo123（用户）</div>
      </template>

      <template v-else>
        <div class="reset-head">
          <span>忘记密码</span>
          <el-button text size="small" @click="mode = 'login'">返回登录</el-button>
        </div>
        <el-form :model="resetForm" :rules="resetRules" ref="resetFormRef">
          <el-form-item prop="email">
            <el-input v-model="resetForm.email" placeholder="电子邮件（必填）" size="large" :prefix-icon="Message" />
          </el-form-item>
          <el-form-item prop="username">
            <el-input v-model="resetForm.username" placeholder="用户名（必填）" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="detail">
            <el-input v-model="resetForm.detail" type="textarea" :rows="3" maxlength="500" show-word-limit
                      size="large" placeholder="详情（选填，最多 500 字）" />
          </el-form-item>
          <el-button type="primary" size="large" class="submit" :loading="resetLoading" @click="submitReset">
            提交给管理员
          </el-button>
        </el-form>
        <div class="hint">提交后请等待管理员处理，管理员重置完成后可通过新密码登录</div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'
import { submitPasswordReset } from '../api/userRequests'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const formRef = ref()
const resetFormRef = ref()
const loading = ref(false)
const resetLoading = ref(false)
const mode = ref('login')
const form = reactive({ username: '', password: '' })
const resetForm = reactive({ email: '', username: '', detail: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, min: 6, message: '密码至少 6 位', trigger: 'blur' }]
}
const resetRules = {
  email: [
    { required: true, message: '请输入电子邮件', trigger: 'blur' },
    { type: 'email', message: '电子邮件格式不正确', trigger: 'blur' }
  ],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  detail: [{ max: 500, message: '详情不能超过500字', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await auth.login(form.username, form.password)
    if (data.newUser) {
      ElMessage.success(`欢迎加入云阅，已自动注册账号 ${form.username}`)
    } else {
      ElMessage.success('登录成功')
    }
    router.push(route.query.redirect || '/')
  } finally {
    loading.value = false
  }
}

async function submitReset() {
  await resetFormRef.value.validate()
  resetLoading.value = true
  try {
    await submitPasswordReset({
      email: resetForm.email.trim(),
      username: resetForm.username.trim(),
      detail: resetForm.detail.trim() || undefined
    })
    ElMessage.success('已提交给管理员，请耐心等待处理')
    resetForm.email = ''
    resetForm.username = ''
    resetForm.detail = ''
    mode.value = 'login'
  } finally {
    resetLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #34495e 55%, #3f6d9e 100%);
}
.login-card {
  width: 400px;
  background: #fff;
  padding: 42px 38px;
  border: 1px solid #e4e7ed;
}
.logo {
  text-align: center;
  font-size: 28px;
  font-weight: 700;
  color: #1f2d3d;
}
.subtitle {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin: 8px 0 28px;
}
.submit {
  width: 100%;
}
.hint {
  margin-top: 20px;
  text-align: center;
  color: #c0c4cc;
  font-size: 12px;
}
.forgot-row {
  display: flex;
  justify-content: flex-end;
  margin: -4px 0 14px;
}
.forgot-link {
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
}
.forgot-link:hover {
  color: #66b1ff;
}
.reset-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  font-weight: 600;
  color: #303133;
}
</style>
