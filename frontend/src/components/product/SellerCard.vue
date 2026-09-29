<script setup lang="ts">
import { ChevronRight, Store } from '@lucide/vue'
import ZRatingPill from '@/components/ui/ZRatingPill.vue'
import type { SellerSummary } from '@/types/catalog'

defineProps<{ seller: SellerSummary }>()
</script>

<template>
  <RouterLink
    :to="`/store/${seller.slug}`"
    class="group flex items-center gap-3 rounded-xl border border-ink-150 p-4 transition-colors hover:border-ink-200 hover:bg-ink-50/60"
  >
    <span class="grid size-10 shrink-0 place-items-center rounded-lg bg-brand-50 text-brand-700">
      <Store class="size-5" stroke-width="1.75" />
    </span>
    <span class="min-w-0 flex-1">
      <span class="block text-[13px] text-ink-500">Sold by</span>
      <span class="block truncate font-semibold text-ink-900">{{ seller.storeName }}</span>
      <span class="mt-1 flex flex-wrap items-center gap-x-2 text-[13px] text-ink-500">
        <ZRatingPill v-if="seller.ratingCount" :value="seller.ratingAverage" :count="seller.ratingCount" />
        <span v-if="seller.city">Ships from {{ seller.city }}</span>
      </span>
    </span>
    <ChevronRight class="size-4 text-ink-300 transition-transform group-hover:translate-x-0.5" />
  </RouterLink>
</template>
