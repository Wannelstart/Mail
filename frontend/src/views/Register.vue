<template>
  <div class="auth-body">
    <div class="auth-box">
      <!-- Icon -->
      <div class="auth-icon">
        <Mail :size="48" />
      </div>

      <!-- Title -->
      <h2>创建账号</h2>
      <p class="auth-subtitle">注册新邮箱</p>

      <!-- Register Form -->
      <form @submit.prevent="handleRegister">
        <div class="form-group">
          <label>用户名</label>
          <div class="input-icon-wrapper">
            <User :size="18" />
            <input
              v-model="form.username"
              type="text"
              required
              placeholder="3-30个字符"
              minlength="3"
              maxlength="30"
            />
          </div>
        </div>

        <div class="form-group">
          <label>邮箱地址</label>
          <div class="email-input-group">
            <div class="input-icon-wrapper email-prefix">
              <Mail :size="18" />
              <input
                v-model="emailPrefix"
                type="text"
                required
                placeholder="输入邮箱前缀"
                pattern="[a-zA-Z0-9_.-]+"
                title="只能使用字母、数字、点、下划线、短横线"
              />
            </div>
            <span class="email-suffix">@lneqse.com</span>
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
              placeholder="至少6个字符"
              minlength="6"
              maxlength="64"
            />
          </div>
        </div>

        <!-- QQ邮箱绑定（可选） -->
        <div class="form-group qq-bind-section">
          <label>
            绑定QQ邮箱
            <span class="optional-tag">可选</span>
          </label>
          <div class="input-icon-wrapper">
            <Mail :size="18" />
            <input
              v-model="form.qqEmail"
              type="email"
              placeholder="如 123456@qq.com"
            />
          </div>
          <div v-if="form.qqEmail" class="input-icon-wrapper" style="margin-top: 8px;">
            <Lock :size="18" />
            <input
              v-model="form.qqAuthCode"
              type="text"
              placeholder="QQ邮箱授权码（非QQ密码）"
            />
          </div>
          <p class="form-hint">
            绑定后可向站外邮箱（QQ、网易等）发送邮件，未绑定则仅限站内互发
          </p>
        </div>

        <button type="submit" class="btn btn-primary btn-full" style="margin-top: 8px;">
          注册
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

      <!-- Login Link -->
      <p style="text-align: center; margin-top: 24px; font-size: 14px; color: var(--muted);">
        已有账号？
        <router-link to="/login" style="font-weight: 600;">去登录</router-link>
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import api from '../api'
import { Mail, User, Lock } from 'lucide-vue-next'

const router = useRouter()
const auth = useAuthStore()
const error = ref('')
const success = ref('')
const form = ref({ username: '', email: '', password: '', qqEmail: '', qqAuthCode: '' })
const emailPrefix = ref('')
const EMAIL_DOMAIN = '@lneqse.com'

async function handleRegister() {
  error.value = ''
  success.value = ''

  if (!emailPrefix.value.trim()) {
    error.value = '请输入邮箱前缀'
    return
  }

  // 自动拼接完整邮箱地址
  form.value.email = emailPrefix.value.trim() + EMAIL_DOMAIN

  try {
    const res = await api.post('/auth/register', form.value)
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
    error.value = e.response?.data?.message || '注册失败'
  }
}
</script>

<style scoped>
.email-input-group {
  display: flex;
  align-items: center;
  gap: 0;
}

.email-prefix {
  flex: 1;
}

.email-prefix input {
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
  border-right: none;
}

.email-suffix {
  padding: 10px 14px;
  background: var(--background);
  border: 1.5px solid var(--border);
  border-left: none;
  border-radius: 0 var(--radius) var(--radius) 0;
  color: var(--muted);
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  line-height: 1.5;
}

.qq-bind-section {
  border: 1px dashed var(--border);
  border-radius: var(--radius);
  padding: 16px;
  margin-top: 12px;
  background: var(--bg-subtle, #fafbfc);
}

.optional-tag {
  font-size: 12px;
  color: var(--muted);
  font-weight: 400;
  margin-left: 8px;
  background: var(--bg-muted, #e9ecef);
  padding: 2px 8px;
  border-radius: 10px;
}

.form-hint {
  font-size: 12px;
  color: var(--muted);
  margin-top: 8px;
  line-height: 1.4;
}
</style>
