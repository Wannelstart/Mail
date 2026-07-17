<template>
  <div class="contacts-page">
    <!-- Page Header -->
    <div class="page-header">
      <h1 class="page-title">联系人管理</h1>
      <button class="btn-add" @click="showAddDialog = true">
        <Plus :size="16" />
        添加联系人
      </button>
    </div>

    <!-- Search and Filter Bar -->
    <div class="filter-card">
      <div class="filter-row">
        <div class="search-input-wrap">
          <Search :size="18" class="search-icon" />
          <input
            v-model="searchKeyword"
            @keyup.enter="searchContacts"
            placeholder="搜索联系人..."
            class="search-input"
          />
        </div>
        <button class="btn-search" @click="searchContacts">
          <Search :size="16" />
          搜索
        </button>
        <button class="btn-reset" @click="searchKeyword=''; loadContacts()">
          <X :size="16" />
          清除
        </button>
        <div class="filter-divider"></div>
        <select v-model="filterGroup" @change="loadContacts" class="group-select">
          <option value="">全部分组</option>
          <option v-for="g in groups" :key="g" :value="g">{{ g }}</option>
        </select>
      </div>
    </div>

    <!-- Contacts Table -->
    <div class="contacts-card">
      <table class="contacts-table">
        <thead>
          <tr>
            <th width="200">联系人</th>
            <th>邮箱</th>
            <th width="120">昵称</th>
            <th width="100">分组</th>
            <th width="130">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in contacts" :key="c.id" class="contact-row">
            <td>
              <div class="contact-cell">
                <div class="avatar">
                  {{ getInitial(c.username || c.email) }}
                </div>
                <span class="contact-name">{{ c.username }}</span>
              </div>
            </td>
            <td>
              <span class="email-text">{{ c.email }}</span>
            </td>
            <td>
              <span class="nickname-text">{{ c.nickname || '-' }}</span>
            </td>
            <td>
              <span v-if="c.groupName" class="group-pill" :class="getGroupClass(c.groupName)">
                {{ c.groupName }}
              </span>
              <span v-else class="text-muted">-</span>
            </td>
            <td>
              <div class="action-btns">
                <button class="btn-action btn-edit" @click="editContact(c)" title="编辑">
                  <Edit :size="14" />
                </button>
                <button class="btn-action btn-delete" @click="deleteContact(c.id)" title="删除">
                  <Trash2 :size="14" />
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="contacts.length === 0">
            <td colspan="5" class="empty-state">
              <div class="empty-inner">
                <Users :size="40" color="var(--muted)" />
                <p>暂无联系人</p>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Add/Edit Dialog -->
    <Teleport to="body">
      <div v-if="showAddDialog || showEditDialog" class="dialog-overlay" @click.self="closeDialog">
        <div class="dialog-card">
          <div class="dialog-header">
            <div class="dialog-title-wrap">
              <UserPlus :size="20" class="dialog-title-icon" />
              <h3 class="dialog-title">
                {{ showEditDialog ? '编辑联系人' : '添加联系人' }}
              </h3>
            </div>
            <button class="dialog-close" @click="closeDialog">
              <X :size="18" />
            </button>
          </div>
          <form @submit.prevent="showEditDialog ? handleUpdate() : handleAdd()" class="dialog-form">
            <div class="form-group">
              <label class="form-label">
                <User :size="14" />
                邮箱（用户标识）
              </label>
              <input
                v-model="form.email"
                type="email"
                required
                :disabled="showEditDialog"
                class="form-input"
                placeholder="user@example.com"
              />
            </div>
            <div class="form-group">
              <label class="form-label">
                <Edit :size="14" />
                昵称
              </label>
              <input
                v-model="form.nickname"
                type="text"
                class="form-input"
                placeholder="可选"
              />
            </div>
            <div class="form-group">
              <label class="form-label">
                <Users :size="14" />
                分组
              </label>
              <select v-model="form.groupName" class="form-input">
                <option value="">默认分组</option>
                <option value="同事">同事</option>
                <option value="朋友">朋友</option>
                <option value="家人">家人</option>
                <option value="客户">客户</option>
              </select>
            </div>
            <p v-if="dialogError" class="dialog-error">{{ dialogError }}</p>
            <div class="dialog-actions">
              <button type="button" class="btn-cancel" @click="closeDialog">取消</button>
              <button type="submit" class="btn-submit">
                {{ showEditDialog ? '保存' : '添加' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed, onActivated } from 'vue'
import api from '../api'
import { Plus, Search, Edit, Trash2, UserPlus, Users, User, X } from 'lucide-vue-next'
import { useMailStore } from '../stores/mail'

defineOptions({ name: 'ContactsPage' })

const mailStore = useMailStore()
const contacts = ref([])
const searchKeyword = ref('')
const filterGroup = ref('')
const showAddDialog = ref(false)
const showEditDialog = ref(false)
const dialogError = ref('')
const form = ref({ email: '', nickname: '', groupName: '' })
const editId = ref(null)

const groups = computed(() => {
  const set = new Set(contacts.value.map(c => c.groupName).filter(Boolean))
  return Array.from(set)
})

const groupColors = {
  '同事': 'group-blue',
  '朋友': 'group-green',
  '家人': 'group-purple',
  '客户': 'group-orange',
}

function getGroupClass(name) {
  return groupColors[name] || 'group-default'
}

const contactsLoaded = ref(false)

async function loadContacts() {
  // 首次加载优先用预取缓存
  if (!contactsLoaded.value && mailStore.contacts) {
    contacts.value = mailStore.contacts
    contactsLoaded.value = true
    mailStore.contacts = null
  }
  const params = {}
  if (filterGroup.value) params.groupName = filterGroup.value
  const res = await api.get('/contact', { params })
  if (res.code === 0) contacts.value = res.data
  contactsLoaded.value = true
}

async function searchContacts() {
  if (!searchKeyword.value.trim()) return loadContacts()
  const res = await api.get('/contact/search', { params: { keyword: searchKeyword.value } })
  if (res.code === 0) contacts.value = res.data
}

function editContact(c) {
  editId.value = c.id
  form.value = { email: c.email, nickname: c.nickname, groupName: c.groupName }
  showEditDialog.value = true
}

async function handleAdd() {
  dialogError.value = ''
  try {
    const res = await api.post('/contact', form.value)
    if (res.code === 0) {
      closeDialog()
      loadContacts()
    } else {
      dialogError.value = res.message
    }
  } catch (e) {
    dialogError.value = e.response?.data?.message || '添加失败'
  }
}

async function handleUpdate() {
  dialogError.value = ''
  try {
    const res = await api.put(`/contact/${editId.value}`, form.value)
    if (res.code === 0) {
      closeDialog()
      loadContacts()
    } else {
      dialogError.value = res.message
    }
  } catch (e) {
    dialogError.value = e.response?.data?.message || '更新失败'
  }
}

async function deleteContact(id) {
  if (!confirm('确定删除该联系人？')) return
  await api.delete(`/contact/${id}`)
  loadContacts()
}

function closeDialog() {
  showAddDialog.value = false
  showEditDialog.value = false
  form.value = { email: '', nickname: '', groupName: '' }
  dialogError.value = ''
  editId.value = null
}

function getInitial(name) {
  if (!name) return '?'
  return name.charAt(0).toUpperCase()
}

onActivated(loadContacts)
</script>

<style scoped>
.contacts-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--foreground);
}

