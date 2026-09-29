<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Lock, Mail, UserRound } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import PasswordStrength from '@/components/auth/PasswordStrength.vue'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { errorMessage, fieldErrors, toApiError } from '@/services/errors'
import { safeRedirect } from '@/router/redirect'

const auth = useAuthStore()
const toast = useToastStore()
const route = useRoute()
const router = useRouter()

const form = reactive({ fullName: '', email: '', password: '' })
const errors = ref<Record<string, string>>({})
const formError = ref<string | null>(null)
const submitting = ref(false)

function validate() {
  const e: Record<string, string> = {}
  if (form.fullName.trim().length < 2) e.fullName = 'Enter your full name'
  if (!/^\S+@\S+\.\S+$/.test(form.email.trim())) e.email = 'Enter a valid email address'
  if (form.password.length < 8 || !/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) {
    e.password = 'Use at least 8 characters with a letter and a number'
  }
  errors.value = e
  return Object.keys(e).length === 0
}

async function submit() {
  formError.value = null
  if (!validate()) return
  submitting.value = true
  try {
    await auth.register(form.fullName.trim(), form.email.trim(), form.password)
    toast.success('Welcome to Zovira', 'We sent a verification link to your email.')
    await router.replace(safeRedirect(route.query.redirect))
  } catch (e) {
    errors.value = fieldErrors(e)
    if (toApiError(e).code === 'EMAIL_TAKEN') errors.value.email = errorMessage(e)
    else if (!Object.keys(errors.value).length) formError.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold tracking-tight text-ink-900">Create your account</h1>
    <p class="mt-1.5 text-ink-500">Track orders, save favourites and check out faster.</p>

    <form class="mt-8 space-y-5" novalidate @submit.prevent="submit">
      <FormAlert v-if="formError" :message="formError" />
      <ZInput
        v-model="form.fullName"
        label="Full name"
        autocomplete="name"
        :icon="UserRound"
        size="lg"
        :error="errors.fullName"
        autofocus
      />
      <ZInput
        v-model="form.email"
        label="Email address"
        type="email"
        autocomplete="email"
        inputmode="email"
        :icon="Mail"
        size="lg"
        :error="errors.email"
      />
      <div>
        <ZInput
          v-model="form.password"
          label="Password"
          type="password"
          autocomplete="new-password"
          :icon="Lock"
          size="lg"
          :error="errors.password"
        />
        <PasswordStrength :password="form.password" />
      </div>
      <ZButton type="submit" size="lg" block :loading="submitting">Create account</ZButton>
      <p class="text-center text-xs leading-relaxed text-ink-500">
        By creating an account you agree to our
        <RouterLink to="/help/terms" class="underline hover:text-ink-800">Terms of use</RouterLink>
        and
        <RouterLink to="/help/privacy" class="underline hover:text-ink-800">Privacy policy</RouterLink>.
      </p>
    </form>

    <p class="mt-8 text-center text-sm text-ink-600">
      Already have an account?
      <RouterLink :to="{ path: '/login', query: route.query }" class="link">Sign in</RouterLink>
    </p>
  </div>
</template>
