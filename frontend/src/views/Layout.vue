<template>
  <div>
    <!-- Top Bar -->
    <div class="topbar">
      <div class="logo">
        <Mail :size="26" />
        <span>Mail System</span>
      </div>
      <div class="userinfo">
        <!-- User Dropdown -->
        <div class="user-dropdown" @mouseenter="showDropdown = true" @mouseleave="showDropdown = false">
          <div class="avatar">
            <img v-if="auth.user?.avatar" :src="auth.user.avatar" :alt="auth.user?.username" />
            <span v-else>{{ auth.user?.username?.charAt(0) || 'U' }}</span>
          </div>
          <span class="user-name">{{ auth.user?.username }}</span>
          <ChevronDown :size="14" />

          <div v-if="showDropdown" class="dropdown-menu">
            <div class="dropdown-header">
              <div class="dropdown-avatar" @click="openAvatarPreview">
                <img v-if="auth.user?.avatar" :src="auth.user.avatar" :alt="auth.user?.username" />
                <span v-else>{{ auth.user?.username?.charAt(0) || 'U' }}</span>
              </div>
              <div class="dropdown-info">
                <div class="dropdown-username">{{ auth.user?.username }}</div>
                <div class="dropdown-email">{{ auth.user?.email }}</div>
              </div>
            </div>
            <div class="dropdown-divider"></div>
            <div class="dropdown-item" @click="openQqEmailDialog">
              <AtSign :size="16" />
              <span>{{ auth.user?.qqEmail ? '当前绑定：' + auth.user.qqEmail : '未绑定邮箱，点击绑定~' }}</span>
            </div>
            <div class="dropdown-item" @click="openUsernameDialog">
              <UserCog :size="16" />
              <span>修改用户名</span>
            </div>
            <div class="dropdown-item" @click="openPasswordDialog">
              <Lock :size="16" />
              <span>修改密码</span>
            </div>
            <div class="dropdown-item dropdown-logout" @click="handleLogout">
              <LogOut :size="16" />
              <span>退出登录</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Layout Container -->
    <div class="container">
      <!-- Sidebar -->
      <div class="sidebar">
        <ul class="sidebar-nav">
          <li>
            <router-link to="/inbox">
              <Inbox :size="20" />
              <span>收件箱</span>
            </router-link>
          </li>
          <li>
            <router-link to="/sent">
              <Send :size="20" />
              <span>发件箱</span>
            </router-link>
          </li>
          <li>
            <router-link to="/drafts">
              <FileText :size="20" />
              <span>草稿箱</span>
            </router-link>
          </li>
          <li>
            <router-link to="/compose">
              <PenSquare :size="20" />
              <span>写邮件</span>
            </router-link>
          </li>
          <li>
            <router-link to="/contacts">
              <Users :size="20" />
              <span>联系人</span>
            </router-link>
          </li>
          <li>
            <router-link to="/search">
              <Search :size="20" />
              <span>搜索</span>
            </router-link>
          </li>
        </ul>
      </div>

      <!-- Main Content -->
      <div class="main">
        <router-view v-slot="{ Component }">
          <keep-alive include="InboxPage,SentPage,DraftsPage,ContactsPage">
            <component :is="Component" />
          </keep-alive>
        </router-view>
      </div>
    </div>

    <!-- Change Password Dialog -->
    <div v-if="showPasswordDialog" class="dialog-overlay" @click.self="closePasswordDialog">
      <div class="dialog-box">
        <div class="dialog-header">
          <h3>修改密码</h3>
          <button class="dialog-close" @click="closePasswordDialog">
            <X :size="20" />
          </button>
        </div>
        <form @submit.prevent="handleChangePassword">
          <div class="dialog-form-group">
            <label>旧密码</label>
            <input v-model="pwdForm.oldPassword" type="password" required placeholder="输入当前密码" />
          </div>
          <div class="dialog-form-group">
            <label>新密码</label>
            <input v-model="pwdForm.newPassword" type="password" required placeholder="至少6个字符" minlength="6" />
          </div>
          <div class="dialog-form-group">
            <label>确认新密码</label>
            <input v-model="pwdForm.confirmPassword" type="password" required placeholder="再次输入新密码" />
          </div>
          <div v-if="pwdError" class="dialog-error">{{ pwdError }}</div>
          <div v-if="pwdSuccess" class="dialog-success">{{ pwdSuccess }}</div>
          <div class="dialog-actions">
            <button type="button" class="btn btn-outline" @click="closePasswordDialog">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="pwdLoading">
              {{ pwdLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Change Username Dialog -->
    <div v-if="showUsernameDialog" class="dialog-overlay" @click.self="closeUsernameDialog">
      <div class="dialog-box">
        <div class="dialog-header">
          <h3>修改用户名</h3>
          <button class="dialog-close" @click="closeUsernameDialog">
            <X :size="20" />
          </button>
        </div>
        <form @submit.prevent="handleChangeUsername">
          <div class="dialog-form-group">
            <label>当前用户名</label>
            <input :value="auth.user?.username" type="text" disabled />
          </div>
          <div class="dialog-form-group">
            <label>新用户名</label>
            <input v-model="newUsername" type="text" required placeholder="3-30个字符" minlength="3" maxlength="30" />
          </div>
          <div v-if="usernameError" class="dialog-error">{{ usernameError }}</div>
          <div v-if="usernameSuccess" class="dialog-success">{{ usernameSuccess }}</div>
          <div class="dialog-actions">
            <button type="button" class="btn btn-outline" @click="closeUsernameDialog">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="usernameLoading">
              {{ usernameLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- QQ Email Binding Dialog -->
    <div v-if="showQqEmailDialog" class="dialog-overlay" @click.self="closeQqEmailDialog">
      <div class="dialog-box">
        <div class="dialog-header">
          <h3>{{ auth.user?.qqEmail ? '更改绑定邮箱' : '绑定QQ邮箱' }}</h3>
          <button class="dialog-close" @click="closeQqEmailDialog">
            <X :size="20" />
          </button>
        </div>
        <form @submit.prevent="handleBindQqEmail">
          <div v-if="auth.user?.qqEmail" class="dialog-info">
            当前绑定：{{ auth.user.qqEmail }}
          </div>
          <div class="dialog-form-group">
            <label>QQ邮箱</label>
            <input v-model="qqEmailForm.qqEmail" type="email" required placeholder="例如：123456@qq.com" />
          </div>
          <div class="dialog-form-group">
            <label>SMTP授权码</label>
            <input v-model="qqEmailForm.qqAuthCode" type="password" required placeholder="QQ邮箱设置中获取的授权码" />
          </div>
          <div class="dialog-hint">
            前往 QQ邮箱 → 设置 → 账户 → POP3/SMTP服务 → 开启并获取授权码
          </div>
          <div v-if="qqEmailError" class="dialog-error">{{ qqEmailError }}</div>
          <div v-if="qqEmailSuccess" class="dialog-success">{{ qqEmailSuccess }}</div>
          <div class="dialog-actions">
            <button type="button" class="btn btn-outline" @click="closeQqEmailDialog">取消</button>
            <button type="submit" class="btn btn-primary" :disabled="qqEmailLoading">
              {{ qqEmailLoading ? '保存中...' : '保存' }}
            </button>
          </div>
        </form>
      </div>
    </div>

    <!-- Avatar Preview / Editor Modal -->
    <div v-if="showAvatarModal" class="avatar-modal" @click.self="closeAvatarModal">
      <div class="avatar-modal-dialog">
        <!-- Preview mode -->
        <template v-if="!avatarEditing">
          <div class="avatar-preview-circle">
            <img v-if="auth.user?.avatar" :src="auth.user.avatar" />
            <span v-else>{{ auth.user?.username?.charAt(0) || 'U' }}</span>
          </div>
          <div class="avatar-preview-name">{{ auth.user?.username }}</div>
          <div class="avatar-preview-email">{{ auth.user?.email }}</div>
          <button class="avatar-change-btn" @click="triggerAvatarFileSelect">
            <Camera :size="16" />
            <span>更换头像</span>
          </button>
        </template>
        <!-- Editor mode -->
        <template v-else>
          <div class="avatar-editor-title">调整头像</div>
          <div class="avatar-crop-viewport" ref="editViewportRef">
            <img ref="editImageRef" :src="avatarEditSrc" class="avatar-edit-image"
                 :style="{ width: displayWidth + 'px', height: displayHeight + 'px' }"
                 @load="initFrameForImage"
                 draggable="false" />
            <!-- 4-piece dark overlay around the crop frame -->
            <div class="avatar-crop-overlay"
                 :style="{ top: 0, left: 0, width: '100%', height: frameY + 'px' }"></div>
            <div class="avatar-crop-overlay"
                 :style="{ top: frameY + 'px', left: 0, width: frameX + 'px', height: frameSize + 'px' }"></div>
            <div class="avatar-crop-overlay"
                 :style="{ top: frameY + 'px', left: (frameX + frameSize) + 'px', right: 0, height: frameSize + 'px' }"></div>
            <div class="avatar-crop-overlay"
                 :style="{ top: (frameY + frameSize) + 'px', left: 0, right: 0, bottom: 0 }"></div>
            <div class="avatar-crop-frame"
                 :style="{ left: frameX + 'px', top: frameY + 'px', width: frameSize + 'px', height: frameSize + 'px' }">
              <div class="crop-handle corner tl" @mousedown.stop="startFrameResize('tl', $event)"></div>
              <div class="crop-handle corner tr" @mousedown.stop="startFrameResize('tr', $event)"></div>
              <div class="crop-handle corner bl" @mousedown.stop="startFrameResize('bl', $event)"></div>
              <div class="crop-handle corner br" @mousedown.stop="startFrameResize('br', $event)"></div>
              <div class="crop-handle edge top" @mousedown.stop="startFrameResize('top', $event)"></div>
              <div class="crop-handle edge right" @mousedown.stop="startFrameResize('right', $event)"></div>
              <div class="crop-handle edge bottom" @mousedown.stop="startFrameResize('bottom', $event)"></div>
              <div class="crop-handle edge left" @mousedown.stop="startFrameResize('left', $event)"></div>
              <div class="crop-frame-body"
                   :style="{ cursor: frameDrag ? 'grabbing' : 'grab' }"
                   @mousedown.stop="startFrameDrag($event)"></div>
            </div>
          </div>
          <div class="avatar-editor-hint">拖动截取框调整位置 · 拖拽边框调整大小</div>
          <div class="avatar-editor-actions">
            <button class="btn btn-outline" @click="cancelAvatarEdit">取消</button>
            <button class="btn btn-primary" @click="cropAndUploadAvatar">确认上传</button>
          </div>
        </template>
      </div>
    </div>

    <input ref="avatarFileInputRef" type="file" accept="image/*" @change="onAvatarFileSelected" style="display: none" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useMailStore } from '../stores/mail'
import api from '../api'
import { Mail, Inbox, Send, FileText, PenSquare, Users, Search, LogOut, ChevronDown, Lock, X, UserCog, Camera, AtSign } from 'lucide-vue-next'

const mailStore = useMailStore()
onMounted(() => {
  mailStore.preloadAll()
})

const router = useRouter()
const auth = useAuthStore()

const showDropdown = ref(false)
const showPasswordDialog = ref(false)
const showUsernameDialog = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdError = ref('')
const pwdSuccess = ref('')
const pwdLoading = ref(false)
const newUsername = ref('')
const usernameError = ref('')
const usernameSuccess = ref('')
const usernameLoading = ref(false)

// QQ email binding state
const showQqEmailDialog = ref(false)
const qqEmailForm = ref({ qqEmail: '', qqAuthCode: '' })
const qqEmailError = ref('')
const qqEmailSuccess = ref('')
const qqEmailLoading = ref(false)

// Avatar modal state
const showAvatarModal = ref(false)
const avatarEditing = ref(false)
const avatarEditSrc = ref('')
const avatarZoom = ref(1)
const avatarFileInputRef = ref(null)
const editImageRef = ref(null)
const editViewportRef = ref(null)

// Crop frame state (position relative to the displayed image)
const frameX = ref(0)
const frameY = ref(0)
const frameSize = ref(200)

// Frame interaction state
const frameDrag = ref(false)
const frameDragStart = ref({ x: 0, y: 0, fx: 0, fy: 0 })
const frameResize = ref(null)
const frameResizeStart = ref({ x: 0, y: 0, fx: 0, fy: 0, fs: 0 })

// Computed: displayed image dimensions (fit within viewport, scaled by zoom)
const displayWidth = computed(() => {
  const img = editImageRef.value
  if (!img || !img.naturalWidth) return 400
  const maxW = 400, maxH = 400
  const scale = Math.min(maxW / img.naturalWidth, maxH / img.naturalHeight) * avatarZoom.value
  return Math.min(maxW, img.naturalWidth * scale)
})

const displayHeight = computed(() => {
  const img = editImageRef.value
  if (!img || !img.naturalHeight) return 400
  const maxW = 400, maxH = 400
  const scale = Math.min(maxW / img.naturalWidth, maxH / img.naturalHeight) * avatarZoom.value
  return Math.min(maxH, img.naturalHeight * scale)
})

function handleLogout() {
  showDropdown.value = false
  auth.logout()
  router.push('/login')
}

function openPasswordDialog() {
  showDropdown.value = false
  showPasswordDialog.value = true
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdError.value = ''
  pwdSuccess.value = ''
}

function closePasswordDialog() {
  showPasswordDialog.value = false
}

async function handleChangePassword() {
  pwdError.value = ''
  pwdSuccess.value = ''

  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    pwdError.value = '两次输入的新密码不一致'
    return
  }
  if (pwdForm.value.newPassword.length < 6) {
    pwdError.value = '新密码至少6个字符'
    return
  }

  pwdLoading.value = true
  try {
    const res = await api.put('/auth/password', {
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    if (res.code === 0) {
      pwdSuccess.value = '密码修改成功！'
      setTimeout(() => closePasswordDialog(), 1500)
    } else {
      pwdError.value = res.message
    }
  } catch (e) {
    pwdError.value = e.response?.data?.message || '修改失败'
  } finally {
    pwdLoading.value = false
  }
}

function openUsernameDialog() {
  showDropdown.value = false
  showUsernameDialog.value = true
  newUsername.value = auth.user?.username || ''
  usernameError.value = ''
  usernameSuccess.value = ''
}

function closeUsernameDialog() {
  showUsernameDialog.value = false
}

async function handleChangeUsername() {
  usernameError.value = ''
  usernameSuccess.value = ''

  if (newUsername.value.trim().length < 3) {
    usernameError.value = '用户名至少3个字符'
    return
  }

  usernameLoading.value = true
  try {
    const res = await api.put('/auth/username', { username: newUsername.value.trim() })
    if (res.code === 0) {
      usernameSuccess.value = '用户名修改成功！'
      // 更新本地存储的用户信息
      auth.setAuth(res.data.token, {
        id: res.data.id,
        username: res.data.username,
        email: res.data.email,
        avatar: res.data.avatar,
        qqEmail: res.data.qqEmail
      })
      setTimeout(() => closeUsernameDialog(), 1500)
    } else {
      usernameError.value = res.message
    }
  } catch (e) {
    usernameError.value = e.response?.data?.message || '修改失败'
  } finally {
    usernameLoading.value = false
  }
}

function initFrameForImage() {
  const img = editImageRef.value
  if (!img || !img.naturalWidth) return
  const maxW = 400, maxH = 400
  const baseScale = Math.min(maxW / img.naturalWidth, maxH / img.naturalHeight)
  const dw = img.naturalWidth * baseScale * avatarZoom.value
  const dh = img.naturalHeight * baseScale * avatarZoom.value
  const size = Math.min(dw, dh) * 0.6
  const offsetX = (400 - dw) / 2
  const offsetY = (400 - dh) / 2
  frameSize.value = Math.max(40, size)
  frameX.value = offsetX + (dw - size) / 2
  frameY.value = offsetY + (dh - size) / 2
}

function clampFrame() {
  const img = editImageRef.value
  if (!img) return
  const maxW = 400, maxH = 400
  const baseScale = Math.min(maxW / img.naturalWidth, maxH / img.naturalHeight)
  const dw = Math.min(maxW, img.naturalWidth * baseScale * avatarZoom.value)
  const dh = Math.min(maxH, img.naturalHeight * baseScale * avatarZoom.value)
  const offsetX = (maxW - dw) / 2
  const offsetY = (maxH - dh) / 2
  const imgRight = offsetX + dw
  const imgBottom = offsetY + dh
  frameSize.value = Math.max(40, Math.min(frameSize.value, dw, dh))
  frameX.value = Math.max(offsetX, Math.min(imgRight - frameSize.value, frameX.value))
  frameY.value = Math.max(offsetY, Math.min(imgBottom - frameSize.value, frameY.value))
}

function handleAvatarUpload(event) {
  const file = event.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { alert('请选择图片文件'); return }
  if (file.size > 2 * 1024 * 1024) { alert('头像大小不能超过 2MB'); return }
  const reader = new FileReader()
  reader.onload = (e) => {
    avatarEditSrc.value = e.target.result
    avatarEditing.value = true
    avatarZoom.value = 1
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

function openQqEmailDialog() {
  showDropdown.value = false
  showQqEmailDialog.value = true
  qqEmailForm.value = { qqEmail: '', qqAuthCode: '' }
  qqEmailError.value = ''
  qqEmailSuccess.value = ''
}

function closeQqEmailDialog() {
  showQqEmailDialog.value = false
}

async function handleBindQqEmail() {
  qqEmailError.value = ''
  qqEmailSuccess.value = ''

  if (!qqEmailForm.value.qqEmail || !qqEmailForm.value.qqEmail.includes('@')) {
    qqEmailError.value = '请输入有效的邮箱地址'
    return
  }
  if (!qqEmailForm.value.qqAuthCode || qqEmailForm.value.qqAuthCode.length < 10) {
    qqEmailError.value = '请输入有效的授权码'
    return
  }

  qqEmailLoading.value = true
  try {
    const res = await api.put('/auth/qq-email', {
      qqEmail: qqEmailForm.value.qqEmail.trim(),
      qqAuthCode: qqEmailForm.value.qqAuthCode.trim()
    })
    if (res.code === 0) {
      qqEmailSuccess.value = '绑定成功！'
      auth.user.qqEmail = qqEmailForm.value.qqEmail.trim()
      auth.syncUser()
      setTimeout(() => closeQqEmailDialog(), 1500)
    } else {
      qqEmailError.value = res.message
    }
  } catch (e) {
    qqEmailError.value = e.response?.data?.message || '绑定失败'
  } finally {
    qqEmailLoading.value = false
  }
}

function openAvatarPreview() {
  showDropdown.value = false
  showAvatarModal.value = true
  avatarEditing.value = false
}

function triggerAvatarFileSelect() {
  avatarFileInputRef.value?.click()
}

function onAvatarFileSelected(event) {
  const file = event.target.files[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { alert('请选择图片文件'); return }
  if (file.size > 2 * 1024 * 1024) { alert('头像大小不能超过 2MB'); return }
  const reader = new FileReader()
  reader.onload = (e) => {
    avatarEditSrc.value = e.target.result
    avatarEditing.value = true
    avatarZoom.value = 1
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

function startFrameDrag(e) {
  frameDrag.value = true
  frameDragStart.value = { x: e.clientX, y: e.clientY, fx: frameX.value, fy: frameY.value }
  document.addEventListener('mousemove', onFrameDragMove)
  document.addEventListener('mouseup', onFrameDragEnd)
}

function onFrameDragMove(e) {
  if (!frameDrag.value) return
  frameX.value = frameDragStart.value.fx + (e.clientX - frameDragStart.value.x)
  frameY.value = frameDragStart.value.fy + (e.clientY - frameDragStart.value.y)
  clampFrame()
}

function onFrameDragEnd() {
  frameDrag.value = false
  document.removeEventListener('mousemove', onFrameDragMove)
  document.removeEventListener('mouseup', onFrameDragEnd)
}

function startFrameResize(handle, e) {
  frameResize.value = handle
  frameResizeStart.value = {
    x: e.clientX, y: e.clientY,
    fx: frameX.value, fy: frameY.value, fs: frameSize.value
  }
  document.addEventListener('mousemove', onFrameResizeMove)
  document.addEventListener('mouseup', onFrameResizeEnd)
}

function onFrameResizeMove(e) {
  if (!frameResize.value) return
  const dx = e.clientX - frameResizeStart.value.x
  const dy = e.clientY - frameResizeStart.value.y
  const h = frameResize.value
  const s = frameResizeStart.value
  let newX = s.fx, newY = s.fy, newSize = s.fs
  if (h === 'tl') {
    const d = Math.min(dx, dy)
    newX = s.fx + d; newY = s.fy + d; newSize = s.fs - d
  } else if (h === 'tr') {
    const d = Math.max(dx, -dy)
    newY = s.fy - d; newSize = s.fs + d
  } else if (h === 'bl') {
    const d = Math.min(-dx, dy)
    newX = s.fx + d; newSize = s.fs - d
  } else if (h === 'br') {
    const d = Math.max(dx, dy)
    newSize = s.fs + d
  } else if (h === 'left') {
    newX = s.fx + dx; newSize = s.fs - dx
  } else if (h === 'right') {
    newSize = s.fs + dx
  } else if (h === 'top') {
    newY = s.fy + dy; newSize = s.fs - dy
  } else if (h === 'bottom') {
    newSize = s.fs + dy
  }
  if (newSize < 40) return
  frameX.value = newX
  frameY.value = newY
  frameSize.value = newSize
  clampFrame()
}

function onFrameResizeEnd() {
  frameResize.value = null
  document.removeEventListener('mousemove', onFrameResizeMove)
  document.removeEventListener('mouseup', onFrameResizeEnd)
}

async function cropAndUploadAvatar() {
  const img = editImageRef.value
  if (!img || !img.naturalWidth) return

  const maxW = 400, maxH = 400
  const baseScale = Math.min(maxW / img.naturalWidth, maxH / img.naturalHeight) * avatarZoom.value
  const dw = Math.min(maxW, img.naturalWidth * baseScale)
  const dh = Math.min(maxH, img.naturalHeight * baseScale)
  const offsetX = (maxW - dw) / 2
  const offsetY = (maxH - dh) / 2

  const scaleX = img.naturalWidth / dw
  const scaleY = img.naturalHeight / dh

  const srcX = (frameX.value - offsetX) * scaleX
  const srcY = (frameY.value - offsetY) * scaleY
  const srcW = frameSize.value * scaleX
  const srcH = frameSize.value * scaleY

  const outputSize = 512
  const canvas = document.createElement('canvas')
  canvas.width = outputSize
  canvas.height = outputSize
  const ctx = canvas.getContext('2d')
  ctx.drawImage(img, srcX, srcY, srcW, srcH, 0, 0, outputSize, outputSize)

  canvas.toBlob(async (blob) => {
    if (!blob) return
    try {
      const formData = new FormData()
      formData.append('file', blob, 'avatar.png')
      const res = await api.post('/auth/avatar', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      })
      if (res.code === 0) {
        auth.user.avatar = res.data.avatarUrl
        auth.syncUser()
        showAvatarModal.value = false
        avatarEditing.value = false
      } else {
        alert(res.message || '上传失败')
      }
    } catch (e) {
      alert(e.response?.data?.message || '上传失败')
    }
  }, 'image/png')
}

function cancelAvatarEdit() {
  avatarEditing.value = false
}

function closeAvatarModal() {
  showAvatarModal.value = false
  avatarEditing.value = false
}
</script>

<style scoped>
.userinfo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius);
  transition: var(--transition);
  position: relative;
}

.user-dropdown:hover {
  background: var(--background);
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  right: 0;
  width: 260px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  z-index: 1000;
  overflow: hidden;
  padding-top: 4px;
}

.dropdown-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
}

.dropdown-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  overflow: hidden;
  flex-shrink: 0;
  cursor: pointer;
  transition: var(--transition);
}

.dropdown-avatar:hover {
  opacity: 0.85;
  transform: scale(1.05);
}

.dropdown-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Avatar Preview Modal */
.avatar-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 3000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.avatar-modal-dialog {
  background: var(--surface);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  padding: 36px;
  min-width: 340px;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  animation: scaleIn 0.2s ease;
}

@keyframes scaleIn {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}

.avatar-preview-circle {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  background: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 52px;
  font-weight: 700;
  text-transform: uppercase;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(37, 99, 235, 0.25);
}

.avatar-preview-circle img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-preview-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--foreground);
}

.avatar-preview-email {
  font-size: 14px;
  color: var(--muted);
  margin-top: -8px;
}

.avatar-change-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  color: var(--foreground);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  font-family: inherit;
  margin-top: 4px;
}

.avatar-change-btn:hover {
  background: var(--primary);
  color: white;
  border-color: var(--primary);
}

/* Avatar Editor */
.avatar-editor-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--foreground);
}

