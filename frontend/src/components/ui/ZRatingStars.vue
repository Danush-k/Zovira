<script setup lang="ts">
import { computed } from 'vue'
import { Star } from '@lucide/vue'

const props = withDefaults(defineProps<{ value: number; size?: 'sm' | 'md' | 'lg' }>(), { size: 'md' })

const sizes = { sm: 'size-3.5', md: 'size-4', lg: 'size-5' }
const fills = computed(() =>
  Array.from({ length: 5 }, (_, i) => Math.max(0, Math.min(1, props.value - i)) * 100),
)
</script>

<template>
  <span class="inline-flex items-center gap-0.5" role="img" :aria-label="`Rated ${value.toFixed(1)} out of 5`">
    <span v-for="(fill, i) in fills" :key="i" class="relative inline-block">
      <Star :class="[sizes[size], 'fill-ink-150 text-ink-150']" stroke-width="1.5" />
      <span class="absolute inset-0 overflow-hidden" :style="{ width: `${fill}%` }">
        <Star :class="[sizes[size], 'fill-star text-star']" stroke-width="1.5" />
      </span>
    </span>
  </span>
</template>
