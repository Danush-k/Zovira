<script setup lang="ts">
import { computed } from 'vue'
import ZBadge, { type BadgeTone } from '@/components/ui/ZBadge.vue'

const props = defineProps<{ status: string }>()

const map: Record<string, { label: string; tone: BadgeTone }> = {
  PENDING_PAYMENT: { label: 'Awaiting payment', tone: 'warning' },
  PLACED: { label: 'Placed', tone: 'info' },
  CONFIRMED: { label: 'Confirmed', tone: 'info' },
  PROCESSING: { label: 'Processing', tone: 'info' },
  SHIPPED: { label: 'Shipped', tone: 'brand' },
  OUT_FOR_DELIVERY: { label: 'Out for delivery', tone: 'accent' },
  DELIVERED: { label: 'Delivered', tone: 'success' },
  CANCELLED: { label: 'Cancelled', tone: 'neutral' },
  PAYMENT_FAILED: { label: 'Payment failed', tone: 'danger' },
  RETURNED: { label: 'Returned', tone: 'neutral' },
  REQUESTED: { label: 'Return requested', tone: 'warning' },
  APPROVED: { label: 'Approved', tone: 'info' },
  REJECTED: { label: 'Rejected', tone: 'danger' },
  PICKED_UP: { label: 'Picked up', tone: 'brand' },
  REFUNDED: { label: 'Refunded', tone: 'success' },
}
const entry = computed(() => map[props.status] ?? { label: props.status, tone: 'neutral' as BadgeTone })
</script>

<template>
  <ZBadge :tone="entry.tone" dot>{{ entry.label }}</ZBadge>
</template>
