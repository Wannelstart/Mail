<template>
  <div class="view-mail-page" v-if="mail">
    <!-- Action Bar -->
    <div class="action-bar">
      <button class="action-btn" @click="$router.back()" title="返回">
        <ArrowLeft :size="18" />
      </button>
      <div class="action-divider"></div>
      <button
        class="action-btn"
        :class="{ active: mail.starred }"
        @click="toggleFlag('starred', !mail.starred)"
        :title="mail.starred ? '取消星标' : '标星'"
      >
        <Star :size="18" :fill="mail.starred ? 'var(--star)' : 'none'" :color="mail.starred ? 'var(--star)' : 'currentColor'" />
      </button>
      <button
        class="action-btn"
        :class="{ active: mail.important }"
        @click="toggleFlag('important', !mail.important)"
        :title="mail.important ? '取消重要' : '标记重要'"
      >
        <Flag :size="18" :fill="mail.important ? 'var(--important)' : 'none'" :color="mail.important ? 'var(--important)' : 'currentColor'" />
      </button>
      <button
        v-if="mail.senderEmail !== currentEmail"
        class="action-btn"
        @click="replyMail"
        title="回复"
      >
        <Reply :size="18" />
      </button>
      <button class="action-btn" @click="forwardMail" title="转发">
        <Share2 :size="18" />
      </button>
      <div class="action-spacer"></div>
      <button class="action-btn action-btn-danger" @click="deleteMail" title="删除">
        <Trash2 :size="18" />
      </button>
    </div>

    <!-- Mail Content Card -->
    <div class="mail-content-card">
      <!-- Header -->
      <div class="mail-header">
        <h2 class="mail-subject">{{ mail.subject || '(无主题)' }}</h2>
        <div class="mail-meta">
          <div class="meta-row">
            <User :size="15" class="meta-icon" />
            <span class="meta-label">发件人</span>
            <span class="meta-value">
              {{ mail.remote ? mail.remoteSenderEmail : `${mail.senderUsername} <${mail.senderEmail}>` }}
            </span>
          </div>
          <div class="meta-row">
            <User :size="15" class="meta-icon" />
            <span class="meta-label">收件人</span>
            <span class="meta-value">{{ mail.receiverEmail }}</span>
          </div>
          <div class="meta-row">
            <Clock :size="15" class="meta-icon" />
            <span class="meta-label">时间</span>
            <span class="meta-value">{{ formatTime(mail.sentAt || mail.createdAt) }}</span>
          </div>
        </div>
      </div>

      <!-- Divider -->
      <div class="mail-divider"></div>

      <!-- Content -->
      <div v-if="isHtmlContent" class="mail-body mail-body-html" v-html="sanitizedContent"></div>
      <div v-else class="mail-body">{{ mail.content }}</div>

      <!-- Attachments -->
      <div v-if="mail.attachments && mail.attachments.length > 0" class="attachments-section">
        <div class="attachments-header">
          <Paperclip :size="16" />
          <span>附件（{{ mail.attachments.length }}）</span>
        </div>
        <div class="attachments-grid">
          <div v-for="a in mail.attachments" :key="a.id" class="attachment-card">
            <div class="attach-icon-wrap">
              <FileText :size="20" />
            </div>
            <div class="attach-info">
              <span class="attach-name">{{ a.fileName }}</span>
              <span class="attach-size">{{ formatSize(a.fileSize) }}</span>
            </div>
            <button class="attach-download" @click="downloadFile(a)">
              <Download :size="16" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { useAuthStore } from '../stores/auth'
import { ArrowLeft, Trash2, Star, Flag, Reply, Share2, Download, Paperclip, User, Clock, FileText } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const mail = ref(null)
const currentEmail = computed(() => auth.user?.email || '')
const isHtmlContent = computed(() => {
  const content = mail.value?.content || ''
  return content.includes('<') && content.includes('>')
})
const sanitizedContent = computed(() => {
  if (!mail.value?.content) return ''
  let html = mail.value.content
  // 移除可能影响页面布局的标签
  html = html.replace(/<style[^>]*>[\s\S]*?<\/style>/gi, '')
  html = html.replace(/<script[^>]*>[\s\S]*?<\/script>/gi, '')
  html = html.replace(/<link[^>]*>/gi, '')
  html = html.replace(/<meta[^>]*>/gi, '')
  html = html.replace(/<title[^>]*>[\s\S]*?<\/title>/gi, '')
  html = html.replace(/<\/?(html|head|body)[^>]*>/gi, '')
  html = html.replace(/<\!--[\s\S]*?-->/g, '')
  return html
})

