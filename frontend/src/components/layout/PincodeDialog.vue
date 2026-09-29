<script setup lang="ts">
import { ref, watch } from 'vue'
import { MapPin } from '@lucide/vue'
import ZDialog from '@/components/ui/ZDialog.vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import { useLocationStore } from '@/stores/location'
import { useToastStore } from '@/stores/toast'
import { errorMessage } from '@/services/errors'
import { isValidPincode } from '@/utils/india'

const open = defineModel<boolean>('open', { default: false })
const location = useLocationStore()
const toast = useToastStore()
const value = ref('')
const error = ref<string | null>(null)

watch(open, (isOpen) => {
  if (isOpen) {
    value.value = location.pincode ?? ''
    error.value = null
  }
})

async function save() {
  error.value = null
  if (!isValidPincode(value.value.trim())) {
    error.value = 'Enter a valid 6-digit PIN code'
    return
  }
  try {
    const estimate = await location.setPincode(value.value.trim())
    open.value = false
    if (!estimate.serviceable) toast.warning('Not serviceable yet', `We don't deliver to ${estimate.pincode} yet.`)
  } catch (e) {
    error.value = errorMessage(e)
  }
}
</script>

<template>
  <ZDialog v-model:open="open" title="Choose your delivery location" description="Delivery dates and options vary by PIN code." size="sm">
    <form id="pincode-form" novalidate @submit.prevent="save">
      <ZInput
        v-model="value"
        label="PIN code"
        inputmode="numeric"
        maxlength="6"
        autocomplete="postal-code"
        :icon="MapPin"
        :error="error"
        autofocus
      />
    </form>
    <template #footer>
      <ZButton variant="ghost" @click="open = false">Cancel</ZButton>
      <ZButton type="submit" form="pincode-form" :loading="location.loading">Apply</ZButton>
    </template>
  </ZDialog>
</template>
