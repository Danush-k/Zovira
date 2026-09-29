<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import ZDialog from '@/components/ui/ZDialog.vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZInput from '@/components/ui/ZInput.vue'
import ZSelect from '@/components/ui/ZSelect.vue'
import ZCheckbox from '@/components/ui/ZCheckbox.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import { accountApi } from '@/services/account'
import { errorMessage, fieldErrors } from '@/services/errors'
import { INDIAN_STATES, isValidMobile, isValidPincode } from '@/utils/india'
import type { Address, AddressInput, AddressType } from '@/types/api'

const props = defineProps<{ address?: Address | null; defaultName?: string; defaultPhone?: string | null }>()
const emit = defineEmits<{ saved: [address: Address] }>()
const open = defineModel<boolean>('open', { default: false })

const blank = (): AddressInput => ({
  fullName: props.defaultName ?? '',
  phone: props.defaultPhone ?? '',
  line1: '',
  line2: '',
  landmark: '',
  city: '',
  state: '',
  pincode: '',
  type: 'HOME',
  makeDefault: false,
})

const form = reactive<AddressInput>(blank())
const errors = ref<Record<string, string>>({})
const formError = ref<string | null>(null)
const saving = ref(false)

watch(open, (isOpen) => {
  if (!isOpen) return
  errors.value = {}
  formError.value = null
  const a = props.address
  Object.assign(
    form,
    a
      ? {
          fullName: a.fullName,
          phone: a.phone,
          line1: a.line1,
          line2: a.line2 ?? '',
          landmark: a.landmark ?? '',
          city: a.city,
          state: a.state,
          pincode: a.pincode,
          type: a.type,
          makeDefault: a.isDefault,
        }
      : blank(),
  )
})

const types: { value: AddressType; label: string }[] = [
  { value: 'HOME', label: 'Home' },
  { value: 'WORK', label: 'Work' },
  { value: 'OTHER', label: 'Other' },
]
const stateOptions = INDIAN_STATES.map((s) => ({ value: s, label: s }))

function validate() {
  const e: Record<string, string> = {}
  if (!form.fullName.trim()) e.fullName = "Enter the recipient's name"
  if (!isValidMobile(form.phone)) e.phone = 'Enter a valid 10-digit mobile number'
  if (!isValidPincode(form.pincode)) e.pincode = 'Enter a valid 6-digit PIN code'
  if (!form.line1.trim()) e.line1 = 'Enter the house number and street'
  if (!form.city.trim()) e.city = 'Enter a city'
  if (!form.state) e.state = 'Select a state'
  errors.value = e
  return !Object.keys(e).length
}

async function save() {
  formError.value = null
  if (!validate()) return
  saving.value = true
  try {
    const saved = props.address
      ? await accountApi.updateAddress(props.address.id, form)
      : await accountApi.createAddress(form)
    emit('saved', saved)
    open.value = false
  } catch (e) {
    errors.value = fieldErrors(e)
    if (!Object.keys(errors.value).length) formError.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <ZDialog v-model:open="open" :title="address ? 'Edit address' : 'Add a new address'" size="lg">
    <form id="address-form" class="grid gap-4 sm:grid-cols-2" novalidate @submit.prevent="save">
      <FormAlert v-if="formError" :message="formError" class="sm:col-span-2" />
      <ZInput v-model="form.fullName" label="Full name" autocomplete="name" :error="errors.fullName" />
      <ZInput
        v-model="form.phone"
        label="Mobile number"
        type="tel"
        inputmode="numeric"
        maxlength="10"
        autocomplete="tel-national"
        :error="errors.phone"
        hint="For delivery updates"
      />
      <ZInput
        v-model="form.line1"
        label="Flat, house no., building, street"
        autocomplete="address-line1"
        :error="errors.line1"
        class="sm:col-span-2"
      />
      <ZInput
        v-model="form.line2"
        label="Area, locality"
        autocomplete="address-line2"
        optional
        class="sm:col-span-2"
      />
      <ZInput v-model="form.landmark" label="Landmark" optional />
      <ZInput
        v-model="form.pincode"
        label="PIN code"
        inputmode="numeric"
        maxlength="6"
        autocomplete="postal-code"
        :error="errors.pincode"
      />
      <ZInput v-model="form.city" label="City / town" autocomplete="address-level2" :error="errors.city" />
      <ZSelect
        v-model="form.state"
        label="State"
        :options="stateOptions"
        placeholder="Select state"
        autocomplete="address-level1"
        :error="errors.state"
      />
      <fieldset class="sm:col-span-2">
        <legend class="field-label">Address type</legend>
        <div class="flex gap-2">
          <label
            v-for="t in types"
            :key="t.value"
            :class="[
              'cursor-pointer rounded-full border px-4 py-1.5 text-[13px] font-semibold transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-brand-500',
              form.type === t.value ? 'border-brand-700 bg-brand-50 text-brand-800' : 'border-ink-200 text-ink-700 hover:border-ink-300',
            ]"
          >
            <input v-model="form.type" type="radio" name="address-type" :value="t.value" class="sr-only" />
            {{ t.label }}
          </label>
        </div>
      </fieldset>
      <ZCheckbox v-model="form.makeDefault" label="Make this my default address" class="sm:col-span-2" />
    </form>
    <template #footer>
      <ZButton variant="ghost" @click="open = false">Cancel</ZButton>
      <ZButton type="submit" form="address-form" :loading="saving">
        {{ address ? 'Save address' : 'Add address' }}
      </ZButton>
    </template>
  </ZDialog>
</template>
