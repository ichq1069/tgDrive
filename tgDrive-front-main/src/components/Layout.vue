<!-- src/components/Layout.vue -->
<template>
  <el-container class="layout-container">
    <el-header class="header">
      <div class="header-content">
        <div class="logo">
          <el-icon class="toggle-sidebar hidden-xs-only" @click="toggleSidebar">
            <Expand v-if="isCollapsed" />
            <Fold v-else />
          </el-icon>
          <el-icon class="logo-icon"><Cloudy /></el-icon>
          <div class="logo-text" v-show="!isCollapsed">
            <span class="main-title">TG-Drive</span>
            <span class="version-badge">v{{ appVersion }}</span>
          </div>
        </div>
        <div class="actions">
          <el-icon class="toggle-mobile-sidebar hidden-sm-and-up" @click="isMobileSidebarOpen = true">
            <Fold />
          </el-icon>
          <el-dropdown @command="handleThemeCommand" trigger="click">
            <span class="el-dropdown-link" style="outline: none; cursor: pointer;">
              <el-icon :size="20"><component :is="themeIcon" /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="light" :icon="Sunny">亮色模式</el-dropdown-item>
                <el-dropdown-item command="dark" :icon="Moon">暗色模式</el-dropdown-item>
                <el-dropdown-item command="auto" :icon="Monitor">跟随系统</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button v-if="userRole === 'admin'" type="primary" plain @click="goToAdmin">管理</el-button>
          <el-button v-if="!isLoggedIn" type="primary" @click="goToLogin">登录</el-button>
          <el-dropdown v-else @command="handleUserCommand" trigger="click">
            <div class="user-info">
              <el-avatar :size="32" src="/favicon.ico" style="cursor: pointer;" />
              <div class="user-details">
                <div class="username">{{ currentUsername }}</div>
                <div class="user-role">{{ userRoleText }}</div>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item disabled>
                  <div class="dropdown-user-info">
                    <div class="dropdown-username">{{ currentUsername }}</div>
                    <div class="dropdown-email">{{ currentEmail }}</div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- Desktop Sidebar -->
      <el-aside v-if="isLoggedIn && userRole === 'user' && !isMobile" :width="isCollapsed ? '64px' : '220px'" class="sidebar">
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleSelect"
          :collapse="isCollapsed"
          :collapse-transition="false"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>我的文件</template>
          </el-menu-item>
          <el-menu-item index="/tele-library">
            <el-icon><Box /></el-icon>
            <template #title>Tele 库</template>
          </el-menu-item>
          <el-menu-item index="/shared-library">
            <el-icon><Share /></el-icon>
            <template #title>共享库</template>
          </el-menu-item>
          <el-menu-item v-if="isPrivateAuthorized" index="/private-library">
            <el-icon><Lock /></el-icon>
            <template #title>私密库</template>
          </el-menu-item>
          <el-menu-item index="/gallery">
            <el-icon><Picture /></el-icon>
            <template #title>画廊</template>
          </el-menu-item>
          <el-menu-item index="/url-import">
            <el-icon><Link /></el-icon>
            <template #title>URL 导入</template>
          </el-menu-item>
          <el-menu-item index="/web-scrape">
            <el-icon><Picture /></el-icon>
            <template #title>网页解析</template>
          </el-menu-item>
          <el-menu-item index="/link-parser">
            <el-icon><Link /></el-icon>
            <template #title>链接解析</template>
          </el-menu-item>
          <el-menu-item index="/">
            <el-icon><Top /></el-icon>
            <template #title>上传文件</template>
          </el-menu-item>
          <el-menu-item index="/changePassword">
            <el-icon><EditPen /></el-icon>
            <template #title>修改密码</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- Mobile Sidebar Drawer -->
      <el-drawer
        v-if="isMobile"
        v-model="isMobileSidebarOpen"
        direction="ltr"
        :with-header="false"
        size="220px"
      >
        <el-menu
          v-if="isLoggedIn && userRole === 'user'"
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleSelectAndCloseDrawer"
          :collapse="false"
          :collapse-transition="false"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>我的文件</template>
          </el-menu-item>
          <el-menu-item index="/tele-library">
            <el-icon><Box /></el-icon>
            <template #title>Tele 库</template>
          </el-menu-item>
          <el-menu-item index="/shared-library">
            <el-icon><Share /></el-icon>
            <template #title>共享库</template>
          </el-menu-item>
          <el-menu-item v-if="isPrivateAuthorized" index="/private-library">
            <el-icon><Lock /></el-icon>
            <template #title>私密库</template>
          </el-menu-item>
          <el-menu-item index="/gallery">
            <el-icon><Picture /></el-icon>
            <template #title>画廊</template>
          </el-menu-item>
          <el-menu-item index="/url-import">
            <el-icon><Link /></el-icon>
            <template #title>URL 导入</template>
          </el-menu-item>
          <el-menu-item index="/web-scrape">
            <el-icon><Picture /></el-icon>
            <template #title>网页解析</template>
          </el-menu-item>
          <el-menu-item index="/link-parser">
            <el-icon><Link /></el-icon>
            <template #title>链接解析</template>
          </el-menu-item>
          <el-menu-item index="/">
            <el-icon><Top /></el-icon>
            <template #title>上传文件</template>
          </el-menu-item>
          <el-menu-item index="/changePassword">
            <el-icon><EditPen /></el-icon>
            <template #title>修改密码</template>
          </el-menu-item>
        </el-menu>
      </el-drawer>

      <el-main class="main-content" :class="{ 'no-sidebar': !isLoggedIn || userRole !== 'user' }">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <el-footer class="footer">
      <div class="footer-content">
        <div class="footer-links">
          <el-button type="text" @click="goToAbout" class="footer-link">关于我们</el-button>
          <el-divider direction="vertical" />
          <el-button type="text" @click="goToAgreement" class="footer-link">用户协议</el-button>
          <el-divider direction="vertical" />
          <el-button type="text" @click="goToPrivacy" class="footer-link">隐私政策</el-button>
        </div>
        <div class="footer-copyright">
          <span>© 2025 TG-Drive. All rights reserved.</span>
        </div>
      </div>
    </el-footer>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Cloudy, Menu, HomeFilled, Folder, Box, Share, Lock, Picture, Link, Top, EditPen, Sunny, Moon, Monitor, SwitchButton, Expand, Fold
} from '@element-plus/icons-vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import { APP_VERSION, checkVersionUpdate, markVersionSeen } from '@/version'
import request from '@/utils/request'