.avatar-crop-viewport {
  width: 400px;
  height: 400px;
  max-width: calc(90vw - 72px);
  max-height: calc(90vw - 72px);
  border-radius: var(--radius);
  overflow: hidden;
  background: #f0f0f0;
  position: relative;
  user-select: none;
}

.avatar-edit-image {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  object-fit: contain;
  pointer-events: none;
  z-index: 1;
}

/* Dark overlay pieces (positioned via inline styles) */
.avatar-crop-overlay {
  position: absolute;
  background: rgba(0, 0, 0, 0.55);
  z-index: 2;
  pointer-events: none;
}

/* The crop frame (square, draggable, resizable) */
.avatar-crop-frame {
  position: absolute;
  z-index: 3;
  border: 2px solid white;
  box-shadow: 0 0 0 1px rgba(0, 0, 0, 0.3);
  box-sizing: border-box;
}

.crop-frame-body {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
}

/* Corner handles */
.crop-handle.corner {
  position: absolute;
  width: 14px;
  height: 14px;
  background: white;
  border: 2px solid var(--primary);
  border-radius: 2px;
  z-index: 4;
}

.crop-handle.corner.tl { top: -7px; left: -7px; cursor: nwse-resize; }
.crop-handle.corner.tr { top: -7px; right: -7px; cursor: nesw-resize; }
.crop-handle.corner.bl { bottom: -7px; left: -7px; cursor: nesw-resize; }
.crop-handle.corner.br { bottom: -7px; right: -7px; cursor: nwse-resize; }

