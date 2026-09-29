<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import MobileBottomNav from '@/components/layout/MobileBottomNav.vue'
import MobileCategoryDrawer from '@/components/layout/MobileCategoryDrawer.vue'
import CompareTray from '@/components/layout/CompareTray.vue'
import { useCatalogStore } from '@/stores/catalog'
import { useCartStore } from '@/stores/cart'
import { useWishlistStore } from '@/stores/wishlist'
import { useAuthStore } from '@/stores/auth'

const catalog = useCatalogStore()
const cart = useCartStore()
const wishlist = useWishlistStore()
const auth = useAuthStore()
const header = ref<InstanceType<typeof AppHeader> | null>(null)
const drawerOpen = ref(false)

onMounted(() => {
  catalog.loadCategories().catch(() => undefined)
  void catalog.loadSettings()
  void auth.init().then(() => {
    cart.load().catch(() => undefined)
    wishlist.load().catch(() => undefined)
  })
})

auth.onChange((user) => {
  void cart.onAuthChange(!!user)
  wishlist.load().catch(() => undefined)
})
</script>

<template>
  <div class="flex min-h-dvh flex-col">
    <a
      href="#main"
      class="sr-only focus:not-sr-only focus:fixed focus:top-2 focus:left-2 focus:z-[100] focus:rounded-lg focus:bg-ink-900 focus:px-4 focus:py-2 focus:text-white"
    >
      Skip to content
    </a>
    <AppHeader ref="header" :cart-count="cart.count" @menu="drawerOpen = true" />
    <main id="main" class="flex-1" tabindex="-1">
      <RouterView />
    </main>
    <AppFooter />
    <MobileBottomNav :cart-count="cart.count" @categories="drawerOpen = true" @search="header?.focusSearch()" />
    <MobileCategoryDrawer v-model:open="drawerOpen" />
    <CompareTray />
  </div>
</template>
