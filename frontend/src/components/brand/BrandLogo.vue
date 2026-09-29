<script setup lang="ts">
import BrandMark from './BrandMark.vue'

withDefaults(defineProps<{ inverse?: boolean; compact?: boolean; size?: 'sm' | 'md' | 'lg' }>(), { size: 'md' })

const markSizes = { sm: 'size-7', md: 'size-8', lg: 'size-10' }
const textSizes = { sm: 'text-[19px]', md: 'text-[22px]', lg: 'text-[28px]' }
</script>

<template>
  <span class="inline-flex items-center gap-2" role="img" aria-label="Zovira">
    <BrandMark :class="markSizes[size]" :inverse="inverse" />
    <span
      v-if="!compact"
      :class="['font-[780] leading-none tracking-[-0.035em]', textSizes[size], inverse ? 'text-white' : 'text-ink-900']"
      aria-hidden="true"
    >
      zovira
    </span>
  </span>
</template>
