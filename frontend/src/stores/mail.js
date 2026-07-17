import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'

export const useMailStore = defineStore('mail', () => {
  const inbox = ref(null)
  const sent = ref(null)
  const drafts = ref(null)
  const contacts = ref(null)

  // 预加载所有模块数据
  async function preloadAll() {
    const [inboxRes, sentRes, draftsRes, contactsRes] = await Promise.allSettled([
      api.get('/mail/inbox', { params: { page: 1, size: 20 } }),
      api.get('/mail/sent', { params: { page: 1, size: 20 } }),
      api.get('/mail/drafts', { params: { page: 1, size: 20 } }),
      api.get('/contact')
    ])
    if (inboxRes.status === 'fulfilled' && inboxRes.value.code === 0) inbox.value = inboxRes.value.data
    if (sentRes.status === 'fulfilled' && sentRes.value.code === 0) sent.value = sentRes.value.data
    if (draftsRes.status === 'fulfilled' && draftsRes.value.code === 0) drafts.value = draftsRes.value.data
    if (contactsRes.status === 'fulfilled' && contactsRes.value.code === 0) contacts.value = contactsRes.value.data
  }

  return { inbox, sent, drafts, contacts, preloadAll }
})
