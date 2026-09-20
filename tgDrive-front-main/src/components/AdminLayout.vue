<!-- src/components/AdminLayout.vue -->
<template>
  <el-container class="app-container">
    <el-header class="header">
      <div class="header-logo">
        <div class="logo-container">
          <el-icon class="toggle-sidebar hidden-xs-only" @click="toggleSidebar">
            <Expand v-if="isCollapsed" />
            <Fold v-else />
          </el-icon>
          <el-icon class="logo-icon"><Monitor /></el-icon>
          <span class="logo-text hidden-xs-only" v-show="!isCollapsed">TG-Drive 管理 <span class="version-badge">v{{ appVersion }}</span></span>
        </div>
      </div>
      <div class="header-actions">
        <el-icon class="toggle-mobile-sidebar hidden-sm-and-up" @click="isMobileSidebarOpen = true">
          <Fold />
        </el-icon>
        <el-dropdown @command="handleThemeCommand" trigger="click">
          <span class="el-dropdown-link">
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
        <el-dropdown @command="handleUserCommand">
          <el-avatar class="user-avatar" :size="32" src="/public/favicon.ico" />
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-container">
      <!-- Desktop Sidebar -->
      <el-aside v-if="!isMobile" :width="isCollapsed ? '64px' : '220px'" class="sidebar">
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleSelect"
          :collapse="isCollapsed"
          :collapse-transition="false"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <el-sub-menu index="file-mgmt">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span>文件管理</span>
            </template>
            <el-menu-item index="/fileList">文件列表</el-menu-item>
            <el-menu-item index="/tele-library">Tele 库</el-menu-item>
            <el-menu-item index="/shared-library">共享库</el-menu-item>
            <el-menu-item index="/private-library">私密库</el-menu-item>
            <el-menu-item index="/">上传文件</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="tools">
            <template #title>
              <el-icon><Link /></el-icon>
              <span>资源工具</span>
            </template>
            <el-menu-item index="/gallery">画廊</el-menu-item>
            <el-menu-item index="/url-import">URL 导入</el-menu-item>
            <el-menu-item index="/web-scrape">网页解析</el-menu-item>
            <el-menu-item index="/link-parser">链接解析</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="content-mgmt">
            <template #title>
              <el-icon><PriceTag /></el-icon>
              <span>内容管理</span>
            </template>
            <el-menu-item index="/tags">标签管理</el-menu-item>
            <el-menu-item index="/tag-rules">标签规则</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="sys-mgmt">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/user-management">用户管理</el-menu-item>
            <el-menu-item index="/private-whitelist">私密库白名单</el-menu-item>
            <el-menu-item index="/redeem-codes">兑换码管理</el-menu-item>
            <el-menu-item index="/changePassword">修改密码</el-menu-item>
            <el-menu-item index="/backup">备份数据库</el-menu-item>
            <el-menu-item index="/webdav-config">WebDAV配置</el-menu-item>
            <el-menu-item index="/agreement">用户协议</el-menu-item>
            <el-menu-item index="/privacy">隐私政策</el-menu-item>
          </el-sub-menu>
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
          :default-active="activeMenu"
          class="sidebar-menu"
          @select="handleSelectAndCloseDrawer"
          :collapse="false"
          :collapse-transition="false"
        >
          <el-menu-item index="/home">
            <el-icon><HomeFilled /></el-icon>
            <template #title>首页</template>
          </el-menu-item>

          <el-sub-menu index="file-mgmt">
            <template #title>
              <el-icon><Folder /></el-icon>
              <span>文件管理</span>
            </template>
            <el-menu-item index="/fileList">文件列表</el-menu-item>
            <el-menu-item index="/tele-library">Tele 库</el-menu-item>
            <el-menu-item index="/shared-library">共享库</el-menu-item>
            <el-menu-item index="/private-library">私密库</el-menu-item>
            <el-menu-item index="/">上传文件</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="tools">
            <template #title>
              <el-icon><Link /></el-icon>
              <span>资源工具</span>
            </template>
            <el-menu-item index="/gallery">画廊</el-menu-item>
            <el-menu-item index="/url-import">URL 导入</el-menu-item>
            <el-menu-item index="/web-scrape">网页解析</el-menu-item>
            <el-menu-item index="/link-parser">链接解析</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="content-mgmt">
            <template #title>
              <el-icon><PriceTag /></el-icon>
              <span>内容管理</span>
            </template>
            <el-menu-item index="/tags">标签管理</el-menu-item>
            <el-menu-item index="/tag-rules">标签规则</el-menu-item>
          </el-sub-menu>

          <el-sub-menu index="sys-mgmt">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/user-management">用户管理</el-menu-item>
            <el-menu-item index="/private-whitelist">私密库白名单</el-menu-item>
            <el-menu-item index="/redeem-codes">兑换码管理</el-menu-item>
            <el-menu-item index="/changePassword">修改密码</el-menu-item>
            <el-menu-item index="/backup">备份数据库</el-menu-item>
            <el-menu-item index="/webdav-config">WebDAV配置</el-menu-item>
            <el-menu-item index="/agreement">用户协议</el-menu-item>
            <el-menu-item index="/privacy">隐私政策</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-drawer>

      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  HomeFilled, Folder, Top, EditPen, Sunny, Moon, Expand, Fold, Download, Monitor, SwitchButton, Service, Connection, Setting, User,
  Box, Share, Lock, PriceTag, Picture, Ticket, Link
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { APP_VERSION, checkVersionUpdate, markVersionSeen } from '@/version'
import { ElMessageBox } from 'element-plus'

type Theme = 'light' | 'dark' | 'auto'

const router = useRouter()
const route = useRoute()
const appVersion = APP_VERSION

const activeMenu = computed(() => route.path)
const isCollapsed = ref(false)
const theme = ref<Theme>('auto')
const isMobile = ref(false)
const isMobileSidebarOpen = ref(false)

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
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', systemThemeChangeHandler)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkMobile)
  window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', systemThemeChangeHandler)
})

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
  localStorage.setItem('sidebarState', isCollapsed.value ? 'collapsed' : 'expanded')
}

const handleUserCommand = (command: string) => {
  if (command === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('tokenExpireAt')
    router.push('/login')
  }
}

const handleSelect = (index: string) => {
  router.push(index)
}

const handleSelectAndCloseDrawer = (index: string) => {
  handleSelect(index)
  isMobileSidebarOpen.value = false
}
</script>

<style scoped>
.app-container {
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
  transition: background-color 0.3s, border-color 0.3s;
}

.header-logo, .header-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.logo-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: var(--text-color);
}

.version-badge {
  font-size: 10px;
  color: #909399;
  font-weight: 400;
  margin-left: 4px;
}

.toggle-sidebar {
  cursor: pointer;
  font-size: 20px;
}

.user-avatar {
  cursor: pointer;
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

.sidebar-menu :deep(.el-sub-menu__title) {
  font-size: 14px;
}

.sidebar-menu :deep(.el-menu-item) {
  font-size: 13px;
  min-width: 0;
  padding-left: 48px !important;
}

.main-content {
  padding: 20px;
  background-color: var(--background-color);
  height: calc(100vh - 60px);
  overflow-y: auto;
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
  .header-logo .logo-text {
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
}
</style>
