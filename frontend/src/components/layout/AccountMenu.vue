<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ChevronDown, LayoutDashboard, LogOut, MapPin, ShieldCheck, Store, UserRound } from '@lucide/vue'
import ZPopover from '@/components/ui/ZPopover.vue'
import ZButton from '@/components/ui/ZButton.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { accountNav } from '@/views/account/accountNav'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const links = computed(() => [
  { label: 'Your account', to: '/account', icon: UserRound },
  ...accountNav,
  { label: 'Addresses', to: '/account/addresses', icon: MapPin },
  { label: 'Login & security', to: '/account/security', icon: ShieldCheck },
])

async function signOut(close: () => void) {
  close()
  await auth.logout()
  toast.info('You have been signed out')
  await router.push('/')
}
</script>

<template>
  <ZPopover align="end" width="w-72" hover label="Account menu">
    <template #trigger="{ open, toggle }">
      <button
        type="button"
        class="flex items-center gap-2 rounded-lg px-2.5 py-1.5 text-left hover:bg-ink-50"
        :aria-expanded="open"
        aria-haspopup="menu"
        @click="auth.isAuthenticated ? toggle() : router.push('/login')"
      >
        <UserRound class="size-5 text-ink-800" stroke-width="1.75" />
        <span class="hidden leading-tight xl:block">
          <span class="block text-xs text-ink-500">
            {{ auth.isAuthenticated ? `${$t('nav.hello')}, ${auth.firstName}` : `${$t('nav.hello')}, ${$t('nav.signIn').toLowerCase()}` }}
          </span>
          <span class="flex items-center gap-0.5 text-sm font-semibold text-ink-900">
            {{ $t('nav.account') }}
            <ChevronDown class="size-3.5 text-ink-500" />
          </span>
        </span>
      </button>
    </template>

    <template #default="{ close }">
      <div v-if="!auth.isAuthenticated" class="p-4">
        <ZButton to="/login" block @click="close">{{ $t('nav.signIn') }}</ZButton>
        <p class="mt-3 text-center text-[13px] text-ink-600">
          New customer?
          <RouterLink to="/register" class="link" @click="close">Start here</RouterLink>
        </p>
      </div>
      <div v-else>
        <div class="border-b border-ink-100 px-4 py-3">
          <p class="truncate text-sm font-semibold">{{ auth.user?.fullName }}</p>
          <p class="truncate text-[13px] text-ink-500">{{ auth.user?.email }}</p>
        </div>
        <ul class="p-1.5" role="menu">
          <li v-for="link in links" :key="link.to" role="none">
            <RouterLink
              :to="link.to"
              role="menuitem"
              class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-ink-700 hover:bg-ink-50 hover:text-ink-900 focus:bg-ink-50 focus:outline-none"
              @click="close"
            >
              <component :is="link.icon" class="size-4 text-ink-500" />
              {{ link.label }}
            </RouterLink>
          </li>
          <li v-if="auth.isSeller" role="none">
            <RouterLink
              to="/seller"
              role="menuitem"
              class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-ink-700 hover:bg-ink-50 focus:bg-ink-50 focus:outline-none"
              @click="close"
            >
              <Store class="size-4 text-ink-500" />
              Seller dashboard
            </RouterLink>
          </li>
          <li v-if="auth.isAdmin" role="none">
            <RouterLink
              to="/admin"
              role="menuitem"
              class="flex items-center gap-3 rounded-lg px-3 py-2 text-sm text-ink-700 hover:bg-ink-50 focus:bg-ink-50 focus:outline-none"
              @click="close"
            >
              <LayoutDashboard class="size-4 text-ink-500" />
              Admin console
            </RouterLink>
          </li>
        </ul>
        <div class="border-t border-ink-100 p-1.5">
          <button
            type="button"
            role="menuitem"
            class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-ink-700 hover:bg-ink-50 focus:bg-ink-50 focus:outline-none"
            @click="signOut(close)"
          >
            <LogOut class="size-4 text-ink-500" />
            Sign out
          </button>
        </div>
      </div>
    </template>
  </ZPopover>
</template>
