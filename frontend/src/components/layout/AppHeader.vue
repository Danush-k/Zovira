<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { MapPin, Menu, Search, ShoppingBag } from '@lucide/vue'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import AccountMenu from './AccountMenu.vue'
import CategoryNav from './CategoryNav.vue'
import PincodeDialog from './PincodeDialog.vue'
import { useLocationStore } from '@/stores/location'

defineProps<{ cartCount?: number }>()
const emit = defineEmits<{ menu: [] }>()

const router = useRouter()
const route = useRoute()
const location = useLocationStore()
const query = ref(typeof route.query.q === 'string' ? route.query.q : '')
const pincodeOpen = ref(false)
const mobileSearch = ref<HTMLInputElement | null>(null)

watch(
  () => route.query.q,
  (q) => (query.value = typeof q === 'string' ? q : ''),
)

function submit() {
  const q = query.value.trim()
  if (q) void router.push({ path: '/search', query: { q } })
}

function focusSearch() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
  mobileSearch.value?.focus()
}

defineExpose({ focusSearch })
</script>

<template>
  <header class="sticky top-0 z-40 bg-white">
    <div class="hidden bg-brand-900 text-[12.5px] text-brand-100 lg:block">
      <div class="container-page flex h-8 items-center justify-between">
        <p class="flex items-center gap-4">
          <span>{{ $t('promo.freeDelivery') }}</span>
          <span class="size-1 rounded-full bg-brand-400" aria-hidden="true" />
          <span>{{ $t('promo.easyReturns') }}</span>
          <span class="size-1 rounded-full bg-brand-400" aria-hidden="true" />
          <span>{{ $t('promo.securePayments') }}</span>
        </p>
        <nav class="flex items-center gap-5" aria-label="Utility">
          <RouterLink to="/sell" class="hover:text-white">{{ $t('nav.sellOnZovira') }}</RouterLink>
          <RouterLink to="/account" class="hover:text-white">{{ $t('nav.trackOrder') }}</RouterLink>
          <RouterLink to="/help" class="hover:text-white">{{ $t('nav.help') }}</RouterLink>
        </nav>
      </div>
    </div>

    <div class="border-b border-ink-150">
      <div class="container-page flex h-16 items-center gap-3 lg:gap-6">
        <button
          type="button"
          class="-ml-2 grid size-10 place-items-center rounded-lg text-ink-800 hover:bg-ink-100 lg:hidden"
          :aria-label="$t('nav.allCategories')"
          @click="emit('menu')"
        >
          <Menu class="size-5" />
        </button>

        <RouterLink to="/" class="shrink-0 rounded-md" aria-label="Zovira home">
          <BrandLogo />
        </RouterLink>

        <button
          type="button"
          class="hidden shrink-0 items-center gap-2 rounded-lg px-2 py-1.5 text-left hover:bg-ink-50 lg:flex"
          @click="pincodeOpen = true"
        >
          <MapPin class="size-5 text-ink-700" stroke-width="1.75" />
          <span class="leading-tight">
            <span class="block text-xs text-ink-500">{{ $t('nav.deliverTo') }}</span>
            <span class="block text-sm font-semibold text-ink-900">{{ location.pincode ?? $t('nav.setPincode') }}</span>
          </span>
        </button>

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
            class="relative flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm font-semibold text-ink-800 hover:bg-ink-50"
            :aria-label="cartCount ? `${$t('nav.cart')}, ${cartCount} items` : $t('nav.cart')"
          >
            <span class="relative">
              <ShoppingBag class="size-[22px]" stroke-width="1.75" />
              <span
                v-if="cartCount"
                class="tabular absolute -top-1.5 -right-2 grid h-[18px] min-w-[18px] place-items-center rounded-full bg-accent-500 px-1 text-[10.5px] font-bold text-ink-950"
              >
                {{ cartCount > 99 ? '99+' : cartCount }}
              </span>
            </span>
            <span class="hidden xl:inline">{{ $t('nav.cart') }}</span>
          </RouterLink>
        </nav>
      </div>

      <div class="container-page pb-3 md:hidden">
        <form role="search" class="relative" @submit.prevent="submit">
          <label for="mobile-search" class="sr-only">{{ $t('common.search') }}</label>
          <Search class="pointer-events-none absolute top-1/2 left-3.5 size-[18px] -translate-y-1/2 text-ink-400" />
          <input
            id="mobile-search"
            ref="mobileSearch"
            v-model="query"
            type="search"
            autocomplete="off"
            enterkeyhint="search"
            :placeholder="$t('common.searchPlaceholder')"
            class="h-11 w-full rounded-xl border border-ink-200 bg-ink-50 pr-4 pl-10 text-[15px] placeholder:text-ink-400 focus:border-brand-500 focus:bg-white focus:ring-3 focus:ring-brand-500/15 focus:outline-none"
          />
        </form>
      </div>
    </div>

    <CategoryNav />
    <PincodeDialog v-model:open="pincodeOpen" />
  </header>
</template>