type Theme = 'light' | 'dark' | 'auto'

const router = useRouter()
const route = useRoute()

const isLoggedIn = ref(false)
const currentUsername = ref('')
const currentEmail = ref('')
const userRole = ref('')
const isPrivateAuthorized = ref(false)
const appVersion = APP_VERSION

const activeMenu = computed(() => route.path)
const isCollapsed = ref(false)
const isMobile = ref(false)
const isMobileSidebarOpen = ref(false)

const userRoleText = computed(() => {
  const roles: Record<string, string> = { admin: '管理员', user: '普通用户', visitor: '访客' }
  return roles[userRole.value] || '未知角色'
})

const theme = ref<Theme>('auto')
const themeIcon = computed(() => {
  if (theme.value === 'light') return Sunny
  if (theme.value === 'dark') return Moon
  return Monitor
})

const applyTheme = () => {
  document.body.classList.add('theme-switching')
  setTimeout(() => {
    if (theme.value === 'auto') {
      const prefersDark = window.matchMedia('(prefers-color-scheme: dark)')
      document.documentElement.classList.toggle('dark', prefersDark.matches)
    } else {
      document.documentElement.classList.toggle('dark', theme.value === 'dark')
    }
  }, 80)
  setTimeout(() => {
    document.body.classList.remove('theme-switching')
  }, 600)
}

const handleThemeCommand = (command: Theme) => {
  theme.value = command
  localStorage.setItem('theme', command)
  if ('vibrate' in navigator) navigator.vibrate(50)
  applyTheme()
}

const systemThemeChangeHandler = (e: MediaQueryListEvent) => {
  if (theme.value === 'auto') {
    document.body.classList.add('theme-switching')
    setTimeout(() => {
      document.documentElement.classList.toggle('dark', e.matches)
    }, 80)
    setTimeout(() => {
      document.body.classList.remove('theme-switching')
    }, 600)
  }
}

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) {
    isCollapsed.value = true
  } else {
    const savedSidebarState = localStorage.getItem('sidebarState')
    isCollapsed.value = savedSidebarState === 'collapsed'
  }
}

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem('sidebarState', isCollapsed.value ? 'collapsed' : 'expanded')
}

