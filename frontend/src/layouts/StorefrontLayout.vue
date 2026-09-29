<script setup lang="ts">
import { onMounted, ref } from 'vue'
import AppHeader from '@/components/layout/AppHeader.vue'
import AppFooter from '@/components/layout/AppFooter.vue'
import MobileBottomNav from '@/components/layout/MobileBottomNav.vue'
import MobileCategoryDrawer from '@/components/layout/MobileCategoryDrawer.vue'
import CompareTray from '@/components/layout/CompareTray.vue'
import { useCatalogStore } from '@/stores/catalog'

const catalog = useCatalogStore()
const header = ref<InstanceType<typeof AppHeader> | null>(null)
const drawerOpen = ref(false)

onMounted(() => {
  catalog.loadCategories().catch(() => undefined)
  void catalog.loadSettings()
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
    <AppHeader ref="header" @menu="drawerOpen = true" />
    <main id="main" class="flex-1" tabindex="-1">
      <RouterView />
    </main>
    <AppFooter />
    <MobileBottomNav @categories="drawerOpen = true" @search="header?.focusSearch()" />
    <MobileCategoryDrawer v-model:open="drawerOpen" />
    <CompareTray />
  </div>
</template>
