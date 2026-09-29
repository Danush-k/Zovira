<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import ZDialog from '@/components/ui/ZDialog.vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZSelect from '@/components/ui/ZSelect.vue'
import ZTextarea from '@/components/ui/ZTextarea.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import { ordersApi, RETURN_REASONS } from '@/services/orders'
import { errorMessage } from '@/services/errors'
import type { OrderItem } from '@/types/order'
import { formatDate, formatPrice } from '@/utils/format'

const props = defineProps<{ orderNumber: string; item: OrderItem | null }>()
const emit = defineEmits<{ done: [] }>()
const open = defineModel<boolean>('open', { default: false })

const form = reactive({ quantity: 1, reason: '', comments: '' })
const error = ref<string | null>(null)
const saving = ref(false)

watch(open, (v) => {
  if (v) Object.assign(form, { quantity: 1, reason: '', comments: '' })
  error.value = null
})

async function submit() {
  if (!props.item) return
  if (!form.reason) {
    error.value = 'Choose a reason for the return'
    return
  }
  saving.value = true
  try {
    await ordersApi.requestReturn(props.orderNumber, props.item.id, { ...form, comments: form.comments || undefined })
    open.value = false
    emit('done')
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <ZDialog v-model:open="open" title="Return an item" size="md">
    <div v-if="item" class="space-y-4">
      <div class="flex items-center gap-3 rounded-lg bg-ink-50 p-3">
        <img v-if="item.imageUrl" :src="item.imageUrl" alt="" class="size-12 rounded-md bg-white object-contain p-1" />
        <div class="min-w-0 text-sm">
          <p class="line-clamp-1 font-medium">{{ item.title }}</p>
          <p class="text-ink-500">Return by {{ formatDate(item.returnableUntil) }}</p>
        </div>
      </div>
      <FormAlert v-if="error" :message="error" />
      <form id="return-form" class="space-y-4" @submit.prevent="submit">
        <ZSelect
          v-if="item.quantity > 1"
          v-model="form.quantity"
          label="Quantity to return"
          :options="Array.from({ length: item.quantity }, (_, i) => ({ value: i + 1, label: String(i + 1) }))"
        />
        <ZSelect v-model="form.reason" label="Reason" placeholder="Select a reason" :options="[...RETURN_REASONS]" />
        <ZTextarea v-model="form.comments" label="Tell us more" optional :maxlength="1000" :rows="3" />
      </form>
      <p class="text-[13px] text-ink-600">
        Estimated refund: <span class="font-semibold">{{ formatPrice(((item.lineTotal - item.couponDiscount) / item.quantity) * form.quantity) }}</span>,
        issued to your original payment method after the seller receives the item.
      </p>
    </div>
    <template #footer>
      <ZButton variant="ghost" @click="open = false">Cancel</ZButton>
      <ZButton type="submit" form="return-form" :loading="saving">Request return</ZButton>
    </template>
  </ZDialog>
</template>
