<script setup lang="ts">
import { ref, watch, type Component } from 'vue'
import { useRoute } from 'vue-router'
import { ExternalLink, Menu } from '@lucide/vue'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import ZDrawer from '@/components/ui/ZDrawer.vue'

export interface DashboardNavItem {
  label: string
  to: string
  icon: Component
  exact?: boolean
}

export interface DashboardNavGroup {
  label?: string
  items: DashboardNavItem[]
}

defineProps<{ title: string; groups: DashboardNavGroup[] }>()

const route = useRoute()
const mobileOpen = ref(false)
watch(() => route.fullPath, () => (mobileOpen.value = false))

function isActive(item: DashboardNavItem) {
  return item.exact ? route.path === item.to : route.path === item.to || route.path.startsWith(item.to + '/')
}
</script>

<template>
  <div class="min-h-dvh bg-canvas lg:grid lg:grid-cols-[256px_minmax(0,1fr)]">
    <aside class="sticky top-0 hidden h-dvh flex-col border-r border-ink-150 bg-white lg:flex">
      <div class="flex h-16 items-center gap-2.5 border-b border-ink-100 px-5">
        <RouterLink to="/" aria-label="Zovira storefront"><BrandLogo size="sm" /></RouterLink>
        <span class="rounded-md bg-ink-900 px-1.5 py-0.5 text-[11px] font-semibold tracking-wide text-white uppercase">
          {{ title }}
        </span>
      </div>
      <nav class="flex-1 space-y-6 overflow-y-auto p-3" :aria-label="`${title} navigation`">
        <div v-for="(group, gi) in groups" :key="gi">
          <p v-if="group.label" class="mb-1.5 px-3 text-[11px] font-semibold tracking-wider text-ink-400 uppercase">
            {{ group.label }}
          </p>
          <ul class="space-y-0.5">
            <li v-for="item in group.items" :key="item.to">
              <RouterLink
                :to="item.to"
                :aria-current="isActive(item) ? 'page' : undefined"
                :class="[
                  'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors',
                  isActive(item) ? 'bg-brand-50 text-brand-800' : 'text-ink-600 hover:bg-ink-50 hover:text-ink-900',
                ]"
              >
                <component :is="item.icon" class="size-[18px]" stroke-width="1.75" />
                {{ item.label }}
              </RouterLink>
            </li>
          </ul>
        </div>
      </nav>
      <div class="border-t border-ink-100 p-3">
        <RouterLink
          to="/"
          class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-ink-600 hover:bg-ink-50 hover:text-ink-900"
        >
          <ExternalLink class="size-[18px]" stroke-width="1.75" />
          Back to storefront
        </RouterLink>
      </div>
    </aside>

    <div class="flex min-w-0 flex-col">
      <header class="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-ink-150 bg-white/95 px-4 backdrop-blur sm:px-6">
        <button
          type="button"
          class="grid size-10 place-items-center rounded-lg text-ink-700 hover:bg-ink-100 lg:hidden"
          aria-label="Open navigation"
          @click="mobileOpen = true"
        >
          <Menu class="size-5" />
        </button>
        <slot name="header" />
      </header>
      <main id="main" class="flex-1 px-4 py-6 sm:px-6 lg:px-8" tabindex="-1">
        <RouterView />
      </main>
    </div>

    <ZDrawer v-model:open="mobileOpen" side="left" width="max-w-xs">
      <template #header>
        <BrandLogo size="sm" />
      </template>
      <nav class="space-y-6 p-3">
        <div v-for="(group, gi) in groups" :key="gi">
          <p v-if="group.label" class="mb-1.5 px-3 text-[11px] font-semibold tracking-wider text-ink-400 uppercase">
            {{ group.label }}
          </p>
          <RouterLink
            v-for="item in group.items"
            :key="item.to"
            :to="item.to"
            :class="[
              'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium',
              isActive(item) ? 'bg-brand-50 text-brand-800' : 'text-ink-700 hover:bg-ink-50',
            ]"
          >
            <component :is="item.icon" class="size-[18px]" stroke-width="1.75" />
            {{ item.label }}
          </RouterLink>
        </div>
      </nav>
    </ZDrawer>
  </div>
</template>
