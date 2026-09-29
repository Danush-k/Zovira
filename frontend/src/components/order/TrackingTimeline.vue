<script setup lang="ts">
import { computed } from 'vue'
import { Check, X } from '@lucide/vue'
import { formatDateTime } from '@/utils/format'
import type { OrderDetail } from '@/types/order'

const props = defineProps<{ order: OrderDetail }>()

const PATH = [
  { status: 'PLACED', label: 'Order placed' },
  { status: 'CONFIRMED', label: 'Payment confirmed' },
  { status: 'PROCESSING', label: 'Seller preparing package' },
  { status: 'SHIPPED', label: 'Shipped' },
  { status: 'OUT_FOR_DELIVERY', label: 'Out for delivery' },
  { status: 'DELIVERED', label: 'Delivered' },
]

const halted = computed(() => ['CANCELLED', 'PAYMENT_FAILED'].includes(props.order.status))

const steps = computed(() => {
  const reached = new Map(props.order.timeline.map((t) => [t.status, t]))
  const currentIndex = PATH.reduce((acc, s, i) => (reached.has(s.status) ? i : acc), -1)
  const list = PATH.map((s, i) => ({
    ...s,
    label: s.status === 'CONFIRMED' && props.order.paymentMethod === 'COD' ? 'Order confirmed' : s.label,
    at: reached.get(s.status)?.at ?? null,
    note: reached.get(s.status)?.note ?? null,
    state: i < currentIndex ? 'done' : i === currentIndex ? (i === PATH.length - 1 ? 'done' : 'current') : 'upcoming',
  }))
  if (halted.value) {
    const stop = props.order.timeline.find((t) => t.status === props.order.status)
    return [...list.slice(0, currentIndex + 1).map((s) => ({ ...s, state: 'done' })), {
      status: props.order.status,
      label: props.order.status === 'CANCELLED' ? 'Cancelled' : 'Payment not completed',
      at: stop?.at ?? null,
      note: stop?.note ?? null,
      state: 'halted',
    }]
  }
  return list
})
</script>

<template>
  <ol class="relative">
    <li v-for="(step, i) in steps" :key="step.status" class="relative flex gap-4 pb-6 last:pb-0">
      <span
        v-if="i < steps.length - 1"
        :class="['absolute top-7 left-[13px] h-[calc(100%-1.25rem)] w-0.5', step.state === 'done' ? 'bg-success' : 'bg-ink-150']"
        aria-hidden="true"
      />
      <span
        :class="[
          'relative z-10 grid size-7 shrink-0 place-items-center rounded-full border-2',
          step.state === 'done' && 'border-success bg-success text-white',
          step.state === 'current' && 'border-brand-600 bg-white',
          step.state === 'upcoming' && 'border-ink-200 bg-white',
          step.state === 'halted' && 'border-danger bg-danger text-white',
        ]"
      >
        <Check v-if="step.state === 'done'" class="size-4" stroke-width="3" />
        <X v-else-if="step.state === 'halted'" class="size-4" stroke-width="3" />
        <span v-else-if="step.state === 'current'" class="size-2.5 animate-pulse rounded-full bg-brand-600" />
      </span>
      <div class="-mt-0.5 min-w-0">
        <p :class="['text-sm font-semibold', step.state === 'upcoming' ? 'text-ink-400' : 'text-ink-900']">
          {{ step.label }}
          <span v-if="step.state === 'current'" class="ml-1 text-xs font-medium text-brand-700">In progress</span>
        </p>
        <p v-if="step.at" class="text-[13px] text-ink-500">{{ formatDateTime(step.at) }}</p>
        <p v-if="step.note && step.state !== 'upcoming'" class="text-[13px] text-ink-600">{{ step.note }}</p>
      </div>
    </li>
  </ol>
</template>
