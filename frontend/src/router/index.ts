import { createRouter, createWebHistory, type RouteComponent, type RouteMeta, type RouteRecordRaw } from 'vue-router'
import StorefrontLayout from '@/layouts/StorefrontLayout.vue'
import { useAuthStore } from '@/stores/auth'
import { safeRedirect } from './redirect'
import type { Role } from '@/types/api'

const AuthLayout = () => import('@/layouts/AuthLayout.vue')

/** Each auth page is its own top-level route so it never competes with the storefront for "/". */
function authRoute(
  path: string,
  name: string,
  component: () => Promise<RouteComponent>,
  meta: RouteMeta,
): RouteRecordRaw {
  return { path, component: AuthLayout, children: [{ path: '', name, component, meta }] }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    authRoute('/login', 'login', () => import('@/views/auth/LoginView.vue'), { title: 'Sign in', guestOnly: true }),
    authRoute('/register', 'register', () => import('@/views/auth/RegisterView.vue'), {
      title: 'Create account',
      guestOnly: true,
    }),
    authRoute('/forgot-password', 'forgot-password', () => import('@/views/auth/ForgotPasswordView.vue'), {
      title: 'Forgot password',
    }),
    authRoute('/reset-password', 'reset-password', () => import('@/views/auth/ResetPasswordView.vue'), {
      title: 'Reset password',
    }),
    authRoute('/verify-email', 'verify-email', () => import('@/views/auth/VerifyEmailView.vue'), {
      title: 'Verify email',
    }),
    {
      path: '/seller',
      component: () => import('@/views/seller/SellerLayout.vue'),
      meta: { requiresAuth: true, roles: ['SELLER'] },
      children: [
        { path: '', name: 'seller', component: () => import('@/views/seller/SellerOverviewView.vue'), meta: { title: 'Seller dashboard' } },
        { path: 'orders', name: 'seller-orders', component: () => import('@/views/seller/SellerOrdersView.vue'), meta: { title: 'Seller orders' } },
        { path: 'returns', name: 'seller-returns', component: () => import('@/views/seller/SellerReturnsView.vue'), meta: { title: 'Seller returns' } },
        { path: 'products', name: 'seller-products', component: () => import('@/views/seller/SellerProductsView.vue'), meta: { title: 'Your products' } },
        { path: 'products/:id', name: 'seller-product-form', component: () => import('@/views/seller/SellerProductFormView.vue'), meta: { title: 'Edit product' } },
        { path: 'inventory', name: 'seller-inventory', component: () => import('@/views/seller/SellerInventoryView.vue'), meta: { title: 'Inventory' } },
        { path: 'profile', name: 'seller-profile', component: () => import('@/views/seller/SellerProfileView.vue'), meta: { title: 'Store profile' } },
      ],
    },
    {
      path: '/',
      component: StorefrontLayout,
      children: [
        { path: '', name: 'home', component: () => import('@/views/HomeView.vue') },
        {
          path: 'p/:slug',
          name: 'product',
          component: () => import('@/views/ProductView.vue'),
          meta: { title: 'Product' },
        },
        { path: 'search', name: 'search', component: () => import('@/views/SearchView.vue'), meta: { title: 'Search' } },
        { path: 'c/:slug', name: 'category', component: () => import('@/views/SearchView.vue'), meta: { title: 'Shop' } },
        { path: 'brand/:slug', name: 'brand', component: () => import('@/views/SearchView.vue'), meta: { title: 'Brand' } },
        { path: 'store/:slug', name: 'store', component: () => import('@/views/SearchView.vue'), meta: { title: 'Store' } },
        {
          path: 'checkout',
          name: 'checkout',
          component: () => import('@/views/CheckoutView.vue'),
          meta: { title: 'Checkout', requiresAuth: true },
        },
        {
          path: 'order-confirmation/:number',
          name: 'order-confirmation',
          component: () => import('@/views/OrderConfirmationView.vue'),
          meta: { title: 'Order confirmation', requiresAuth: true },
        },
        { path: 'cart', name: 'cart', component: () => import('@/views/CartView.vue'), meta: { title: 'Cart' } },
        {
          path: 'wishlist',
          name: 'wishlist',
          component: () => import('@/views/WishlistView.vue'),
          meta: { title: 'Wishlist', requiresAuth: true },
        },
        { path: 'sell', name: 'sell', component: () => import('@/views/SellView.vue'), meta: { title: 'Sell on Zovira' } },
        { path: 'brands', name: 'brands', component: () => import('@/views/BrandsView.vue'), meta: { title: 'Brands' } },
        {
          path: 'compare',
          name: 'compare',
          component: () => import('@/views/CompareView.vue'),
          meta: { title: 'Compare products' },
        },
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
              path: 'orders',
              name: 'account-orders',
              component: () => import('@/views/account/OrdersView.vue'),
              meta: { title: 'Your orders' },
            },
            {
              path: 'orders/:number',
              name: 'account-order',
              component: () => import('@/views/account/OrderDetailView.vue'),
              meta: { title: 'Order details' },
            },
            {
              path: 'returns',
              name: 'account-returns',
              component: () => import('@/views/account/ReturnsView.vue'),
              meta: { title: 'Returns' },
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
  // Product pages set a richer title once their data loads.
  if (to.name === 'product') return
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} | Zovira` : 'Zovira - Shop electronics, fashion, home and more'
})

export default router
