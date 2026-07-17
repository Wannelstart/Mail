<template>
  <div class="auth-body">
    <div class="auth-box">
      <!-- Icon -->
      <div class="auth-icon">
        <Mail :size="48" />
      </div>

      <!-- Title -->
      <h2>欢迎回来</h2>
      <p class="auth-subtitle">登录您的邮箱</p>

      <!-- Login Form -->
      <form @submit.prevent="handleLogin">
        <div class="form-group">
          <label>邮箱地址</label>
          <div class="input-icon-wrapper">
            <Mail :size="18" />
            <input
              v-model="form.email"
              type="email"
              required
              placeholder="请输入邮箱地址"
            />
          </div>
        </div>

        <div class="form-group">
          <label>密码</label>
          <div class="input-icon-wrapper">
            <Lock :size="18" />
            <input
              v-model="form.password"
              type="password"
              required
              placeholder="请输入密码"
            />
          </div>
        </div>

        <button type="submit" class="btn btn-primary btn-full" style="margin-top: 8px;">
          登录
        </button>
      </form>

      <!-- Error Message -->
      <div v-if="error" class="error-msg">
        {{ error }}
      </div>

      <!-- Success Message -->
      <div v-if="success" class="success-msg">
        {{ success }}
      </div>

      <!-- Register Link -->
      <p style="text-align: center; margin-top: 24px; font-size: 14px; color: var(--muted);">
        还没有账号？
        <router-link to="/register" style="font-weight: 600;">去注册</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../api'
import { Mail, User, Lock } from 'lucide-vue-next'

const router = useRouter()
const auth = useAuthStore()
const error = ref('')
const success = ref('')
const form = ref({ email: '', password: '' })

async function handleLogin() {
  error.value = ''
  success.value = ''
  try {
    const res = await api.post('/auth/login', form.value)
    if (res.code === 0) {
      auth.setAuth(res.data.token, {
        id: res.data.id,
        username: res.data.username,
        email: res.data.email,
        avatar: res.data.avatar,
        qqEmail: res.data.qqEmail
      })
      router.push('/inbox')
    } else {
      error.value = res.message
    }
  } catch (e) {
    error.value = e.response?.data?.message || '登录失败'
  }
}
</script>
