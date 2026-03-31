import { createRouter, createWebHistory } from 'vue-router'
import { appRouteChildren, canAccessRoute, type AppRouteMeta } from '@/router/access'
import { useAuthStore } from '@/store/auth'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: {
      public: true
    }
  },
  {
    path: '/403',
    name: 'forbidden',
    component: () => import('@/views/ForbiddenView.vue'),
    meta: {
      requiresAuth: true,
      allowFirstLogin: true
    }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: {
      requiresAuth: true
    },
    children: appRouteChildren
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  const hasSession = Boolean(authStore.state.session?.token)

  if (to.meta.public) {
    if (hasSession) {
      return '/'
    }
    return true
  }

  if (!hasSession) {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  try {
    await authStore.ensureCurrentUser()
  } catch {
    return {
      path: '/login',
      query: {
        redirect: to.fullPath
      }
    }
  }

  const roleCode = authStore.state.currentUser?.roleCode ?? authStore.state.session?.userInfo.roleCode
  const firstLoginRequired = Boolean(authStore.state.session?.firstLoginRequired)
  const meta = to.meta as unknown as AppRouteMeta | undefined

  if (firstLoginRequired && !meta?.allowFirstLogin) {
    return '/'
  }

  if (!canAccessRoute(meta, roleCode)) {
    return '/403'
  }

  return true
})

export default router