.btn-add {
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

.btn-add:hover {
  background: var(--primary-hover);
}

.filter-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  padding: 14px 20px;
  margin-bottom: 16px;
}

.filter-row {
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

.btn-search {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: var(--primary);
  color: white;
  border: none;
  border-radius: var(--radius);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-search:hover {
  background: var(--primary-hover);
}

.btn-reset {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 10px 14px;
  background: var(--background);
  color: var(--muted);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 13px;
  cursor: pointer;
  transition: var(--transition);
}

.btn-reset:hover {
  background: #FEF2F2;
  border-color: var(--destructive);
  color: var(--destructive);
}

.filter-divider {
  width: 1px;
  height: 28px;
  background: var(--border);
}

.group-select {
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

.group-select:focus {
  border-color: var(--primary);
}

.contacts-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.contacts-table {
  width: 100%;
  border-collapse: collapse;
}

.contacts-table th {
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

.contacts-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  vertical-align: middle;
}

.contact-row {
  transition: background 0.15s ease;
}

.contact-row:hover {
  background: var(--background);
}

.contact-row:last-child td {
  border-bottom: none;
}

.contact-cell {
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

.contact-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--foreground);
}

.email-text {
  font-size: 14px;
  color: var(--foreground);
}

.nickname-text {
  font-size: 14px;
  color: var(--foreground);
}

.text-muted {
  color: var(--muted);
  font-size: 14px;
}

.group-pill {
  padding: 2px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}

.group-blue {
  background: #DBEAFE;
  color: #1D4ED8;
}

.group-green {
  background: #D1FAE5;
  color: #059669;
}

.group-purple {
  background: #EDE9FE;
  color: #7C3AED;
}

.group-orange {
  background: #FEF3C7;
  color: #D97706;
}

.group-default {
  background: var(--background);
  color: var(--muted);
}

.action-btns {
  display: flex;
  gap: 6px;
}

.btn-action {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--border);
  border-radius: 6px;
  background: var(--surface);
  color: var(--muted);
  cursor: pointer;
  transition: var(--transition);
}

.btn-action:hover {
  background: var(--background);
  color: var(--foreground);
}

.btn-edit:hover {
  background: var(--primary-light);
  border-color: var(--primary);
  color: var(--primary);
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

/* Dialog Styles */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(15, 23, 42, 0.5);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.dialog-card {
  background: var(--surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  width: 440px;
  max-width: 90vw;
  overflow: hidden;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px 16px;
  border-bottom: 1px solid var(--border);
}

.dialog-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.dialog-title-icon {
  color: var(--primary);
}

.dialog-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--foreground);
}

.dialog-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--radius);
  background: transparent;
  color: var(--muted);
  cursor: pointer;
  transition: var(--transition);
}

.dialog-close:hover {
  background: var(--background);
  color: var(--foreground);
}

.dialog-form {
  padding: 20px 24px 24px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--foreground);
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  font-size: 14px;
  color: var(--foreground);
  background: var(--background);
  transition: var(--transition);
  outline: none;
}

.form-input:focus {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.form-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.dialog-error {
  color: var(--destructive);
  font-size: 13px;
  text-align: center;
  margin-bottom: 12px;
}

.dialog-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
}

.btn-cancel {
  padding: 10px 20px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  color: var(--foreground);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-cancel:hover {
  background: var(--background);
}

.btn-submit {
  padding: 10px 24px;
  border: none;
  border-radius: var(--radius);
  background: var(--primary);
  color: white;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.btn-submit:hover {
  background: var(--primary-hover);
}
</style>
