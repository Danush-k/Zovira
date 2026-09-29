<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { LayoutGrid, MapPin, ShieldCheck, UserRound } from '@lucide/vue'
import ZAvatar from '@/components/ui/ZAvatar.vue'
import { useAuthStore } from '@/stores/auth'
import { accountNav } from './accountNav'

const auth = useAuthStore()
const route = useRoute()

const items = computed(() => [
  { label: 'Overview', to: '/account', icon: LayoutGrid, exact: true },
  ...accountNav,
  { label: 'Profile', to: '/account/profile', icon: UserRound },
  { label: 'Addresses', to: '/account/addresses', icon: MapPin },
  { label: 'Login & security', to: '/account/security', icon: ShieldCheck },
])

function active(to: string, exact?: boolean) {
  return exact ? route.path === to : route.path.startsWith(to)
}
</script>

<template>
  <div class="container-page py-6 lg:py-10">
    <div class="grid gap-8 lg:grid-cols-[240px_minmax(0,1fr)]">
      <aside class="hidden lg:block">
        <div class="flex items-center gap-3 px-2">
          <ZAvatar :name="auth.user?.fullName ?? ''" :src="auth.user?.avatarUrl" />
          <div class="min-w-0">
            <p class="truncate text-sm font-semibold">{{ auth.user?.fullName }}</p>
            <p class="truncate text-[13px] text-ink-500">{{ auth.user?.email }}</p>
          </div>
        </div>
        <nav class="mt-6 space-y-0.5" aria-label="Account">
          <RouterLink
            v-for="item in items"
            :key="item.to"
            :to="item.to"
            :aria-current="active(item.to, item.exact) ? 'page' : undefined"
            :class="[
              'flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors',
              active(item.to, item.exact) ? 'bg-white text-brand-800 shadow-card' : 'text-ink-600 hover:bg-white hover:text-ink-900',
            ]"
          >
            <component :is="item.icon" class="size-[18px]" stroke-width="1.75" />
            {{ item.label }}
          </RouterLink>
        </nav>
      </aside>

      <nav class="scrollbar-none -mx-4 flex gap-2 overflow-x-auto px-4 lg:hidden" aria-label="Account">
        <RouterLink
          v-for="item in items"
          :key="item.to"
          :to="item.to"
          :class="[
            'shrink-0 rounded-full border px-3.5 py-1.5 text-[13px] font-semibold',
            active(item.to, item.exact) ? 'border-brand-700 bg-brand-700 text-white' : 'border-ink-200 bg-white text-ink-700',
          ]"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <section class="min-w-0">
        <RouterView />
      </section>
    </div>
  </div>
</template>
