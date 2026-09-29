<script setup lang="ts">
import { computed, ref } from 'vue'
import { FlaskConical, Lock } from '@lucide/vue'
import ZDialog from '@/components/ui/ZDialog.vue'
import ZButton from '@/components/ui/ZButton.vue'
import type { PaymentIntent, PaymentMethod } from '@/types/order'
import { formatPrice } from '@/utils/format'

const props = defineProps<{ intent: PaymentIntent | null; method: PaymentMethod }>()
const emit = defineEmits<{ complete: [success: boolean]; dismiss: [] }>()
const busy = ref<'pay' | 'fail' | null>(null)

const open = computed({
  get: () => !!props.intent,
  set: (v) => {
    if (!v && !busy.value) emit('dismiss')
  },
})

const methodLabel: Record<PaymentMethod, string> = {
  UPI: 'UPI',
  CARD: 'Credit / debit card',
  NETBANKING: 'Net banking',
  WALLET: 'Wallet',
  COD: 'Cash on delivery',
}

function finish(success: boolean) {
  busy.value = success ? 'pay' : 'fail'
  emit('complete', success)
}
</script>

<template>
  <ZDialog v-model:open="open" title="Test payment" size="sm" :dismissible="!busy">
    <div class="flex items-start gap-3 rounded-lg border border-accent-300/60 bg-accent-50 p-3 text-[13px] text-ink-700">
      <FlaskConical class="mt-0.5 size-4 shrink-0 text-accent-700" />
      <p>
        This store is running in <strong>test mode</strong>. No money will be charged. Choose an outcome to simulate the
        payment provider's response.
      </p>
    </div>
    <dl v-if="intent" class="mt-5 space-y-2 text-sm">
      <div class="flex justify-between"><dt class="text-ink-500">Order</dt><dd class="font-mono">{{ intent.orderNumber }}</dd></div>
      <div class="flex justify-between"><dt class="text-ink-500">Method</dt><dd>{{ methodLabel[method] }}</dd></div>
      <div class="flex justify-between text-base font-semibold"><dt>Amount</dt><dd class="tabular">{{ formatPrice(intent.amount) }}</dd></div>
    </dl>
    <template #footer>
      <ZButton variant="ghost" :loading="busy === 'fail'" :disabled="!!busy" @click="finish(false)">Simulate failure</ZButton>
      <ZButton :loading="busy === 'pay'" :disabled="!!busy" @click="finish(true)">
        <Lock class="size-4" />
        Pay {{ intent ? formatPrice(intent.amount) : '' }}
      </ZButton>
    </template>
  </ZDialog>
</template>
