<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { CheckCircle2, Lock } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import PasswordStrength from '@/components/auth/PasswordStrength.vue'
import { authApi } from '@/services/auth'
import { errorMessage } from '@/services/errors'

const route = useRoute()
const token = computed(() => (typeof route.query.token === 'string' ? route.query.token : ''))

const form = reactive({ password: '', confirm: '' })
const errors = ref<Record<string, string>>({})
const formError = ref<string | null>(null)
const submitting = ref(false)
const done = ref(false)

async function submit() {
  errors.value = {}
  formError.value = null
  if (form.password.length < 8 || !/[A-Za-z]/.test(form.password) || !/\d/.test(form.password)) {
    errors.value.password = 'Use at least 8 characters with a letter and a number'
  }
  if (form.confirm !== form.password) errors.value.confirm = 'Passwords do not match'
  if (Object.keys(errors.value).length) return

  submitting.value = true
  try {
    await authApi.resetPassword(token.value, form.password)
    done.value = true
  } catch (e) {
    formError.value = errorMessage(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <div v-if="!token" class="text-center">
      <h1 class="text-2xl font-semibold tracking-tight">This reset link is incomplete</h1>
      <p class="mt-2 text-ink-500">Open the link from your email again, or request a new one.</p>
      <ZButton to="/forgot-password" class="mt-8">Request a new link</ZButton>
    </div>

    <div v-else-if="done" class="text-center">
      <div class="mx-auto grid size-14 place-items-center rounded-2xl bg-success-soft text-success">
        <CheckCircle2 class="size-7" stroke-width="1.75" />
      </div>
      <h1 class="mt-5 text-2xl font-semibold tracking-tight">Password updated</h1>
      <p class="mt-2 text-ink-500">For your security, you've been signed out of all devices.</p>
      <ZButton to="/login" size="lg" class="mt-8">Sign in</ZButton>
    </div>

    <template v-else>
      <h1 class="text-2xl font-semibold tracking-tight text-ink-900">Choose a new password</h1>
      <p class="mt-1.5 text-ink-500">Make it something you haven't used before.</p>
      <form class="mt-8 space-y-5" novalidate @submit.prevent="submit">
        <FormAlert v-if="formError" :message="formError" />
        <div>
          <ZInput
            v-model="form.password"
            label="New password"
            type="password"
            autocomplete="new-password"
            :icon="Lock"
            size="lg"
            :error="errors.password"
            autofocus
          />
          <PasswordStrength :password="form.password" />
        </div>
        <ZInput
          v-model="form.confirm"
          label="Confirm new password"
          type="password"
          autocomplete="new-password"
          :icon="Lock"
          size="lg"
          :error="errors.confirm"
        />
        <ZButton type="submit" size="lg" block :loading="submitting">Update password</ZButton>
      </form>
    </template>
  </div>
</template>
