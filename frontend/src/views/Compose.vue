<template>
  <div class="compose-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">
          <PenSquare :size="22" />
          {{ isDraftMode ? '编辑草稿' : (isForwardMode ? '转发邮件' : (route.query.replyTo ? '回复邮件' : '写邮件')) }}
        </h1>
      </div>
    </div>

    <!-- Draft Edit Banner -->
    <div v-if="isDraftMode" class="draft-banner">
      您正在编辑草稿，修改后将覆盖原草稿内容
    </div>

    <!-- Form Card -->
    <div class="compose-card">
      <form @submit.prevent="handleSend">
        <!-- Recent Contacts -->
        <div v-if="recentContacts.length > 0 && !isReplyMode" class="recent-contacts">
          <div class="recent-contacts-header">
            <Users :size="16" />
            <span>常用联系人</span>
          </div>
          <div class="contact-buttons">
            <button
              v-for="contact in recentContacts"
              :key="contact.contactId"
              type="button"
              class="contact-chip"
              @click="fillRecipient(contact.email)"
            >
              <span class="chip-avatar">{{ (contact.nickname || contact.username || '?').charAt(0) }}</span>
              {{ contact.nickname || contact.username }}
            </button>
          </div>
        </div>

        <!-- Recipient -->
        <div class="form-group">
          <label>收件人邮箱</label>
          <input v-model="form.receiverEmail" type="email" required placeholder="输入邮箱地址" />
          <!-- Contact Search -->
          <div v-if="!isReplyMode" class="contact-search">
            <div class="contact-search-input">
              <Search :size="16" class="contact-search-icon" />
              <input
                v-model="searchKeyword"
                @keyup="debouncedSearch"
                type="text"
                placeholder="从联系人中搜索..."
                class="search-input"
              />
            </div>
            <div v-if="searchResults.length > 0" class="search-dropdown">
              <div
                v-for="result in searchResults"
                :key="result.contactId"
                class="search-item"
                @click="selectContact(result)"
              >
                <div class="search-item-avatar">{{ (result.username || '?').charAt(0) }}</div>
                <div class="search-item-info">
                  <div class="contact-name">{{ result.username }}</div>
                  <div class="contact-email">{{ result.email }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Subject -->
        <div class="form-group">
          <label>主题</label>
          <input v-model="form.subject" type="text" placeholder="邮件主题" />
        </div>

        <!-- Content -->
        <div class="form-group">
          <label>正文</label>
          <div class="textarea-wrapper">
            <textarea v-model="form.content" rows="10" placeholder="请输入邮件内容..."></textarea>
            <div class="textarea-footer">
              <button type="button" class="ai-polish-btn" @click="handleAiOptimize" :disabled="aiLoading || !form.content.trim()">
                <Sparkles :size="14" />
                {{ aiLoading ? 'AI润色中...' : '点击进行AI文本润色' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Attachments -->
        <div class="form-group">
          <label>附件</label>
          <input type="file" multiple @change="handleFiles" ref="fileInput" style="display:none;" />
          <button type="button" class="btn-add-attachment" @click="triggerFileInput">
            <Paperclip :size="16" />
            添加附件
          </button>
          <span class="attachment-hint-top">每个文件≤10MB，总计≤20MB</span>

          <!-- Attachment List -->
          <div v-if="files.length > 0 || existingAttachments.length > 0" class="attachment-section">
            <div class="attachment-counter" :class="counterClass">
              已选择 {{ totalAttachmentCount }}/{{ MAX_ATTACHMENTS }} 个附件
            </div>
            <div v-for="att in existingAttachments" :key="'e-'+att.id" class="attachment-item">
              <FileText :size="16" class="att-icon" />
              <span class="att-name">{{ att.fileName }}</span>
              <span class="att-size">{{ formatSize(att.fileSize) }}</span>
              <button type="button" class="remove-btn" @click="removeExistingAttachment(att.id)">
                <X :size="14" />
              </button>
            </div>
            <div v-for="(file, index) in files" :key="'n-'+index" class="attachment-item">
              <FileText :size="16" class="att-icon" />
              <span class="att-name">{{ file.name }}</span>
              <span class="att-size">{{ formatSize(file.size) }}</span>
              <button type="button" class="remove-btn" @click="removeFile(index)">
                <X :size="14" />
              </button>
            </div>
          </div>
        </div>

        <!-- Forward Attachments -->
        <div v-if="isForwardMode && originalMail?.attachments?.length > 0" class="form-group forward-section">
          <label>转发附件（选择要携带的附件）</label>
          <div v-for="att in originalMail.attachments" :key="'fwd-'+att.id" class="forward-attachment">
            <input type="checkbox" :value="att.id" v-model="selectedForwardAttachments" :id="'fwd-att-'+att.id" />
            <label :for="'fwd-att-'+att.id" class="checkbox-label">
              <FileText :size="14" />
              {{ att.fileName }} ({{ formatSize(att.fileSize) }})
            </label>
          </div>
        </div>

        <!-- Actions -->
        <div class="form-actions">
          <button type="submit" class="btn btn-primary" :disabled="loading">
            {{ loading ? '发送中...' : '发送' }}
          </button>
          <button type="button" class="btn btn-outline" @click="handleSaveDraft" :disabled="loading">
            存为草稿
          </button>
          <button type="button" class="btn btn-outline" @click="handleCancel">
            取消
          </button>
        </div>

        <div v-if="error" class="alert alert-error">{{ error }}</div>
        <div v-if="success && !sentMailId" class="alert alert-success">{{ success }}</div>
      </form>
    </div>

    <!-- Recall Bar -->
    <div v-if="sentMailId && recallCountdown > 0" class="recall-bar">
      <div class="recall-info">
        <span>邮件已发送，可在 {{ recallCountdown }} 秒内撤回</span>
      </div>
      <div class="recall-actions">
        <button type="button" class="btn btn-recall" @click="handleRecall" :disabled="loading">
          撤回邮件
        </button>
        <button type="button" class="btn btn-outline" @click="cancelRecall">
          忽略
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '../api'
import { PenSquare, Users, Search, Paperclip, FileText, X, Sparkles } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

// Constants
const MAX_ATTACHMENTS = 5
const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
const MAX_TOTAL_SIZE = 20 * 1024 * 1024 // 20MB

// State
const form = ref({ receiverEmail: '', subject: '', content: '' })
const files = ref([])
const existingAttachments = ref([]) // For draft/forward mode
const error = ref('')
const success = ref('')
const loading = ref(false)
const aiLoading = ref(false)

// Recall functionality
const sentMailId = ref(null)
const recallCountdown = ref(0)
let recallTimer = null

// Contact search
const searchKeyword = ref('')
const searchResults = ref([])
const recentContacts = ref([])
let searchTimeout = null

// Mode detection
const draftId = ref(null)
const forwardId = ref(null)
const originalMail = ref(null)
const selectedForwardAttachments = ref([])

const isDraftMode = computed(() => !!draftId.value)
const isForwardMode = computed(() => !!forwardId.value)
const isReplyMode = computed(() => !!route.query.replyTo)

// Template refs
const fileInput = ref(null)

function triggerFileInput() {
  fileInput.value?.click()
}

// Attachment validation
const totalAttachmentCount = computed(() => files.value.length + existingAttachments.value.length)
const totalFileSize = computed(() => {
  const newFilesSize = files.value.reduce((sum, f) => sum + f.size, 0)
  const existingSize = existingAttachments.value.reduce((sum, a) => sum + a.fileSize, 0)
  return newFilesSize + existingSize
})

const counterClass = computed(() => {
  if (totalAttachmentCount.value >= MAX_ATTACHMENTS) return 'counter-full'
  if (totalAttachmentCount.value > 0) return 'counter-has-files'
  return ''
})

// Lifecycle
onMounted(async () => {
  // Load recent contacts
  await loadRecentContacts()

  // Check route query params
  draftId.value = route.query.draftId
  forwardId.value = route.query.forwardId

  if (isDraftMode.value) {
    await loadDraft(draftId.value)
  } else if (isForwardMode.value) {
    await loadForwardMail(forwardId.value)
  } else if (route.query.replyTo) {
    form.value.receiverEmail = route.query.replyTo
    if (route.query.replySubject) {
      form.value.subject = `Re: ${route.query.replySubject}`
    }
  }

  // Close dropdown on outside click
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  if (recallTimer) {
    clearInterval(recallTimer)
    recallTimer = null
  }
})

// Functions
async function loadRecentContacts() {
  try {
    const res = await api.get('/contact')
    if (res.code === 0) {
      recentContacts.value = res.data || []
    }
  } catch (e) {
    console.error('Failed to load contacts:', e)
  }
}

async function loadDraft(id) {
  try {
    const res = await api.get(`/mail/${id}`)
    if (res.code === 0) {
      const mail = res.data
      form.value.receiverEmail = mail.receiverEmail || ''
      form.value.subject = mail.subject || ''
      form.value.content = mail.content || ''
      existingAttachments.value = mail.attachments || []
    }
  } catch (e) {
    error.value = '加载草稿失败'
  }
}

async function loadForwardMail(id) {
  try {
    const res = await api.get(`/mail/${id}`)
    if (res.code === 0) {
      originalMail.value = res.data
      form.value.subject = `Fwd: ${res.data.subject || ''}`
      form.value.content = ''
      existingAttachments.value = []
    }
  } catch (e) {
    error.value = '加载转发邮件失败'
  }
}

function fillRecipient(email) {
  form.value.receiverEmail = email
}

function debouncedSearch() {
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    performSearch()
  }, 300)
}

async function performSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) {
    searchResults.value = []
    return
  }

  try {
    const res = await api.get('/contact/search', { params: { keyword } })
    if (res.code === 0) {
      searchResults.value = (res.data || []).slice(0, 10)
    }
  } catch (e) {
    console.error('Search failed:', e)
  }
}