const checkLoginStatus = () => {
  const token = localStorage.getItem('token')
  if (token) {
    isLoggedIn.value = true
    currentUsername.value = localStorage.getItem('username') || ''
    currentEmail.value = localStorage.getItem('userEmail') || ''
    userRole.value = localStorage.getItem('userRole') || ''
    isPrivateAuthorized.value = localStorage.getItem('isPrivateAuthorized') === 'true'
  }
}

const checkVersion = () => {
  const hasUpdate = checkVersionUpdate()
  if (hasUpdate) {
    ElMessageBox.alert(
      '页面已更新，刷新以获取最新版本。',
      '版本更新',
      {
        confirmButtonText: '刷新页面',
        type: 'info',
        showClose: false,
        closeOnClickModal: false,
        closeOnPressEscape: false,
      }
    ).then(() => {
      markVersionSeen()
      window.location.reload()
    }).catch(() => {})
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
  if (!isMobile.value) {
    const savedSidebarState = localStorage.getItem('sidebarState')
    isCollapsed.value = savedSidebarState === 'collapsed'
  }
  const savedTheme = localStorage.getItem('theme') as Theme | null
  theme.value = savedTheme || 'auto'
  applyTheme()
  checkLoginStatus()
  checkVersion()
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', systemThemeChangeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
  window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', systemThemeChangeHandler)
})

watch(() => route.path, () => { checkLoginStatus() }, { immediate: true })

const handleSelect = (index: string) => { router.push(index) }
const handleSelectAndCloseDrawer = (index: string) => { handleSelect(index); isMobileSidebarOpen.value = false }
const goToLogin = () => router.push('/login')
const goToAdmin = () => router.push('/admin')
const goToAbout = () => router.push('/about')
const goToAgreement = () => router.push('/agreement')
const goToPrivacy = () => router.push('/privacy')
const handleUserCommand = (command: string) => {
  if (command === 'logout') {
    ElNotification({ title: '已退出登录', type: 'info', duration: 1500 })
    localStorage.removeItem('token')
    localStorage.removeItem('tokenExpireAt')
    localStorage.removeItem('userRole')
    localStorage.removeItem('username')
    isLoggedIn.value = false
    userRole.value = ''
    router.push('/login')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 60px;
  border-bottom: 1px solid var(--border-color);
  background-color: var(--container-bg-color);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.logo-text {
  display: flex;
  align-items: center;
  gap: 8px;
}

.main-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
}

.version-badge {
  font-size: 10px;
  color: #909399;
  font-weight: 400;
}

.toggle-sidebar {
  cursor: pointer;
  font-size: 20px;
  color: var(--text-color);
}

.actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.user-details .username {
  font-size: 14px;
  font-weight: 500;
  color: var(--text-color);
}

.user-details .user-role {
  font-size: 12px;
  color: #909399;
}

.dropdown-user-info {
  padding: 5px 0;
}

.dropdown-username {
  font-size: 14px;
  font-weight: 500;
}

.dropdown-email {
  font-size: 12px;
  color: #909399;
}

.sidebar {
  background-color: var(--container-bg-color);
  border-right: 1px solid var(--border-color);
  transition: width 0.3s, background-color 0.3s, border-color 0.3s;
  overflow: hidden;
}

.sidebar-menu {
  height: 100%;
  border-right: none;
}

.main-content {
  padding: 20px;
  background-color: var(--background-color);
  height: calc(100vh - 60px);
  overflow-y: auto;
  transition: margin-left 0.3s;
}

.main-content.no-sidebar {
  margin-left: 0;
}

.footer {
  border-top: 1px solid var(--border-color);
  background-color: var(--container-bg-color);
  height: 40px !important;
  padding: 0 20px;
}

.footer-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 5px;
}

.footer-link {
  font-size: 12px;
  color: #909399;
}

.footer-copyright {
  font-size: 12px;
  color: #909399;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.hidden-sm-and-up {
  display: none !important;
}

@media (max-width: 767px) {
  .hidden-xs-only {
    display: none !important;
  }
  .hidden-sm-and-up {
    display: flex !important;
  }
  .header {
    padding: 0 15px;
  }
  .logo-text {
    display: none;
  }
  .toggle-mobile-sidebar {
    cursor: pointer;
    font-size: 20px;
    margin-right: 10px;
  }
  .main-content {
    padding: 10px;
  }
  .footer {
    padding: 0 10px;
  }
}
</style>
