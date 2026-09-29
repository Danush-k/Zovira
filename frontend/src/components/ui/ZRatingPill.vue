<script setup lang="ts">
import { computed } from 'vue'
import { Star } from '@lucide/vue'
import { formatCompact } from '@/utils/format'

const props = defineProps<{ value: number | string; count?: number }>()
const rating = computed(() => Number(props.value))
const tone = computed(() =>
  rating.value >= 4 ? 'bg-brand-700' : rating.value >= 3 ? 'bg-brand-500' : rating.value > 0 ? 'bg-accent-600' : 'bg-ink-300',
)
</script>

<template>
  <span class="inline-flex items-center gap-1.5 text-[13px]">
    <span
      :class="['tabular inline-flex items-center gap-0.5 rounded-[5px] px-1.5 py-px text-xs font-bold text-white', tone]"
      :aria-label="`Rated ${rating.toFixed(1)} out of 5`"
    >
      {{ rating.toFixed(1) }}
      <Star class="size-3 fill-current" stroke-width="0" aria-hidden="true" />
    </span>
    <span v-if="count !== undefined" class="tabular text-ink-500">({{ formatCompact(count) }})</span>
  </span>
</template>
