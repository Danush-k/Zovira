<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import ZTextarea from '@/components/ui/ZTextarea.vue'
import ZSelect from '@/components/ui/ZSelect.vue'
import ZBadge from '@/components/ui/ZBadge.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import { sellerApi } from '@/services/seller'
import { errorMessage, fieldErrors } from '@/services/errors'
import { useToastStore } from '@/stores/toast'
import { INDIAN_STATES } from '@/utils/india'
import { formatDate } from '@/utils/format'
import type { SellerApplication, SellerProfile } from '@/types/seller'

const toast = useToastStore()
const profile = ref<SellerProfile | null>(null)
const form = reactive<SellerApplication>({
  storeName: '', description: '', gstin: '', supportEmail: '', supportPhone: '',
  pickupLine1: '', pickupCity: '', pickupState: '', pickupPincode: '',
})
const errors = ref<Record<string, string>>({})
const formError = ref<string | null>(null)
const saving = ref(false)

onMounted(async () => {
  try {
    const p = await sellerApi.profile()
    profile.value = p
    Object.assign(form, {
      storeName: p.storeName, description: p.description ?? '', gstin: p.gstin ?? '',
      supportEmail: p.supportEmail ?? '', supportPhone: p.supportPhone ?? '',
      pickupLine1: p.pickupLine1 ?? '', pickupCity: p.pickupCity ?? '',
      pickupState: p.pickupState ?? '', pickupPincode: p.pickupPincode ?? '',
    })
  } catch (e) {
    formError.value = errorMessage(e)
  }
})

async function save() {
  errors.value = {}
  formError.value = null
  saving.value = true
  try {
    profile.value = await sellerApi.updateProfile({ ...form, gstin: form.gstin || undefined })
    toast.success('Store profile updated')
  } catch (e) {
    errors.value = fieldErrors(e)
    if (!Object.keys(errors.value).length) formError.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="max-w-2xl">
    <h1 class="text-xl font-semibold tracking-tight">Store profile</h1>
    <p class="mt-1 text-sm text-ink-500">Shown to shoppers on your storefront and product pages.</p>

    <div v-if="profile" class="surface mt-5 flex flex-wrap items-center justify-between gap-4 p-5">
      <div>
        <p class="text-sm text-ink-500">Status</p>
        <ZBadge tone="success" class="mt-1">Approved</ZBadge>
        <p class="mt-1 text-[13px] text-ink-500">Selling since {{ formatDate(profile.approvedAt ?? profile.createdAt) }}</p>
      </div>
      <ZButton :to="`/store/${profile.slug}`" variant="outline" size="sm">View storefront</ZButton>
    </div>

    <form class="surface mt-5 space-y-5 p-5" novalidate @submit.prevent="save">
      <FormAlert v-if="formError" :message="formError" />
      <ZInput v-model="form.storeName" label="Store name" :error="errors.storeName" />
      <ZTextarea v-model="form.description" label="About your store" :maxlength="1000" :rows="3" optional />
      <div class="grid gap-4 sm:grid-cols-2">
        <ZInput v-model="form.supportEmail" label="Support email" type="email" :error="errors.supportEmail" />
        <ZInput v-model="form.supportPhone" label="Support phone" maxlength="10" inputmode="numeric" :error="errors.supportPhone" />
      </div>
      <ZInput v-model="form.gstin" label="GSTIN" optional maxlength="15" :error="errors.gstin" hint="Shown on tax invoices" />
      <fieldset class="space-y-4">
        <legend class="field-label">Pickup address</legend>
        <ZInput v-model="form.pickupLine1" label="Address" :error="errors.pickupLine1" />
        <div class="grid gap-4 sm:grid-cols-3">
          <ZInput v-model="form.pickupCity" label="City" :error="errors.pickupCity" />
          <ZSelect v-model="form.pickupState" label="State" placeholder="Select" :options="INDIAN_STATES.map((s) => ({ value: s, label: s }))" :error="errors.pickupState" />
          <ZInput v-model="form.pickupPincode" label="PIN code" maxlength="6" inputmode="numeric" :error="errors.pickupPincode" />
        </div>
      </fieldset>
      <div class="flex justify-end"><ZButton type="submit" :loading="saving">Save changes</ZButton></div>
    </form>
  </div>
</template>
