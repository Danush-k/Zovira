<script setup lang="ts">
import { computed } from 'vue'
import { AlertTriangle, Trash2 } from '@lucide/vue'
import ZQuantityStepper from '@/components/ui/ZQuantityStepper.vue'
import ZPrice from '@/components/ui/ZPrice.vue'
import type { CartItem } from '@/types/cart'
import { formatPrice } from '@/utils/format'

const props = defineProps<{ item: CartItem; busy?: boolean; saved?: boolean; canSave?: boolean }>()
const emit = defineEmits<{ quantity: [value: number]; remove: []; save: []; move: [] }>()

const notice = computed(() => {
  const i = props.item
  if (i.issues.includes('UNAVAILABLE')) return { tone: 'danger', text: 'This item is no longer available.' }
  if (i.issues.includes('OUT_OF_STOCK')) return { tone: 'danger', text: 'Out of stock. Remove it or save it for later.' }
  if (i.issues.includes('LIMITED_STOCK')) return { tone: 'warning', text: `Only ${i.maxQuantity} left. Reduce the quantity to continue.` }
  if (i.issues.includes('PRICE_INCREASED') && i.priceAtAdd) return { tone: 'warning', text: `Price increased from ${formatPrice(i.priceAtAdd)}.` }
  if (i.issues.includes('PRICE_DROPPED') && i.priceAtAdd) return { tone: 'success', text: `Price dropped from ${formatPrice(i.priceAtAdd)}.` }
  return null
})
const options = computed(() => Object.entries(props.item.options).map(([k, v]) => `${k}: ${v}`).join(' · '))
</script>

<template>
  <article :class="['flex gap-4 py-5', busy && 'opacity-60']">
    <RouterLink :to="`/p/${item.slug}`" class="size-24 shrink-0 overflow-hidden rounded-xl bg-ink-50 sm:size-28">
      <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title" class="size-full object-contain p-2 mix-blend-multiply" />
    </RouterLink>
    <div class="min-w-0 flex-1">
      <div class="flex items-start justify-between gap-4">
        <div class="min-w-0">
          <RouterLink :to="`/p/${item.slug}`" class="line-clamp-2 font-medium text-ink-900 hover:text-brand-800">{{ item.title }}</RouterLink>
          <p v-if="options" class="mt-0.5 text-[13px] text-ink-500">{{ options }}</p>
          <p class="mt-0.5 text-[13px] text-ink-500">Sold by {{ item.sellerName }}</p>
        </div>
        <ZPrice :price="item.unitPrice" :mrp="item.unitMrp" size="sm" class="hidden shrink-0 justify-end sm:flex" />
      </div>
      <p v-if="notice" :class="['mt-2 flex items-center gap-1.5 text-[13px] font-medium', { 'text-danger': notice.tone === 'danger', 'text-warning': notice.tone === 'warning', 'text-success': notice.tone === 'success' }]">
        <AlertTriangle v-if="notice.tone !== 'success'" class="size-3.5" />
        {{ notice.text }}
      </p>
      <ZPrice :price="item.unitPrice" :mrp="item.unitMrp" size="sm" class="mt-2 sm:hidden" />
      <div class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2">
        <ZQuantityStepper v-if="!saved" :model-value="item.quantity" :min="1" :max="Math.max(item.maxQuantity, item.quantity)" size="sm" :disabled="busy || !!item.issues.find((i) => i === 'UNAVAILABLE' || i === 'OUT_OF_STOCK')" @update:model-value="(v) => emit('quantity', v)" />
        <button v-if="canSave && !saved" type="button" class="text-[13px] font-semibold text-brand-700 hover:underline" :disabled="busy" @click="emit('save')">Save for later</button>
        <button v-if="saved" type="button" class="text-[13px] font-semibold text-brand-700 hover:underline" :disabled="busy || !item.purchasable" @click="emit('move')">Move to cart</button>
        <button type="button" class="inline-flex items-center gap-1 text-[13px] font-semibold text-ink-500 hover:text-danger" :disabled="busy" @click="emit('remove')">
          <Trash2 class="size-3.5" /> Remove
        </button>
      </div>
    </div>
  </article>
</template>
