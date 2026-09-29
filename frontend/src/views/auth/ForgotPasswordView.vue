<script setup lang="ts">
import { ref } from 'vue'
import { ArrowLeft, Mail, MailCheck } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import { authApi } from '@/services/auth'
import { errorMessage } from '@/services/errors'

const email = ref('')
const error = ref<string | null>(null)
const formError = ref<string | null>(null)
const submitting = ref(false)
const sent = ref(false)

async function submit() {
  error.value = null
  formError.value = null
  if (!/^\S+@\S+\.\S+$/.test(email.value.trim())) {
    error.value = 'Enter a valid email address'
    return
  }
  submitting.value = true
  try {
    await authApi.forgotPassword(email.value.trim())
    sent.value = true
  } catch (e) {
    formError.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <RouterLink to="/login" class="inline-flex items-center gap-1.5 text-sm font-medium text-ink-600 hover:text-ink-900">
      <ArrowLeft class="size-4" />
      Back to sign in
    </RouterLink>

    <template v-if="!sent">
      <h1 class="mt-6 text-2xl font-semibold tracking-tight text-ink-900">Forgot your password?</h1>
      <p class="mt-1.5 text-ink-500">Enter the email you use for Zovira and we'll send you a reset link.</p>
      <form class="mt-8 space-y-5" novalidate @submit.prevent="submit">
        <FormAlert v-if="formError" :message="formError" />
        <ZInput
          v-model="email"
          label="Email address"
          type="email"
          autocomplete="email"
          :icon="Mail"
          size="lg"
          :error="error"
          autofocus
        />
        <ZButton type="submit" size="lg" block :loading="submitting">Send reset link</ZButton>
      </form>
    </template>

    <div v-else class="mt-10 text-center">
      <div class="mx-auto grid size-14 place-items-center rounded-2xl bg-brand-50 text-brand-700">
        <MailCheck class="size-7" stroke-width="1.75" />
      </div>
      <h1 class="mt-5 text-2xl font-semibold tracking-tight text-ink-900">Check your inbox</h1>
      <p class="mt-2 text-ink-500">
        If an account exists for <span class="font-medium text-ink-800">{{ email }}</span>, you'll receive a link to
        reset your password within a few minutes.
      </p>
      <ZButton variant="outline" class="mt-8" @click="sent = false">Use a different email</ZButton>
    </div>
  </div>
</template>
