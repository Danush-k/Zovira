<script setup lang="ts">
import { reactive, ref, watchEffect } from 'vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import ZBadge from '@/components/ui/ZBadge.vue'
import { accountApi } from '@/services/account'
import { errorMessage, fieldErrors } from '@/services/errors'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { formatDate } from '@/utils/format'
import { isValidMobile } from '@/utils/india'

const auth = useAuthStore()
const toast = useToastStore()

const form = reactive({ fullName: '', phone: '' })
const errors = ref<Record<string, string>>({})
const saving = ref(false)

watchEffect(() => {
  form.fullName = auth.user?.fullName ?? ''
  form.phone = auth.user?.phone ?? ''
})

async function save() {
  errors.value = {}
  if (form.fullName.trim().length < 2) errors.value.fullName = 'Enter your full name'
  if (form.phone && !isValidMobile(form.phone)) errors.value.phone = 'Enter a valid 10-digit mobile number'
  if (Object.keys(errors.value).length) return
  saving.value = true
  try {
    auth.setUser(await accountApi.updateProfile(form.fullName.trim(), form.phone.trim() || null))
    toast.success('Profile updated')
  } catch (e) {
    errors.value = fieldErrors(e)
    if (!Object.keys(errors.value).length) toast.error("Couldn't save your profile", errorMessage(e))
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold tracking-tight">Profile</h1>
    <p class="mt-1 text-ink-500">This information appears on your orders and invoices.</p>

    <form class="surface mt-6 max-w-xl space-y-5 p-6" novalidate @submit.prevent="save">
      <div>
        <p class="field-label">Email address</p>
        <div class="flex flex-wrap items-center gap-2">
          <span class="text-[15px] text-ink-900">{{ auth.user?.email }}</span>
          <ZBadge :tone="auth.user?.emailVerified ? 'success' : 'warning'" size="sm">
            {{ auth.user?.emailVerified ? 'Verified' : 'Not verified' }}
          </ZBadge>
        </div>
      </div>
      <ZInput v-model="form.fullName" label="Full name" autocomplete="name" :error="errors.fullName" />
      <ZInput
        v-model="form.phone"
        label="Mobile number"
        type="tel"
        inputmode="numeric"
        maxlength="10"
        autocomplete="tel-national"
        optional
        :error="errors.phone"
      />
      <div class="flex items-center justify-between gap-4 border-t border-ink-100 pt-5">
        <p class="text-[13px] text-ink-500">Member since {{ formatDate(auth.user?.createdAt) }}</p>
        <ZButton type="submit" :loading="saving">Save changes</ZButton>
      </div>
    </form>
  </div>
</template>
