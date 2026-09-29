<script setup lang="ts">
import ZBadge from '@/components/ui/ZBadge.vue'
import type { CartSummary } from '@/types/cart'
import { formatPrice } from '@/utils/format'

defineProps<{ summary: CartSummary; codFee?: number; title?: string }>()
</script>

<template>
  <section class="surface p-5" aria-labelledby="summary-title">
    <h2 id="summary-title" class="text-base font-semibold text-ink-900">{{ title ?? 'Order summary' }}</h2>
    <dl class="mt-4 space-y-3 text-sm">
      <div class="flex justify-between">
        <dt class="text-ink-600">Price ({{ summary.itemCount }} {{ summary.itemCount === 1 ? 'item' : 'items' }})</dt>
        <dd class="tabular text-ink-900">{{ formatPrice(summary.mrpTotal) }}</dd>
      </div>
      <div v-if="summary.itemDiscount > 0" class="flex justify-between">
        <dt class="text-ink-600">Discount</dt>
        <dd class="tabular text-success">-{{ formatPrice(summary.itemDiscount) }}</dd>
      </div>
      <div v-if="summary.couponApplied" class="flex justify-between">
        <dt class="flex items-center gap-1.5 text-ink-600">Coupon <ZBadge tone="brand" size="sm">{{ summary.couponCode }}</ZBadge></dt>
        <dd class="tabular text-success">-{{ formatPrice(summary.couponDiscount) }}</dd>
      </div>
      <div class="flex justify-between">
        <dt class="text-ink-600">Delivery</dt>
        <dd class="tabular" :class="summary.shippingFee > 0 ? 'text-ink-900' : 'text-success'">
          {{ summary.shippingFee > 0 ? formatPrice(summary.shippingFee) : 'Free' }}
        </dd>
      </div>
      <div v-if="codFee" class="flex justify-between">
        <dt class="text-ink-600">Cash on delivery fee</dt>
        <dd class="tabular text-ink-900">{{ formatPrice(codFee) }}</dd>
      </div>
      <div class="flex justify-between border-t border-ink-150 pt-3 text-base font-semibold">
        <dt>Total</dt>
        <dd class="tabular">{{ formatPrice(summary.total + (codFee ?? 0)) }}</dd>
      </div>
      <p class="text-xs text-ink-500">Includes {{ formatPrice(summary.taxIncluded) }} GST</p>
    </dl>
    <p
      v-if="summary.itemDiscount + (summary.couponApplied ? summary.couponDiscount : 0) > 0"
      class="mt-4 rounded-lg bg-success-soft px-3 py-2 text-[13px] font-semibold text-success"
    >
      You save {{ formatPrice(summary.itemDiscount + (summary.couponApplied ? summary.couponDiscount : 0)) }} on this order
    </p>
    <slot />
  </section>
</template>
