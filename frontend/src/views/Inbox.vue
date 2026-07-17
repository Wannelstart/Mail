<template>
  <div class="inbox-page">
    <!-- Page Header -->
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">收件箱</h1>
        <span class="count-badge">{{ total }} 封邮件</span>
      </div>
    </div>

    <!-- Search Bar -->
    <div class="search-card">
      <div class="search-row">
        <div class="search-input-wrap">
          <Search :size="18" class="search-icon" />
          <input
            v-model="keyword"
            @keyup.enter="doSearch"
            placeholder="搜索邮件..."
            class="search-input"
          />
        </div>
        <select v-model="searchIn" class="search-select">
          <option value="all">全部</option>
          <option value="subject">主题</option>
          <option value="content">内容</option>
          <option value="sender">发件人</option>
        </select>
        <button class="btn-search" @click="doSearch">
          <Search :size="16" />
          搜索
        </button>
      </div>
    </div>

    <!-- Mail Table -->
    <div class="mail-card">
      <table class="mail-table">
        <thead>
          <tr>
            <th width="40"></th>
            <th width="160">发件人</th>
            <th>主题</th>
            <th width="140">时间</th>
            <th width="150">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="m in mails"
            :key="m.id"
            class="mail-row"
            :class="{
              'mail-unread': !m.read,
              'mail-important': m.important
            }"
            @click="router.push(`/mail/${m.id}`)"
            style="cursor: pointer;"
          >
            <td @click.stop>
              <button
                class="star-btn"
                @click="toggleFlag(m, 'starred', !m.starred)"
              >
                <Star
                  :size="16"
                  :fill="m.starred ? 'var(--star)' : 'none'"
                  :color="m.starred ? 'var(--star)' : 'var(--muted)'"
                />
              </button>
            </td>
            <td>
              <div class="sender-cell">
                <div class="avatar">
                  {{ getInitial(m.remote ? m.remoteSenderEmail : m.senderUsername) }}
                </div>
                <span class="sender-name">
                  {{ m.remote ? m.remoteSenderEmail : m.senderUsername }}
                </span>
              </div>
            </td>
            <td>
              <span class="subject-link">
                {{ m.subject || '(无主题)' }}
              </span>
              <Paperclip v-if="m.hasAttachments" :size="14" class="attach-icon" />
            </td>
            <td>
              <span class="time-text">{{ formatTime(m.sentAt) }}</span>
            </td>
            <td @click.stop>
              <div class="action-btns">
                <button
                  class="btn-action btn-flag"
                  :class="{ active: m.important }"
                  @click="toggleFlag(m, 'important', !m.important)"
                >
                  <Flag :size="13" />
                  重要
                </button>
                <button class="btn-action btn-delete" @click="deleteMail(m.id)">
                  <Trash2 :size="13" />
                  删除
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!loaded && mails.length === 0">
            <td colspan="5" class="skeleton-state">
              <div v-for="i in 6" :key="i" class="skeleton-row">
                <div class="skeleton skeleton-icon"></div>
                <div class="skeleton skeleton-name"></div>
                <div class="skeleton skeleton-subject"></div>
                <div class="skeleton skeleton-time"></div>
              </div>
            </td>
          </tr>
          <tr v-else-if="loaded && mails.length === 0">
            <td colspan="5" class="empty-state">
              <div class="empty-inner">
                <Mail :size="40" color="var(--muted)" />
                <p>暂无邮件</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div class="pagination">
      <button
        class="page-btn"
        :disabled="page <= 1"
        @click="page--; loadMails()"
      >
        上一页
      </button>
      <span class="page-info">第 {{ page }} 页 / 共 {{ totalPages }} 页（{{ total }} 封）</span>
      <button
        class="page-btn"
        :disabled="page >= totalPages"
        @click="page++; loadMails()"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onActivated, onDeactivated } from 'vue'
import { useRouter } from 'vue-router'
import api from '../api'
import { Search, Star, Paperclip, Trash2, Flag, Mail } from 'lucide-vue-next'
import { useMailStore } from '../stores/mail'

defineOptions({ name: 'InboxPage' })

const router = useRouter()
const mailStore = useMailStore()
const mails = ref([])
const page = ref(1)
const total = ref(0)
const size = 20
const keyword = ref('')
const searchIn = ref('all')
const loaded = ref(false)

const totalPages = computed(() => Math.ceil(total.value / size) || 1)

async function loadMails(silent = false) {
  if (keyword.value) return doSearch()
  // 首次加载优先用预取缓存
  if (!loaded.value && mailStore.inbox) {
    mails.value = mailStore.inbox.list
    total.value = mailStore.inbox.total
    loaded.value = true
    mailStore.inbox = null
  }
  const res = await api.get('/mail/inbox', { params: { page: page.value, size } })
  if (res.code === 0) {
    const newList = res.data.list
    const newTotal = res.data.total
    if (newTotal !== total.value || JSON.stringify(newList) !== JSON.stringify(mails.value)) {
      mails.value = newList
      total.value = newTotal
    }
  }
  loaded.value = true
}

