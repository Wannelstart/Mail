<template>
  <div class="search-page">
    <!-- Page Header -->
    <div class="page-header">
      <h1 class="page-title">搜索邮件</h1>
    </div>

    <!-- Search Form Card -->
    <div class="search-form-card">
      <div class="search-form-row">
        <div class="search-input-wrap">
          <Search :size="20" class="search-icon" />
          <input
            v-model="keyword"
            @keyup.enter="doSearch"
            placeholder="输入关键词搜索邮件..."
            class="search-input search-input-large"
          />
        </div>
      </div>
      <div class="search-options-row">
        <div class="search-tabs">
          <button
            v-for="opt in boxOptions"
            :key="opt.value"
            class="tab-btn"
            :class="{ active: box === opt.value }"
            @click="box = opt.value"
          >
            {{ opt.label }}
          </button>
        </div>
        <div class="search-fields">
          <select v-model="searchIn" class="search-select">
            <option value="all">搜索全部</option>
            <option value="subject">主题</option>
            <option value="content">内容</option>
          </select>
          <button class="btn-search" @click="doSearch">
            <Search :size="16" />
            搜索
          </button>
          <button
            v-if="keyword || searched"
            class="btn-clear"
            @click="clearSearch"
          >
            <X :size="16" />
            清除
          </button>
        </div>
      </div>
    </div>

    <!-- Results Count -->
    <div v-if="searched" class="results-info">
      <span>找到 <strong>{{ total }}</strong> 封邮件</span>
    </div>

    <!-- Results Table -->
    <div class="mail-card" v-if="mails.length > 0">
      <table class="mail-table">
        <thead>
          <tr>
            <th width="160">发件人</th>
            <th>主题</th>
            <th width="140">时间</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in mails" :key="m.id" class="mail-row">
            <td>
              <div class="sender-cell">
                <div class="avatar">
                  {{ getInitial(m.remote ? m.remoteSenderEmail : m.senderEmail) }}
                </div>
                <span class="sender-name">
                  {{ m.remote ? m.remoteSenderEmail : m.senderEmail }}
                </span>
              </div>
            </td>
            <td>
              <router-link :to="`/mail/${m.id}`" class="subject-link">
                {{ m.subject || '(无主题)' }}
              </router-link>
              <Paperclip v-if="m.hasAttachments" :size="14" class="attach-icon" />
            </td>
            <td>
              <span class="time-text">{{ formatTime(m.sentAt) }}</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Empty State -->
    <div v-if="searched && mails.length === 0" class="empty-card">
      <div class="empty-inner">
        <Search :size="48" class="empty-icon" />
        <p class="empty-title">未找到相关邮件</p>
        <p class="empty-sub">请尝试其他关键词或调整搜索范围</p>
      </div>
    </div>

    <!-- Pagination -->
    <div class="pagination" v-if="total > size">
      <button
        class="page-btn"
        :disabled="page <= 1"
        @click="page--; doSearch()"
      >
        上一页
      </button>
      <span class="page-info">第 {{ page }} 页 / 共 {{ totalPages }} 页</span>
      <button
        class="page-btn"
        :disabled="page >= totalPages"
        @click="page++; doSearch()"
      >
        下一页
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import api from '../api'
import { Search, X, Paperclip } from 'lucide-vue-next'

const keyword = ref('')
const box = ref('all')
const searchIn = ref('all')
const mails = ref([])
const page = ref(1)
const total = ref(0)
const size = 20
const searched = ref(false)
const totalPages = computed(() => Math.ceil(total.value / size) || 1)

const boxOptions = [
  { label: '全部邮箱', value: 'all' },
  { label: '收件箱', value: 'inbox' },
  { label: '发件箱', value: 'sent' },
  { label: '草稿箱', value: 'drafts' },
]

async function doSearch() {
  if (!keyword.value.trim()) return
  searched.value = true
  const res = await api.get('/search', { params: { keyword: keyword.value, box: box.value, searchIn: searchIn.value, page: page.value, size } })
  if (res.code === 0) {
    mails.value = res.data.list
    total.value = res.data.total
  }
}

function clearSearch() {
  keyword.value = ''
  searched.value = false
  mails.value = []
  total.value = 0
  page.value = 1
}

function formatTime(t) {
  if (!t) return ''
  return new Date(t).toLocaleString('zh-CN')
}

function getInitial(email) {
  if (!email) return '?'
  return email.charAt(0).toUpperCase()
}
</script>

<style scoped>
.search-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--foreground);
}

.search-form-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 20px 24px;
  margin-bottom: 16px;
}

.search-form-row {
  margin-bottom: 16px;
}

.search-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 14px;
  color: var(--muted);
  pointer-events: none;
}

.search-input {
  width: 100%;
  padding: 10px 12px 10px 44px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 14px;
  color: var(--foreground);
  background: var(--background);
  transition: var(--transition);
  outline: none;
}

.search-input-large {
  padding: 14px 14px 14px 48px;
  font-size: 16px;
  border-radius: var(--radius-lg);
}

.search-input-large + .search-icon,
.search-input-wrap:has(.search-input-large) .search-icon {
  left: 16px;
}

.search-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.search-options-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.search-tabs {
  display: flex;
  gap: 4px;
  background: var(--background);
  border-radius: var(--radius);
  padding: 3px;
}

.tab-btn {
  padding: 7px 16px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--muted);
  background: transparent;
  cursor: pointer;
  transition: var(--transition);
}

.tab-btn:hover {
  color: var(--foreground);
}

.tab-btn.active {
  background: var(--surface);
  color: var(--primary);
  box-shadow: var(--shadow-sm);
}

.search-fields {
  display: flex;
  gap: 8px;
  align-items: center;
}

.search-select {
  padding: 9px 12px;
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
  padding: 9px 20px;
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

.btn-clear {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 9px 14px;
  background: var(--background);
  color: var(--muted);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-clear:hover {
  background: #FEF2F2;
  border-color: var(--destructive);
  color: var(--destructive);
}

.results-info {
  font-size: 14px;
  color: var(--muted);
  margin-bottom: 12px;
  padding-left: 4px;
}

.results-info strong {
  color: var(--primary);
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

.empty-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 60px 20px;
  margin-bottom: 16px;
}

.empty-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.empty-icon {
  color: var(--border);
  margin-bottom: 8px;
}

.empty-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--foreground);
}

.empty-sub {
  font-size: 14px;
  color: var(--muted);
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
