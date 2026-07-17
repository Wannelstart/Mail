import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import InboxPage from '../views/Inbox.vue'
import SentPage from '../views/Sent.vue'
import DraftsPage from '../views/Drafts.vue'
import ContactsPage from '../views/Contacts.vue'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue') },
  {
    path: '/', name: 'Layout', component: () => import('../views/Layout.vue'),
    meta: { requiresAuth: true },
    children: [
      { path: '', redirect: '/inbox' },
      { path: 'inbox', name: 'Inbox', component: InboxPage },
      { path: 'sent', name: 'Sent', component: SentPage },
      { path: 'drafts', name: 'Drafts', component: DraftsPage },
      { path: 'compose', name: 'Compose', component: () => import('../views/Compose.vue') },
      { path: 'mail/:id', name: 'ViewMail', component: () => import('../views/ViewMail.vue') },
      { path: 'search', name: 'Search', component: () => import('../views/Search.vue') },
      { path: 'contacts', name: 'Contacts', component: ContactsPage },
    ]
  }
]

export const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})
