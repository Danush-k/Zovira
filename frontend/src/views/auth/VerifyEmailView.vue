<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { CheckCircle2, XCircle } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZSpinner from '@/components/ui/ZSpinner.vue'
import { authApi } from '@/services/auth'
import { errorMessage } from '@/services/errors'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const state = ref<'verifying' | 'success' | 'error'>('verifying')
const message = ref('')

onMounted(async () => {
  const token = typeof route.query.token === 'string' ? route.query.token : ''
  if (!token) {
    state.value = 'error'
    message.value = 'This verification link is incomplete. Open the link from your email again.'
    return
  }
  try {
    await authApi.verifyEmail(token)
    if (auth.user) auth.setUser({ ...auth.user, emailVerified: true })
    state.value = 'success'
  } catch (e) {
    state.value = 'error'
    message.value = errorMessage(e)
  }
})
</script>

<template>
  <div class="text-center">
    <template v-if="state === 'verifying'">
      <ZSpinner class="mx-auto size-8 text-brand-700" />
      <h1 class="mt-5 text-xl font-semibold">Verifying your email</h1>
    </template>

    <template v-else-if="state === 'success'">
      <div class="mx-auto grid size-14 place-items-center rounded-2xl bg-success-soft text-success">
        <CheckCircle2 class="size-7" stroke-width="1.75" />
      </div>
      <h1 class="mt-5 text-2xl font-semibold tracking-tight">Email verified</h1>
      <p class="mt-2 text-ink-500">Your account is fully set up. Happy shopping.</p>
      <ZButton to="/" size="lg" class="mt-8">Start shopping</ZButton>
    </template>

    <template v-else>
      <div class="mx-auto grid size-14 place-items-center rounded-2xl bg-danger-soft text-danger">
        <XCircle class="size-7" stroke-width="1.75" />
      </div>
      <h1 class="mt-5 text-2xl font-semibold tracking-tight">We couldn't verify your email</h1>
      <p class="mt-2 text-ink-500">{{ message }}</p>
      <ZButton :to="auth.isAuthenticated ? '/account/security' : '/login'" variant="outline" class="mt-8">
        {{ auth.isAuthenticated ? 'Request a new link' : 'Sign in to request a new link' }}
      </ZButton>
    </template>
  </div>
</template>
