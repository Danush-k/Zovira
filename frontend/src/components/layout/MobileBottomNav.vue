<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { House, LayoutGrid, Search, ShoppingBag, UserRound } from '@lucide/vue'

defineProps<{ cartCount?: number }>()
const emit = defineEmits<{ categories: []; search: [] }>()
const route = useRoute()

const isHome = computed(() => route.path === '/')
const isAccount = computed(() => route.path.startsWith('/account') || route.path === '/login')
const isCart = computed(() => route.path === '/cart')
</script>

<template>
  <nav
    class="fixed inset-x-0 bottom-0 z-40 border-t border-ink-150 bg-white/95 pb-[env(safe-area-inset-bottom)] backdrop-blur lg:hidden"
    aria-label="Primary"
  >
    <ul class="grid h-16 grid-cols-5">
      <li>
        <RouterLink to="/" :class="['flex h-full flex-col items-center justify-center gap-1 text-[11px] font-medium', isHome ? 'text-brand-700' : 'text-ink-500']">
          <House class="size-[22px]" :stroke-width="isHome ? 2.2 : 1.75" />
          {{ $t('nav.home') }}
        </RouterLink>
      </li>
      <li>
        <button type="button" class="flex h-full w-full flex-col items-center justify-center gap-1 text-[11px] font-medium text-ink-500" @click="emit('categories')">
          <LayoutGrid class="size-[22px]" stroke-width="1.75" />
          {{ $t('nav.categories') }}
        </button>
      </li>
      <li>
        <button type="button" class="flex h-full w-full flex-col items-center justify-center gap-1 text-[11px] font-medium text-ink-500" @click="emit('search')">
          <Search class="size-[22px]" stroke-width="1.75" />
          {{ $t('common.search') }}
        </button>
      </li>
      <li>
        <RouterLink :to="'/account'" :class="['flex h-full flex-col items-center justify-center gap-1 text-[11px] font-medium', isAccount ? 'text-brand-700' : 'text-ink-500']">
          <UserRound class="size-[22px]" :stroke-width="isAccount ? 2.2 : 1.75" />
          {{ $t('nav.account') }}
        </RouterLink>
      </li>
      <li>
        <RouterLink to="/cart" :class="['relative flex h-full flex-col items-center justify-center gap-1 text-[11px] font-medium', isCart ? 'text-brand-700' : 'text-ink-500']">
          <span class="relative">
            <ShoppingBag class="size-[22px]" :stroke-width="isCart ? 2.2 : 1.75" />
            <span
              v-if="cartCount"
              class="tabular absolute -top-1.5 -right-2 grid h-4 min-w-4 place-items-center rounded-full bg-accent-500 px-1 text-[10px] font-bold text-ink-950"
            >
              {{ cartCount > 99 ? '99+' : cartCount }}
            </span>
          </span>
          {{ $t('nav.cart') }}
        </RouterLink>
      </li>
    </ul>
  </nav>
</template>