function selectContact(contact) {
  form.value.receiverEmail = contact.email
  searchResults.value = []
  searchKeyword.value = ''
}

function handleClickOutside(event) {
  const dropdown = document.querySelector('.search-dropdown')
  const searchInput = document.querySelector('.search-input')
  if (dropdown && searchInput && !dropdown.contains(event.target) && !searchInput.contains(event.target)) {
    searchResults.value = []
  }
}

function handleFiles(e) {
  const newFiles = Array.from(e.target.files)

  // Validate count
  if (totalAttachmentCount.value + newFiles.length > MAX_ATTACHMENTS) {
    error.value = `最多只能添加 ${MAX_ATTACHMENTS} 个附件`
    return
  }

  // Validate individual file size
  for (const file of newFiles) {
    if (file.size > MAX_FILE_SIZE) {
      error.value = `文件 "${file.name}" 超过 10MB 限制`
      return
    }
  }

  files.value = [...files.value, ...newFiles]
  error.value = ''
}

function removeFile(index) {
  files.value.splice(index, 1)
}

function removeExistingAttachment(id) {
  existingAttachments.value = existingAttachments.value.filter(a => a.id !== id)
}

function formatSize(bytes) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

function validateAttachments() {
  if (totalAttachmentCount.value > MAX_ATTACHMENTS) {
    error.value = `最多只能添加 ${MAX_ATTACHMENTS} 个附件`
    return false
  }

  if (totalFileSize.value > MAX_TOTAL_SIZE) {
    error.value = '附件总大小不能超过 20MB'
    return false
  }

  for (const file of files.value) {
    if (file.size > MAX_FILE_SIZE) {
      error.value = `文件 "${file.name}" 超过 10MB 限制`
      return false
    }
  }

  return true
}

