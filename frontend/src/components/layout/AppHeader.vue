<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ShoppingBag } from '@lucide/vue'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import AccountMenu from './AccountMenu.vue'

const router = useRouter()
const query = ref('')

function submit() {
  const q = query.value.trim()
  if (q) void router.push({ path: '/search', query: { q } })
}
</script>

<template>
  <header class="sticky top-0 z-40 bg-white/95 backdrop-blur supports-[backdrop-filter]:bg-white/85">
    <div class="hidden bg-brand-900 text-[12.5px] text-brand-100 lg:block">
      <div class="container-page flex h-8 items-center justify-between">
        <p class="flex items-center gap-4">
          <span>{{ $t('promo.freeDelivery') }}</span>
          <span class="size-1 rounded-full bg-brand-400" aria-hidden="true" />
          <span>{{ $t('promo.easyReturns') }}</span>
        </p>
        <nav class="flex items-center gap-5" aria-label="Utility">
          <RouterLink to="/sell" class="hover:text-white">{{ $t('nav.sellOnZovira') }}</RouterLink>
          <RouterLink to="/help" class="hover:text-white">{{ $t('nav.help') }}</RouterLink>
        </nav>
      </div>
    </div>

    <div class="border-b border-ink-150">
      <div class="container-page flex h-16 items-center gap-4 lg:gap-8">
        <RouterLink to="/" class="shrink-0 rounded-md" aria-label="Zovira home">
          <BrandLogo />
        </RouterLink>

        <form role="search" class="relative hidden flex-1 md:block" @submit.prevent="submit">
          <label for="header-search" class="sr-only">{{ $t('common.search') }}</label>
          <input
            id="header-search"
            v-model="query"
            type="search"
            autocomplete="off"
            :placeholder="$t('common.searchPlaceholder')"
            class="h-11 w-full rounded-xl border border-ink-200 bg-ink-50 pr-12 pl-4 text-[15px] placeholder:text-ink-400 hover:border-ink-300 focus:border-brand-500 focus:bg-white focus:ring-3 focus:ring-brand-500/15 focus:outline-none"
          />
          <button
            type="submit"
            class="absolute top-1 right-1 grid size-9 place-items-center rounded-lg bg-brand-700 text-white hover:bg-brand-800"
            :aria-label="$t('common.search')"
          >
            <Search class="size-[18px]" />
          </button>
        </form>

        <nav class="ml-auto flex items-center gap-1" aria-label="Account">
          <AccountMenu />
          <RouterLink
            to="/cart"
            class="flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm font-semibold text-ink-800 hover:bg-ink-50"
          >
            <ShoppingBag class="size-5" stroke-width="1.75" />
            <span class="hidden sm:inline">{{ $t('nav.cart') }}</span>
          </RouterLink>
        </nav>
      </div>
    </div>
  </header>
</template>