async function doSearch() {
  if (!keyword.value.trim()) { keyword.value = ''; return loadMails() }
  const res = await api.get('/search', { params: { keyword: keyword.value, box: 'inbox', searchIn: searchIn.value, page: page.value, size } })
  if (res.code === 0) {
    mails.value = res.data.list
    total.value = res.data.total
  }
  loaded.value = true
}

async function toggleFlag(mail, type, value) {
  await api.put(`/mail/${mail.id}/flag`, { [type]: value })
  mail[type] = value
  if (type === 'important') {
    await loadMails() // 重要邮件置顶，刷新列表
  }
}

async function deleteMail(id) {
  if (!confirm('确定删除这封邮件？')) return
  await api.delete(`/mail/${id}`)
  loadMails()
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

function getInitial(email) {
  if (!email) return '?'
  return email.charAt(0).toUpperCase()
}

let refreshTimer = null
onActivated(() => {
  loadMails()
  refreshTimer = setInterval(loadMails, 10000) // 10秒刷新
})
onDeactivated(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})
</script>

<style scoped>
.inbox-page {
  max-width: 1200px;
  margin: 0 auto;
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
}

.count-badge {
  background: var(--primary-light);
  color: var(--primary);
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.search-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 16px 20px;
  margin-bottom: 16px;
}

.search-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.search-input-wrap {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 12px;
  color: var(--muted);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 10px 12px 10px 40px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 14px;
  color: var(--foreground);
  background: var(--background);
  transition: var(--transition);
  outline: none;
}

.search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.search-select {
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 14px;
  color: var(--foreground);
  background: var(--surface);
  cursor: pointer;
  transition: var(--transition);
  outline: none;
}

.search-select:focus {
  border-color: var(--primary);
}

.btn-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: var(--radius);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-search:hover {
  background: var(--primary-hover);
}

.mail-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  margin-bottom: 16px;
}

.mail-table {
  width: 100%;
  border-collapse: collapse;
}

.mail-table th {
  padding: 12px 16px;
  text-align: left;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: var(--muted);
  border-bottom: 1px solid var(--border);
  background: var(--background);
}

.mail-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}

.mail-row {
  transition: background 0.15s ease;
}

.mail-row:hover {
  background: var(--background);
}

.mail-row:last-child td {
  border-bottom: none;
}

.mail-unread {
  border-left: 3px solid var(--primary);
  background: rgba(37, 99, 235, 0.02);
}

.mail-unread .subject-link {
  font-weight: 700;
}

.mail-unread .sender-name {
  font-weight: 600;
}

.mail-important {
  border-left: 3px solid var(--important);
}

.star-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: var(--transition);
  display: flex;
  align-items: center;
  justify-content: center;
}

.star-btn:hover {
  background: var(--background);
}

.sender-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.sender-name {
  font-size: 14px;
  color: var(--foreground);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.subject-link {
  color: var(--foreground);
  text-decoration: none;
  font-size: 14px;
  transition: var(--transition);
}

.subject-link:hover {
  color: var(--primary);
}

.attach-icon {
  color: var(--muted);
  margin-left: 6px;
  vertical-align: middle;
}

.time-text {
  font-size: 13px;
  color: var(--muted);
}

.action-btns {
  display: flex;
  gap: 6px;
}

.btn-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  background: var(--surface);
  color: var(--muted);
}

.btn-action:hover {
  background: var(--background);
  color: var(--foreground);
}

.btn-flag.active {
  background: #FEF2F2;
  border-color: var(--important);
  color: var(--important);
}

.btn-delete:hover {
  background: #FEF2F2;
  border-color: var(--destructive);
  color: var(--destructive);
}

.empty-state {
  padding: 60px 20px !important;
  border-bottom: none !important;
}

.empty-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: var(--muted);
}

.empty-inner p {
  font-size: 15px;
}

.skeleton-state {
  padding: 0 !important;
  border-bottom: none !important;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 20px;
  border-bottom: 1px solid var(--border);
}

.skeleton {
  background: linear-gradient(90deg, var(--border) 25%, var(--muted-light, #e5e7eb) 50%, var(--border) 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 6px;
}

.skeleton-icon {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  flex-shrink: 0;
}

.skeleton-name {
  width: 120px;
  height: 14px;
  flex-shrink: 0;
}

.skeleton-subject {
  flex: 1;
  height: 14px;
}

.skeleton-time {
  width: 100px;
  height: 14px;
  flex-shrink: 0;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 8px 0;
}

.page-btn {
  padding: 8px 18px;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: var(--surface);
  color: var(--foreground);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.page-btn:hover:not(:disabled) {
  background: var(--primary-light);
  border-color: var(--primary);
  color: var(--primary);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 13px;
  color: var(--muted);
}
</style>