/* Edge handles */
.crop-handle.edge {
  position: absolute;
  z-index: 4;
}

.crop-handle.edge.top {
  top: -4px;
  left: 14px;
  right: 14px;
  height: 8px;
  cursor: ns-resize;
}

.crop-handle.edge.bottom {
  bottom: -4px;
  left: 14px;
  right: 14px;
  height: 8px;
  cursor: ns-resize;
}

.crop-handle.edge.left {
  left: -4px;
  top: 14px;
  bottom: 14px;
  width: 8px;
  cursor: ew-resize;
}

.crop-handle.edge.right {
  right: -4px;
  top: 14px;
  bottom: 14px;
  width: 8px;
  cursor: ew-resize;
}

.avatar-editor-hint {
  font-size: 12px;
  color: var(--muted);
}

.avatar-editor-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.avatar-editor-actions .btn-primary {
  padding: 10px 32px;
}

.dropdown-info {
  flex: 1;
  min-width: 0;
}

.dropdown-username {
  font-weight: 600;
  font-size: 14px;
  color: var(--foreground);
}

.dropdown-email {
  font-size: 12px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dropdown-divider {
  height: 1px;
  background: var(--border);
  margin: 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  cursor: pointer;
  font-size: 14px;
  color: var(--foreground);
  transition: var(--transition);
}

.dropdown-item:hover {
  background: var(--background);
}

.dropdown-logout {
  color: var(--destructive);
}

.dropdown-logout:hover {
  background: var(--destructive-light, #FEE2E2);
}

/* Dialog */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.dialog-box {
  background: var(--surface);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  width: 400px;
  max-width: 90vw;
  padding: 24px;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.dialog-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--foreground);
}

.dialog-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  color: var(--muted);
  transition: var(--transition);
}

