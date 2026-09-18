import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';
import request from '@/utils/request';
import { ElMessage } from 'element-plus';
import { callGlobalClearUserInfo } from '@/store/user';

// 使用懒加载导入组件
const Upload = () => import('../views/UploadPage.vue');
const Home = () => import('../views/Home.vue');
const FileList = () => import('../views/FileList.vue');
const Login = () => import('../views/LoginPage.vue');
const Register = () => import('../views/RegisterPage.vue');

const AboutPage = () => import('../views/AboutPage.vue');
const Layout = () => import('@/components/Layout.vue');
const AdminLayout = () => import('@/components/AdminLayout.vue');
const ChangePassword = () => import('../views/ChangePassword.vue');
const BackupPage = () => import('../views/BackupPage.vue');
const BotKeepAlivePage = () => import('../views/BotKeepAlivePage.vue');
const WebDavConfigPage = () => import('../views/WebDavConfigPage.vue');
const UserManagement = () => import('../views/UserManagement.vue');
const UserAgreementPage = () => import('@/views/UserAgreementPage.vue')
const PrivacyPolicyPage = () => import('@/views/PrivacyPolicyPage.vue')
const TeleLibrary = () => import('../views/TeleLibrary.vue')
const SharedLibrary = () => import('../views/SharedLibrary.vue')
const PrivateLibrary = () => import('../views/PrivateLibrary.vue')
const Gallery = () => import('../views/Gallery.vue')
const TagManagement = () => import('../views/TagManagement.vue')
const PrivateWhitelist = () => import('../views/PrivateWhitelist.vue')
const RedeemCodes = () => import('../views/RedeemCodes.vue')
const RedeemPage = () => import('../views/RedeemPage.vue')
const UrlImport = () => import('../views/UrlImport.vue')
const WebScrape = () => import('../views/WebScrape.vue')


interface RouteMeta extends Record<string | number | symbol, unknown> {
  requiresAuth?: boolean;
  requiredRole?: 'admin' | 'visitor' | 'user';
}

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    component: Layout,
    children: [
      {
        path: '',
        component: Upload,
        meta: {
          requiresAuth: true,
          requiredRole: 'visitor'
        } as RouteMeta
      },
      {
        path: 'login',
        component: Login,
      },
      {
        path: 'about',
        component: AboutPage,
      },
      {
        path: 'agreement',
        component: UserAgreementPage,
      },
      {
        path: 'privacy',
        component: PrivacyPolicyPage,
      }
    ]
  },
  {
    path: '/user',
    component: Layout,
    meta: {
      requiresAuth: true,
      requiredRole: 'user'
    } as RouteMeta,
    children: [
      {
        path: 'home',
        component: FileList,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'upload',
        component: Upload,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'changePassword',
        component: ChangePassword,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'tele',
        component: TeleLibrary,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'shared',
        component: SharedLibrary,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'private',
        component: PrivateLibrary,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'gallery',
        component: Gallery,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'redeem',
        component: RedeemPage,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'url-import',
        component: UrlImport,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      },
      {
        path: 'web-scrape',
        component: WebScrape,
        meta: {
          requiresAuth: true,
          requiredRole: 'user'
        } as RouteMeta
      }
    ]
  },
  {
    path: '/',
    component: AdminLayout,
    meta: {
      requiresAuth: true,
      requiredRole: 'admin'
    } as RouteMeta,
    children: [
      {
        path: 'home',
        component: Home,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'fileList',
        component: FileList,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'changePassword',
        component: ChangePassword,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'backup',
        component: BackupPage,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'bot-keep-alive',
        component: BotKeepAlivePage,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'webdav-config',
        component: WebDavConfigPage,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'user-management',
        component: UserManagement,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'tele-library',
        component: TeleLibrary,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'shared-library',
        component: SharedLibrary,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'private-library',
        component: PrivateLibrary,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'gallery',
        component: Gallery,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'tags',
        component: TagManagement,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'private-whitelist',
        component: PrivateWhitelist,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'redeem-codes',
        component: RedeemCodes,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'url-import',
        component: UrlImport,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },
      {
        path: 'web-scrape',
        component: WebScrape,
        meta: {
          requiresAuth: true,
          requiredRole: 'admin'
        } as RouteMeta
      },

    ]
  },
  { 
    path: '/login', 
    component: Login 
  },
  { 
    path: '/register', 
    component: Register,
    beforeEnter: async (_to, _from, next) => {
      try {
        const response = await request.get('/setting/registration-status');
        if (response.data.code === 1 && response.data.data.isRegistrationAllowed) {
          next();
        } else {
          ElMessage.warning('管理员已关闭注册功能');
          next('/login');
        }
      } catch (error) {
        console.error('获取注册状态失败:', error);
        ElMessage.error('无法获取注册状态，请稍后再试');
        next('/login');
      }
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// Navigation guard
const whiteList = ['/login', '/register', '/agreement', '/privacy', '/about']; // Whitelist for routes that don't require authentication

router.beforeEach((to, _from, next) => {
  void _from;
  const token = localStorage.getItem('token');
  const expireAtRaw = localStorage.getItem('tokenExpireAt');

  if (token) {
    if (expireAtRaw) {
      const expireAt = Number(expireAtRaw);
      if (!Number.isNaN(expireAt) && Date.now() >= expireAt) {
        callGlobalClearUserInfo();
        if (to.path !== '/login') {
          ElMessage.warning('登录状态已过期，请重新登录');
        }
        next({ path: '/login', query: { redirect: to.fullPath } });
        return;
      }
    }

    // If logged in
    if (to.path === '/login') {
      // If trying to access login page, redirect to home
      next({ path: '/' });
    } else {
      // For other pages, proceed normally
      // Here you could add logic to verify token validity or fetch user roles if needed
      next();
    }
  } else {
    // If not logged in
    if (whiteList.indexOf(to.path) !== -1) {
      // If the route is in the whitelist, allow access
      next();
    } else {
      // For other routes, redirect to login page with the intended destination
      next({ path: '/login', query: { redirect: to.fullPath } });
    }
  }
});

export default router;