async function loadMail() {
  const res = await api.get(`/mail/${route.params.id}`)
  if (res.code === 0) {
    mail.value = res.data
  }
}

async function toggleFlag(type, value) {
  await api.put(`/mail/${mail.value.id}/flag`, { [type]: value })
  mail.value[type] = value
}

async function deleteMail() {
  if (!confirm('确定删除？')) return
  await api.delete(`/mail/${mail.value.id}`)
  router.back()
}

function replyMail() {
  router.push(`/compose?replyTo=${mail.value.senderEmail}&replySubject=${encodeURIComponent(mail.value.subject)}`)
}

function forwardMail() {
  router.push(`/compose?forwardId=${mail.value.id}`)
}

async function downloadFile(attachment) {
  try {
    const res = await api.get(`/attachment/download/${attachment.id}`, { responseType: 'blob' })
    const blob = new Blob([res])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = attachment.fileName || '附件'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  } catch (e) {
    alert('下载失败，请重试')
  }
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024*1024) return (bytes/1024).toFixed(1) + ' KB'
  return (bytes/1024/1024).toFixed(1) + ' MB'
}

onMounted(loadMail)
</script>

<style scoped>
.view-mail-page {
  max-width: 900px;
  margin: 0 auto;
}

.action-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 8px 12px;
  margin-bottom: 16px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: none;
  border-radius: var(--radius);
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  transition: var(--transition);
}

.action-btn:hover {
  background: var(--background);
  color: var(--foreground);
}

.action-btn.active {
  color: var(--primary);
  background: var(--primary-light);
}

.action-btn-danger:hover {
  background: #FEF2F2;
  color: var(--destructive);
}

.action-divider {
  width: 1px;
  height: 24px;
  background: var(--border);
  margin: 0 6px;
}

.action-spacer {
  flex: 1;
}

.mail-content-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow);
  padding: 32px;
}

.mail-header {
  margin-bottom: 0;
}

.mail-subject {
  font-size: 22px;
  font-weight: 700;
  color: var(--foreground);
  margin-bottom: 20px;
  line-height: 1.3;
}

.mail-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.meta-icon {
  color: var(--muted);
  flex-shrink: 0;
}

.meta-label {
  color: var(--muted);
  min-width: 50px;
  font-weight: 500;
}

.meta-value {
  color: var(--foreground);
}

.mail-divider {
  height: 1px;
  background: var(--border);
  margin: 24px 0;
}

.mail-body {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 15px;
  color: var(--foreground);
  word-break: break-word;
}

.mail-body-html {
  white-space: normal;
  line-height: 1.8;
  font-size: 15px;
  color: var(--foreground);
  word-break: break-word;
}

.mail-body-html a {
  color: var(--primary);
  text-decoration: underline;
  cursor: pointer;
}

.mail-body-html a:hover {
  color: var(--primary-hover);
}

.mail-body-html img {
  max-width: 100%;
  height: auto;
}

.mail-body-html table {
  border-collapse: collapse;
  max-width: 100%;
}

.mail-body-html td, .mail-body-html th {
  padding: 8px;
  border: 1px solid var(--border);
}

.attachments-section {
  margin-top: 28px;
  padding-top: 24px;
  border-top: 1px solid var(--border);
}

.attachments-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--foreground);
  margin-bottom: 14px;
}

.attachments-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--background);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  transition: var(--transition);
}

.attachment-card:hover {
  border-color: var(--primary);
  background: var(--primary-light);
}

.attach-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: var(--radius);
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.attach-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.attach-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attach-size {
  font-size: 12px;
  color: var(--muted);
}

.attach-download {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius);
  background: var(--primary);
  color: white;
  text-decoration: none;
  transition: var(--transition);
  flex-shrink: 0;
  border: none;
  cursor: pointer;
}

.attach-download:hover {
  background: var(--primary-hover);
}
</style>
