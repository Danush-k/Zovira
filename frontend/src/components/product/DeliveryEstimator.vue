<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Banknote, MapPin, Truck, Zap } from '@lucide/vue'
import ZSpinner from '@/components/ui/ZSpinner.vue'
import { useLocationStore } from '@/stores/location'
import { errorMessage } from '@/services/errors'
import { formatDate, formatPrice } from '@/utils/format'
import { isValidPincode } from '@/utils/india'

const props = defineProps<{ codAvailable: boolean; price: number }>()

const location = useLocationStore()
const editing = ref(!location.pincode)
const input = ref(location.pincode ?? '')
const error = ref<string | null>(null)

onMounted(() => void location.refresh())

const estimate = computed(() => location.estimate)
const freeStandard = computed(
  () => !!estimate.value?.standard?.freeAbove && props.price >= estimate.value.standard.freeAbove,
)

async function check() {
  error.value = null
  const value = input.value.trim()
  if (!isValidPincode(value)) {
    error.value = 'Enter a valid 6-digit PIN code'
    return
  }
  try {
    await location.setPincode(value)
    editing.value = false
  } catch (e) {
    error.value = errorMessage(e)
  }
}

const deliveryDay = (iso: string | null | undefined) =>
  iso ? formatDate(iso, { weekday: 'long', day: 'numeric', month: 'short' }) : ''
</script>

<template>
  <section class="rounded-xl border border-ink-150 p-4" aria-labelledby="delivery-title">
    <div class="flex items-center justify-between gap-3">
      <h2 id="delivery-title" class="flex items-center gap-2 text-sm font-semibold text-ink-900">
        <MapPin class="size-4 text-brand-700" />
        Delivery
      </h2>
      <button
        v-if="!editing && location.pincode"
        type="button"
        class="text-[13px] font-semibold text-brand-700 hover:underline"
        @click="editing = true"
      >
        Change
      </button>
    </div>

    <form v-if="editing" class="mt-3 flex gap-2" novalidate @submit.prevent="check">
      <label for="pdp-pincode" class="sr-only">PIN code</label>
      <input
        id="pdp-pincode"
        v-model="input"
        inputmode="numeric"
        maxlength="6"
        placeholder="Enter PIN code"
        :aria-invalid="error ? 'true' : undefined"
        class="field-control h-10 flex-1"
      />
      <button
        type="submit"
        class="h-10 rounded-lg border border-ink-200 px-4 text-sm font-semibold text-brand-800 hover:bg-ink-50"
        :disabled="location.loading"
      >
        <ZSpinner v-if="location.loading" class="size-4" />
        <span v-else>Check</span>
      </button>
    </form>
    <p v-if="error" class="mt-2 text-[13px] text-danger" role="alert">{{ error }}</p>

    <div v-if="!editing && estimate" class="mt-3 space-y-3 text-sm">
      <p class="text-ink-600">
        Delivering to <span class="font-semibold text-ink-900">{{ estimate.pincode }}</span>
        <span class="text-ink-400"> · {{ estimate.zone }}</span>
      </p>
      <template v-if="estimate.serviceable && estimate.standard">
        <div class="flex gap-3">
          <Truck class="mt-0.5 size-4 shrink-0 text-ink-500" />
          <p>
            <span class="font-semibold text-ink-900">{{ deliveryDay(estimate.standard.date) }}</span>
            <span class="text-ink-600">
              · {{ freeStandard ? 'Free delivery' : `${formatPrice(estimate.standard.fee)} delivery` }}
            </span>
            <span v-if="!freeStandard && estimate.standard.freeAbove" class="block text-[13px] text-ink-500">
              Free on orders over {{ formatPrice(estimate.standard.freeAbove) }}
            </span>
          </p>
        </div>
        <div v-if="estimate.express?.available" class="flex gap-3">
          <Zap class="mt-0.5 size-4 shrink-0 text-accent-600" />
          <p>
            Express by <span class="font-semibold text-ink-900">{{ deliveryDay(estimate.express.date) }}</span>
            <span class="text-ink-600"> · {{ formatPrice(estimate.express.fee) }}</span>
          </p>
        </div>
        <div class="flex gap-3">
          <Banknote class="mt-0.5 size-4 shrink-0 text-ink-500" />
          <p class="text-ink-600">
            {{ codAvailable && estimate.codAvailable ? 'Cash on delivery available' : 'Cash on delivery not available' }}
          </p>
        </div>
      </template>
      <p v-else class="text-danger">Sorry, we don't deliver to this PIN code yet.</p>
    </div>
    <p v-else-if="!editing" class="mt-3 text-sm text-ink-500">Enter a PIN code to see delivery dates.</p>
  </section>
</template>
