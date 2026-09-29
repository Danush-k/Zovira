<script setup lang="ts">
import { computed } from 'vue'
import { discountPercent, formatPrice } from '@/utils/format'

const props = withDefaults(
  defineProps<{
    price: number | string
    mrp?: number | string | null
    size?: 'sm' | 'md' | 'lg' | 'xl'
    showDiscount?: boolean
  }>(),
  { size: 'md', showDiscount: true },
)

const priceNum = computed(() => Number(props.price))
const mrpNum = computed(() => (props.mrp == null ? 0 : Number(props.mrp)))
const off = computed(() => discountPercent(priceNum.value, mrpNum.value))

const sizes = {
  sm: { price: 'text-[15px]', mrp: 'text-xs', off: 'text-xs' },
  md: { price: 'text-lg', mrp: 'text-[13px]', off: 'text-[13px]' },
  lg: { price: 'text-2xl', mrp: 'text-sm', off: 'text-sm' },
  xl: { price: 'text-[2rem] leading-none', mrp: 'text-[15px]', off: 'text-[15px]' },
}
</script>

<template>
  <div class="tabular flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
    <span :class="['font-bold tracking-tight text-ink-900', sizes[size].price]">
      <span class="sr-only">Price:</span>{{ formatPrice(priceNum) }}
    </span>
    <template v-if="off > 0">
      <span :class="['text-ink-400 line-through', sizes[size].mrp]">
        <span class="sr-only">Original price:</span>{{ formatPrice(mrpNum) }}
      </span>
      <span v-if="showDiscount" :class="['font-semibold text-deal', sizes[size].off]">{{ off }}% off</span>
    </template>
  </div>
</template>
