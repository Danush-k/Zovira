<script setup lang="ts">
import { ref } from 'vue'
import { MapPin, Menu, ShoppingBag } from '@lucide/vue'
import SearchBox from '@/components/search/SearchBox.vue'
import BrandLogo from '@/components/brand/BrandLogo.vue'
import AccountMenu from './AccountMenu.vue'
import CategoryNav from './CategoryNav.vue'
import PincodeDialog from './PincodeDialog.vue'
import { useLocationStore } from '@/stores/location'

defineProps<{ cartCount?: number }>()
const emit = defineEmits<{ menu: [] }>()

const location = useLocationStore()
const pincodeOpen = ref(false)
const mobileSearch = ref<InstanceType<typeof SearchBox> | null>(null)

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
          <RouterLink to="/account/orders" class="hover:text-white">{{ $t('nav.trackOrder') }}</RouterLink>
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

        <SearchBox input-id="header-search" class="hidden flex-1 md:block" />

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
        <SearchBox ref="mobileSearch" input-id="mobile-search" variant="mobile" />
      </div>
    </div>

    <CategoryNav />
    <PincodeDialog v-model:open="pincodeOpen" />
  </header>
</template>
