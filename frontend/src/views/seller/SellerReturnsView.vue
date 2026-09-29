<script setup lang="ts">
import { ref, watch } from 'vue'
import { RotateCcw } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZSwitch from '@/components/ui/ZSwitch.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import OrderStatusBadge from '@/components/order/OrderStatusBadge.vue'
import { sellerApi } from '@/services/seller'
import { RETURN_REASONS } from '@/services/orders'
import { errorMessage } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useToastStore } from '@/stores/toast'
import { formatDate, formatPrice } from '@/utils/format'

const openOnly = ref(true)
const toast = useToastStore()
const busy = ref<number | null>(null)
const { data, loading, error, execute } = useAsync((open: boolean) => sellerApi.returns(open))
watch(openOnly, (v) => void execute(v), { immediate: true })

const reasonLabel = (r: string) => RETURN_REASONS.find((x) => x.value === r)?.label ?? r

async function decide(id: number, decision: 'APPROVE' | 'REJECT' | 'RECEIVE') {
  busy.value = id
  try {
    await sellerApi.decideReturn(id, decision)
    await execute(openOnly.value)
    toast.success(decision === 'RECEIVE' ? 'Return received and refunded' : decision === 'APPROVE' ? 'Return approved' : 'Return rejected')
  } catch (e) {
    toast.error("Couldn't update the return", errorMessage(e))
  } finally {
    busy.value = null
  }
}
</script>

<template>
  <div>
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold tracking-tight">Returns</h1>
        <p class="mt-1 text-sm text-ink-500">Approve a return, then mark it received to restock and refund.</p>
      </div>
      <ZSwitch v-model="openOnly" label="Open only" />
    </div>

    <ZErrorState v-if="error" :error="error" @retry="execute(openOnly)" />
    <div v-else-if="loading && !data" class="mt-5 space-y-3"><div v-for="i in 2" :key="i" class="skeleton h-24 rounded-xl" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5">
      <ZEmptyState :icon="RotateCcw" title="No returns" description="Return requests for your products will appear here." compact />
    </div>
    <ul v-else class="mt-5 space-y-3">
      <li v-for="r in data.content" :key="r.id" class="surface flex flex-wrap items-center gap-4 p-4">
        <img v-if="r.imageUrl" :src="r.imageUrl" alt="" class="size-16 rounded-lg bg-ink-50 object-contain p-1.5 mix-blend-multiply" />
        <div class="min-w-0 flex-1 text-sm">
          <p class="line-clamp-1 font-medium">{{ r.itemTitle }}</p>
          <p class="text-ink-500">{{ r.returnNumber }} · Order {{ r.orderNumber }} · {{ r.customerName }}</p>
          <p class="text-ink-600">{{ reasonLabel(r.reason) }}<template v-if="r.comments">: “{{ r.comments }}”</template></p>
          <p class="text-ink-500">Requested {{ formatDate(r.createdAt) }} · Qty {{ r.quantity }}</p>
        </div>
        <div class="text-right">
          <OrderStatusBadge :status="r.status" />
          <p class="tabular mt-1 text-sm font-semibold">{{ formatPrice(r.refundAmount) }}</p>
          <div class="mt-2 flex justify-end gap-1">
            <template v-if="r.status === 'REQUESTED'">
              <ZButton size="sm" :loading="busy === r.id" @click="decide(r.id, 'APPROVE')">Approve</ZButton>
              <ZButton size="sm" variant="ghost" :disabled="busy === r.id" @click="decide(r.id, 'REJECT')">Reject</ZButton>
            </template>
            <ZButton v-else-if="r.status === 'APPROVED'" size="sm" :loading="busy === r.id" @click="decide(r.id, 'RECEIVE')">Mark received</ZButton>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>
