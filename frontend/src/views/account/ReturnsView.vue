<script setup lang="ts">
import { RotateCcw } from '@lucide/vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import OrderStatusBadge from '@/components/order/OrderStatusBadge.vue'
import { ordersApi, RETURN_REASONS } from '@/services/orders'
import { useAsync } from '@/composables/useAsync'
import { formatDate, formatPrice } from '@/utils/format'

const { data, loading, error, execute } = useAsync(() => ordersApi.returns(), { immediate: true })
const reasonLabel = (r: string) => RETURN_REASONS.find((x) => x.value === r)?.label ?? r
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold tracking-tight">Returns and refunds</h1>
    <p class="mt-1 text-ink-500">Start a return from any delivered order within its return window.</p>
    <ZErrorState v-if="error" :error="error" @retry="execute" />
    <div v-else-if="loading && !data" class="mt-5 space-y-3"><div v-for="i in 2" :key="i" class="skeleton h-24 rounded-xl" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5">
      <ZEmptyState :icon="RotateCcw" title="No returns" description="Items you return will appear here with their refund status." />
    </div>
    <ul v-else class="mt-5 space-y-3">
      <li v-for="r in data.content" :key="r.id" class="surface flex flex-wrap items-center gap-4 p-4">
        <img v-if="r.imageUrl" :src="r.imageUrl" alt="" class="size-16 rounded-lg bg-ink-50 object-contain p-1.5 mix-blend-multiply" />
        <div class="min-w-0 flex-1 text-sm">
          <p class="line-clamp-1 font-medium">{{ r.itemTitle }}</p>
          <p class="text-ink-500">
            {{ r.returnNumber }} · Order <RouterLink :to="`/account/orders/${r.orderNumber}`" class="link">{{ r.orderNumber }}</RouterLink> · {{ reasonLabel(r.reason) }}
          </p>
          <p class="text-ink-500">Requested {{ formatDate(r.createdAt) }}<template v-if="r.resolutionNote"> · {{ r.resolutionNote }}</template></p>
        </div>
        <div class="text-right">
          <OrderStatusBadge :status="r.status" />
          <p class="tabular mt-1 text-sm font-semibold">{{ formatPrice(r.refundAmount) }}</p>
        </div>
      </li>
    </ul>
  </div>
</template>
