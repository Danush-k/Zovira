import { createRouter, createWebHistory } from 'vue-router'
import StorefrontLayout from '@/layouts/StorefrontLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { safeRedirect } from './redirect'
import type { Role } from '@/types/api'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [
        {
          path: 'login',
          name: 'login',
          component: () => import('@/views/auth/LoginView.vue'),
          meta: { title: 'Sign in', guestOnly: true },
        },
        {
          path: 'register',
          name: 'register',
          component: () => import('@/views/auth/RegisterView.vue'),
          meta: { title: 'Create account', guestOnly: true },
        },
        {
          path: 'forgot-password',
          name: 'forgot-password',
          component: () => import('@/views/auth/ForgotPasswordView.vue'),
          meta: { title: 'Forgot password' },
        },
        {
          path: 'reset-password',
          name: 'reset-password',
          component: () => import('@/views/auth/ResetPasswordView.vue'),
          meta: { title: 'Reset password' },
        },
        {
          path: 'verify-email',
          name: 'verify-email',
          component: () => import('@/views/auth/VerifyEmailView.vue'),
          meta: { title: 'Verify email' },
        },
      ],
    },
    {
      path: '/',
      component: StorefrontLayout,
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
        {
          path: 'account',
          component: () => import('@/views/account/AccountLayout.vue'),
          meta: { requiresAuth: true },
          children: [
            {
              path: '',
              name: 'account',
              component: () => import('@/views/account/AccountOverviewView.vue'),
              meta: { title: 'Your account' },
            },
            {
              path: 'profile',
              name: 'account-profile',
              component: () => import('@/views/account/ProfileView.vue'),
              meta: { title: 'Profile' },
            },
            {
              path: 'addresses',
              name: 'account-addresses',
              component: () => import('@/views/account/AddressesView.vue'),
              meta: { title: 'Addresses' },
            },
            {
              path: 'security',
              name: 'account-security',
              component: () => import('@/views/account/SecurityView.vue'),
              meta: { title: 'Login & security' },
            },
          ],
        },
        {
          path: 'help/:topic?',
          name: 'help',
          component: () => import('@/views/HelpView.vue'),
          meta: { title: 'Help center' },
        },
        {
          path: 'forbidden',
          name: 'forbidden',
          component: () => import('@/views/ForbiddenView.vue'),
          meta: { title: 'Access denied' },
        },
        {
          path: ':pathMatch(.*)*',
          name: 'not-found',
          component: () => import('@/views/NotFoundView.vue'),
          meta: { title: 'Page not found' },
        },
      ],
    },
  ],
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    if (to.path === from.path) return false
    return { top: 0 }
  },
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  await auth.init()

  const requiresAuth = to.matched.some((r) => r.meta.requiresAuth)
  if (requiresAuth && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.guestOnly && auth.isAuthenticated) {
    return safeRedirect(to.query.redirect)
  }
  const roles = to.matched.flatMap((r) => r.meta.roles ?? []) as Role[]
  if (roles.length && !roles.some((r) => auth.hasRole(r))) {
    return { name: 'forbidden' }
  }
  return true
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} | Zovira` : 'Zovira - Shop electronics, fashion, home and more'
})

export default router
