<script setup lang="ts">
import { computed } from 'vue'
import { ChevronRight, MapPin, ShieldCheck, Store, UserRound } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { accountNav } from './accountNav'
import EmailVerificationNotice from '@/components/account/EmailVerificationNotice.vue'

const auth = useAuthStore()

const tiles = computed(() => [
  ...accountNav.map((n) => ({ ...n, body: tileCopy[n.label] ?? '' })),
  { label: 'Profile', to: '/account/profile', icon: UserRound, body: 'Update your name and phone number' },
  { label: 'Addresses', to: '/account/addresses', icon: MapPin, body: 'Manage delivery addresses' },
  { label: 'Login & security', to: '/account/security', icon: ShieldCheck, body: 'Password and signed-in devices' },
  ...(auth.isSeller
    ? [{ label: 'Seller dashboard', to: '/seller', icon: Store, body: 'Manage listings, orders and payouts' }]
    : [{ label: 'Sell on Zovira', to: '/sell', icon: Store, body: 'Reach customers across India' }]),
])

const tileCopy: Record<string, string> = {
  Orders: 'Track, return or buy things again',
  Wishlist: 'Products you saved for later',
  Notifications: 'Order updates and offers',
  Returns: 'Return requests and refunds',
}
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold tracking-tight">Hello, {{ auth.firstName }}</h1>
    <p class="mt-1 text-ink-500">Manage your orders, addresses and account settings.</p>

    <EmailVerificationNotice class="mt-6" />

    <ul class="mt-6 grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      <li v-for="tile in tiles" :key="tile.to">
        <RouterLink
          :to="tile.to"
          class="surface group flex h-full items-center gap-4 p-5 transition-shadow hover:shadow-raised"
        >
          <span class="grid size-11 shrink-0 place-items-center rounded-xl bg-brand-50 text-brand-700">
            <component :is="tile.icon" class="size-5" stroke-width="1.75" />
          </span>
          <span class="min-w-0 flex-1">
            <span class="block font-semibold text-ink-900">{{ tile.label }}</span>
            <span class="mt-0.5 block text-[13px] text-ink-500">{{ tile.body }}</span>
          </span>
          <ChevronRight class="size-4 text-ink-300 transition-transform group-hover:translate-x-0.5" />
        </RouterLink>
      </li>
    </ul>
  </div>
</template>
