<script setup lang="ts">
import { ref } from 'vue'
import { MailWarning } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import { authApi } from '@/services/auth'
import { errorMessage } from '@/services/errors'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'

const auth = useAuthStore()
const toast = useToastStore()
const sending = ref(false)
const sent = ref(false)

async function resend() {
  sending.value = true
  try {
    await authApi.resendVerification()
    sent.value = true
    toast.success('Verification email sent', `Check ${auth.user?.email} for the link.`)
  } catch (e) {
    toast.error("Couldn't send the email", errorMessage(e))
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <div
    v-if="auth.user && !auth.user.emailVerified"
    class="flex flex-col gap-3 rounded-xl border border-accent-300/60 bg-accent-50 p-4 sm:flex-row sm:items-center"
  >
    <MailWarning class="size-5 shrink-0 text-accent-700" />
    <div class="flex-1 text-sm">
      <p class="font-semibold text-ink-900">Verify your email to place orders</p>
      <p class="text-ink-600">We sent a link to {{ auth.user.email }}. It expires in 24 hours.</p>
    </div>
    <ZButton variant="outline" size="sm" :loading="sending" :disabled="sent" @click="resend">
      {{ sent ? 'Email sent' : 'Resend link' }}
    </ZButton>
  </div>
</template>
