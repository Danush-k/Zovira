<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Laptop, LogOut, Smartphone } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import ZBadge from '@/components/ui/ZBadge.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import PasswordStrength from '@/components/auth/PasswordStrength.vue'
import EmailVerificationNotice from '@/components/account/EmailVerificationNotice.vue'
import { accountApi } from '@/services/account'
import { authApi } from '@/services/auth'
import { errorMessage, fieldErrors } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { formatDateTime } from '@/utils/format'

const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

const pw = reactive({ current: '', next: '' })
const pwErrors = ref<Record<string, string>>({})
const savingPw = ref(false)

const { data: sessions, loading, error, execute: loadSessions } = useAsync(authApi.sessions, { immediate: true })
const signingOutAll = ref(false)

async function changePassword() {
  pwErrors.value = {}
  if (!pw.current) pwErrors.value.currentPassword = 'Enter your current password'
  if (pw.next.length < 8 || !/[A-Za-z]/.test(pw.next) || !/\d/.test(pw.next)) {
    pwErrors.value.newPassword = 'Use at least 8 characters with a letter and a number'
  }
  if (Object.keys(pwErrors.value).length) return
  savingPw.value = true
  try {
    await accountApi.changePassword(pw.current, pw.next)
    pw.current = ''
    pw.next = ''
    toast.success('Password changed', 'Other devices have been signed out.')
    await loadSessions()
  } catch (e) {
    const fe = fieldErrors(e)
    pwErrors.value = Object.keys(fe).length ? fe : { currentPassword: errorMessage(e) }
  } finally {
    savingPw.value = false
  }
}

async function revoke(id: number) {
  try {
    await authApi.revokeSession(id)
    await loadSessions()
  } catch (e) {
    toast.error("Couldn't sign out that device", errorMessage(e))
  }
}

async function signOutEverywhere() {
  signingOutAll.value = true
  try {
    await authApi.logoutAll()
  } finally {
    await auth.logout().catch(() => undefined)
    signingOutAll.value = false
    toast.info('Signed out of all devices')
    await router.replace('/login')
  }
}

function describe(ua: string | null) {
  if (!ua) return 'Unknown device'
  const browser = /Edg\//.test(ua)
    ? 'Edge'
    : /Chrome\//.test(ua)
      ? 'Chrome'
      : /Firefox\//.test(ua)
        ? 'Firefox'
        : /Safari\//.test(ua)
          ? 'Safari'
          : 'Browser'
  const os = /Android/.test(ua)
    ? 'Android'
    : /iPhone|iPad/.test(ua)
      ? 'iOS'
      : /Mac OS X/.test(ua)
        ? 'macOS'
        : /Windows/.test(ua)
          ? 'Windows'
          : /Linux/.test(ua)
            ? 'Linux'
            : ''
  return os ? `${browser} on ${os}` : browser
}

const isMobile = (ua: string | null) => !!ua && /Android|iPhone|iPad|Mobile/.test(ua)
</script>

<template>
  <div class="space-y-8">
    <div>
      <h1 class="text-2xl font-semibold tracking-tight">Login & security</h1>
      <p class="mt-1 text-ink-500">Keep your account secure.</p>
      <EmailVerificationNotice class="mt-6" />
    </div>

    <section class="surface max-w-xl p-6">
      <h2 class="text-base font-semibold">Change password</h2>
      <form class="mt-5 space-y-4" novalidate @submit.prevent="changePassword">
        <ZInput
          v-model="pw.current"
          label="Current password"
          type="password"
          autocomplete="current-password"
          :error="pwErrors.currentPassword"
        />
        <div>
          <ZInput
            v-model="pw.next"
            label="New password"
            type="password"
            autocomplete="new-password"
            :error="pwErrors.newPassword"
          />
          <PasswordStrength :password="pw.next" />
        </div>
        <div class="flex justify-end">
          <ZButton type="submit" :loading="savingPw">Update password</ZButton>
        </div>
      </form>
    </section>

    <section class="surface p-6">
      <div class="flex flex-wrap items-start justify-between gap-4">
        <div>
          <h2 class="text-base font-semibold">Signed-in devices</h2>
          <p class="mt-1 text-sm text-ink-500">Sign out of any device you don't recognise.</p>
        </div>
        <ZButton variant="outline" size="sm" :loading="signingOutAll" @click="signOutEverywhere">
          <LogOut class="size-4" />
          Sign out everywhere
        </ZButton>
      </div>

      <div v-if="loading && !sessions" class="mt-5 space-y-3">
        <div v-for="i in 2" :key="i" class="skeleton h-14" />
      </div>
      <ZErrorState v-else-if="error" :error="error" compact @retry="loadSessions" />
      <ul v-else class="mt-5 divide-y divide-ink-100">
        <li v-for="s in sessions" :key="s.id" class="flex items-center gap-4 py-3.5">
          <span class="grid size-10 shrink-0 place-items-center rounded-lg bg-ink-100 text-ink-600">
            <Smartphone v-if="isMobile(s.userAgent)" class="size-5" stroke-width="1.75" />
            <Laptop v-else class="size-5" stroke-width="1.75" />
          </span>
          <div class="min-w-0 flex-1">
            <p class="flex flex-wrap items-center gap-2 text-sm font-semibold">
              {{ describe(s.userAgent) }}
              <ZBadge v-if="s.current" tone="success" size="sm">This device</ZBadge>
            </p>
            <p class="text-[13px] text-ink-500">Last active {{ formatDateTime(s.lastUsedAt) }}</p>
          </div>
          <ZButton v-if="!s.current" variant="ghost" size="sm" @click="revoke(s.id)">Sign out</ZButton>
        </li>
      </ul>
    </section>
  </div>
</template>
