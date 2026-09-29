<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, Mail } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import DemoAccounts from '@/components/auth/DemoAccounts.vue'
import { useAuthStore } from '@/stores/auth'
import { errorMessage, fieldErrors } from '@/services/errors'
import { safeRedirect } from '@/router/redirect'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const form = reactive({ email: '', password: '' })
const errors = ref<Record<string, string>>({})
const formError = ref<string | null>(null)
const submitting = ref(false)
const demoMode = import.meta.env.VITE_DEMO_MODE === 'true'

async function submit() {
  errors.value = {}
  formError.value = null
  if (!form.email.trim()) errors.value.email = 'Enter your email address'
  if (!form.password) errors.value.password = 'Enter your password'
  if (Object.keys(errors.value).length) return

  submitting.value = true
  try {
    await auth.login(form.email.trim(), form.password)
    await router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    errors.value = fieldErrors(e)
    if (!Object.keys(errors.value).length) formError.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}

function useDemo(email: string, password: string) {
  form.email = email
  form.password = password
  void submit()
}
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold tracking-tight text-ink-900">Sign in to Zovira</h1>
    <p class="mt-1.5 text-ink-500">Welcome back. Enter your details to continue.</p>

    <form class="mt-8 space-y-5" novalidate @submit.prevent="submit">
      <FormAlert v-if="formError" :message="formError" />
      <ZInput
        v-model="form.email"
        label="Email address"
        type="email"
        autocomplete="email"
        inputmode="email"
        :icon="Mail"
        size="lg"
        :error="errors.email"
        autofocus
      />
      <div>
        <ZInput
          v-model="form.password"
          label="Password"
          type="password"
          autocomplete="current-password"
          :icon="Lock"
          size="lg"
          :error="errors.password"
        />
        <div class="mt-2 text-right">
          <RouterLink to="/forgot-password" class="link text-[13px]">Forgot password?</RouterLink>
        </div>
      </div>
      <ZButton type="submit" size="lg" block :loading="submitting">Sign in</ZButton>
    </form>

    <p class="mt-8 text-center text-sm text-ink-600">
      New to Zovira?
      <RouterLink :to="{ path: '/register', query: route.query }" class="link">Create an account</RouterLink>
    </p>

    <DemoAccounts v-if="demoMode" @pick="useDemo" />
  </div>
</template>