.dialog-close:hover {
  background: var(--background);
  color: var(--foreground);
}

.dialog-form-group {
  margin-bottom: 16px;
}

.dialog-form-group label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--foreground);
  margin-bottom: 6px;
}

.dialog-form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius);
  font-size: 14px;
  transition: var(--transition);
  box-sizing: border-box;
}

.dialog-form-group input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.dialog-form-group input:disabled {
  background: var(--background);
  color: var(--muted);
  cursor: not-allowed;
}

.dialog-error {
  background: var(--destructive-light, #FEE2E2);
  color: #991B1B;
  padding: 10px 14px;
  border-radius: var(--radius);
  font-size: 13px;
  margin-bottom: 12px;
}

.dialog-success {
  background: var(--accent-light, #D1FAE5);
  color: #065F46;
  padding: 10px 14px;
  border-radius: var(--radius);
  font-size: 13px;
  margin-bottom: 12px;
}

.dialog-info {
  background: var(--primary-light, #DBEAFE);
  color: #1E40AF;
  padding: 10px 14px;
  border-radius: var(--radius);
  font-size: 13px;
  margin-bottom: 12px;
  word-break: break-all;
}

.dialog-hint {
  font-size: 12px;
  color: var(--muted);
  margin-top: -8px;
  margin-bottom: 12px;
  line-height: 1.5;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}
</style>