async function handleSend() {
  if (!validateAttachments()) return

  error.value = ''
  loading.value = true

  try {
    let mailId

    if (isDraftMode.value) {
      // Update existing draft
      const res = await api.put(`/mail/${draftId.value}`, { ...form.value, draft: false })
      if (res.code === 0) {
        mailId = draftId.value
      } else {
        error.value = res.message || '发送失败'
        return
      }
    } else {
      // Create new mail
      const res = await api.post('/mail/send', { ...form.value, draft: false })
      if (res.code === 0) {
        mailId = res.data.id
      } else {
        error.value = res.message || '发送失败'
        return
      }
    }

    // Upload new attachments
    if (files.value.length > 0) {
      const formData = new FormData()
      files.value.forEach(f => formData.append('files', f))
      await api.post(`/attachment/upload/${mailId}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    }

    // Handle forward attachments
    if (isForwardMode.value && selectedForwardAttachments.value.length > 0) {
      await api.post(`/mail/${mailId}/copy-attachments`, {
        sourceMailId: forwardId.value,
        attachmentIds: selectedForwardAttachments.value
      })
    }

    success.value = '发送成功！'
    // 开始撤回倒计时（2分钟 = 120秒）
    sentMailId.value = mailId
    recallCountdown.value = 120
    startRecallCountdown()
  } catch (e) {
    error.value = e.response?.data?.message || '发送失败'
  } finally {
    loading.value = false
  }
}

function startRecallCountdown() {
  if (recallTimer) clearInterval(recallTimer)
  recallTimer = setInterval(() => {
    recallCountdown.value--
    if (recallCountdown.value <= 0) {
      clearInterval(recallTimer)
      recallTimer = null
      sentMailId.value = null
      router.push('/sent')
    }
  }, 1000)
}

async function handleRecall() {
  if (!sentMailId.value) return
  loading.value = true
  error.value = ''
  try {
    const res = await api.post(`/mail/${sentMailId.value}/recall`)
    if (res.code === 0) {
      success.value = '邮件已撤回到草稿箱'
      if (recallTimer) {
        clearInterval(recallTimer)
        recallTimer = null
      }
      sentMailId.value = null
      recallCountdown.value = 0
      setTimeout(() => router.push('/drafts'), 1500)
    } else {
      error.value = res.message || '撤回失败'
    }
  } catch (e) {
    error.value = e.response?.data?.message || '撤回失败'
  } finally {
    loading.value = false
  }
}

async function cancelRecall() {
  if (sentMailId.value) {
    try {
      await api.post(`/mail/${sentMailId.value}/send-now`)
    } catch (e) {
      // 即使发送失败也关闭撤回栏
    }
  }
  if (recallTimer) {
    clearInterval(recallTimer)
    recallTimer = null
  }
  sentMailId.value = null
  recallCountdown.value = 0
  router.push('/sent')
}

async function handleSaveDraft() {
  if (!validateAttachments()) return

  error.value = ''
  loading.value = true

  try {
    let mailId

    if (isDraftMode.value) {
      // Update existing draft
      const res = await api.put(`/mail/${draftId.value}`, { ...form.value, draft: true })
      if (res.code === 0) {
        mailId = draftId.value
      } else {
        error.value = res.message || '保存失败'
        return
      }
    } else {
      // Create new draft
      const res = await api.post('/mail/send', { ...form.value, draft: true })
      if (res.code === 0) {
        mailId = res.data.id
      } else {
        error.value = res.message || '保存失败'
        return
      }
    }

    // Upload new attachments
    if (files.value.length > 0) {
      const formData = new FormData()
      files.value.forEach(f => formData.append('files', f))
      await api.post(`/attachment/upload/${mailId}`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
    }

    // Handle forward attachments
    if (isForwardMode.value && selectedForwardAttachments.value.length > 0) {
      await api.post(`/mail/${mailId}/copy-attachments`, {
        sourceMailId: forwardId.value,
        attachmentIds: selectedForwardAttachments.value
      })
    }

    success.value = '草稿已保存！'
    setTimeout(() => router.push('/drafts'), 1000)
  } catch (e) {
    error.value = e.response?.data?.message || '保存失败'
  } finally {
    loading.value = false
  }
}

async function handleAiOptimize() {
  if (!form.value.content.trim()) {
    error.value = '请先输入邮件内容'
    return
  }
  aiLoading.value = true
  error.value = ''
  try {
    const action = isForwardMode.value ? 'forward' : (route.query.replyTo ? 'reply' : 'compose')
    console.log('AI优化请求:', { content: form.value.content, action })
    const res = await api.post('/ai/optimize', { content: form.value.content, action })
    console.log('AI优化响应:', res)
    if (res.code === 0) {
      form.value.content = res.data.text
    } else {
      error.value = res.message || 'AI优化失败'
    }
  } catch (e) {
    console.error('AI优化错误:', e)
    error.value = e.response?.data?.message || e.message || 'AI优化失败'
  } finally {
    aiLoading.value = false
  }
}

function handleCancel() {
  if (isDraftMode.value) {
    router.push('/drafts')
  } else if (isForwardMode.value) {
    router.push('/inbox')
  } else {
    router.push('/inbox')
  }
}
</script>

<style scoped>
.compose-page {
  max-width: 1200px;
  margin: 0 auto;
}

.textarea-wrapper {
  position: relative;
}

.textarea-wrapper textarea {
  width: 100%;
  border-radius: var(--radius) var(--radius) 0 0;
  border-bottom: none;
  resize: vertical;
}

.textarea-footer {
  display: flex;
  justify-content: flex-end;
  padding: 8px 12px;
  background: var(--background);
  border: 1.5px solid var(--border);
  border-top: 1px solid var(--border);
  border-radius: 0 0 var(--radius) var(--radius);
}

.ai-polish-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 14px;
  border: none;
  border-radius: 20px;
  background: transparent;
  color: var(--muted);
  font-size: 13px;
  cursor: pointer;
  transition: var(--transition);
}

.ai-polish-btn:hover:not(:disabled) {
  background: var(--primary-light, #DBEAFE);
  color: var(--primary);
}

.ai-polish-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--foreground);
  display: flex;
  align-items: center;
  gap: 8px;
}

.draft-banner {
  background: var(--warning-light, #FEF3C7);
  border: 1px solid var(--warning, #F59E0B);
  color: #92400E;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  margin-bottom: 16px;
  font-size: 14px;
  font-weight: 500;
}

.compose-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 28px 32px;
  margin-bottom: 16px;
}

/* Recent Contacts */
.recent-contacts {
  margin-bottom: 24px;
  padding: 16px;
  background: var(--background);
  border-radius: var(--radius);
  border: 1px solid var(--border);
}

.recent-contacts-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
}

.contact-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.contact-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  cursor: pointer;
  font-size: 13px;
  color: var(--foreground);
  transition: var(--transition);
}

.contact-chip:hover {
  background: var(--primary-light);
  border-color: var(--primary);
  color: var(--primary);
}

.chip-avatar {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
}

/* Contact Search */
.contact-search {
  position: relative;
  margin-top: 8px;
}

.contact-search-input {
  position: relative;
}

.contact-search-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--muted);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 9px 12px 9px 36px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 14px;
  transition: var(--transition);
  background: var(--background);
}

.search-input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
  background: var(--surface);
}

.search-dropdown {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  right: 0;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  box-shadow: var(--shadow-md);
  max-height: 240px;
  overflow-y: auto;
  z-index: 100;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.15s ease;
  border-bottom: 1px solid var(--border);
}

.search-item:last-child {
  border-bottom: none;
}

.search-item:hover {
  background: var(--primary-light);
}

.search-item-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.search-item-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-weight: 500;
  font-size: 14px;
  color: var(--foreground);
}

.contact-email {
  font-size: 12px;
  color: var(--muted);
}

/* Attachment */
.btn-add-attachment {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--background);
  border: 1px dashed var(--border);
  border-radius: var(--radius);
  cursor: pointer;
  font-size: 13px;
  color: var(--secondary);
  transition: var(--transition);
}

.btn-add-attachment:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-light);
}

.attachment-hint-top {
  font-size: 12px;
  color: var(--muted);
  margin-left: 10px;
}

.attachment-section {
  margin-top: 12px;
  padding: 14px;
  background: var(--background);
  border-radius: var(--radius);
  border: 1px solid var(--border);
}

.attachment-counter {
  font-size: 13px;
  font-weight: 500;
  margin-bottom: 10px;
  padding: 5px 12px;
  border-radius: 20px;
  display: inline-block;
}

.counter-has-files {
  background: var(--accent-light, #D1FAE5);
  color: #065F46;
}

.counter-full {
  background: var(--destructive-light, #FEE2E2);
  color: #991B1B;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  margin-bottom: 6px;
  transition: var(--transition);
}

.attachment-item:hover {
  border-color: var(--primary);
}

.att-icon {
  color: var(--primary);
  flex-shrink: 0;
}

.att-name {
  flex: 1;
  font-size: 13px;
  color: var(--foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.att-size {
  font-size: 12px;
  color: var(--muted);
  flex-shrink: 0;
}

.remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--destructive-light, #FEE2E2);
  color: var(--destructive);
  border: none;
  cursor: pointer;
  transition: var(--transition);
  flex-shrink: 0;
}

.remove-btn:hover {
  background: var(--destructive);
  color: white;
}

/* Forward */
.forward-section {
  background: var(--primary-light);
  border-radius: var(--radius);
  padding: 16px;
  border: 1px solid rgba(37, 99, 235, 0.15);
}

.forward-attachment {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.forward-attachment input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: var(--primary);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: var(--foreground);
}

/* Actions */
.form-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid var(--border);
}

/* Alerts */
.alert {
  margin-top: 16px;
  padding: 12px 16px;
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 500;
}

.alert-error {
  background: var(--destructive-light, #FEE2E2);
  color: #991B1B;
  border: 1px solid var(--destructive, #DC2626);
}

.alert-success {
  background: var(--accent-light, #D1FAE5);
  color: #065F46;
  border: 1px solid var(--accent, #059669);
}
.recall-bar {
  margin: 20px auto;
  max-width: 800px;
  background: var(--bg-secondary, #f5f5f5);
  border: 1px solid var(--border, #e0e0e0);
  border-radius: 8px;
  padding: 16px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.recall-info {
  font-size: 14px;
  color: var(--text-primary, #333);
}

.recall-actions {
  display: flex;
  gap: 8px;
}

.btn-recall {
  background: var(--danger, #dc3545);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background 0.2s;
}

.btn-recall:hover {
  background: var(--danger-hover, #c82333);
}

.btn-recall:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
