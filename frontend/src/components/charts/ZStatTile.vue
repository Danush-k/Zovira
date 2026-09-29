<script setup lang="ts">
import { computed, type Component } from 'vue'
import { TrendingDown, TrendingUp } from '@lucide/vue'
import ZSparkline from './ZSparkline.vue'

/**
 * Stat tile: label, value, optional delta against a named period, optional sparkline.
 * `upIsGood` decides the delta's colour so "returns up" is not shown as success.
 */
const props = withDefaults(
  defineProps<{
    label: string
    value: string
    previous?: number | null
    current?: number | null
    comparisonLabel?: string
    trend?: number[]
    icon?: Component
    upIsGood?: boolean
    hint?: string
  }>(),
  { upIsGood: true, comparisonLabel: 'vs previous 30 days' },
)

const delta = computed(() => {
  // With no baseline there is no honest percentage to show; the hint carries the context instead.
  if (props.previous == null || props.current == null || props.previous === 0) return null
  return ((props.current - props.previous) / Math.abs(props.previous)) * 100
})
const positive = computed(() => (delta.value ?? 0) >= 0)
const good = computed(() => positive.value === props.upIsGood)
</script>

<template>
  <div class="surface p-4">
    <div class="flex items-start justify-between gap-3">
      <p class="text-[13px] font-medium text-ink-500">{{ label }}</p>
      <component :is="icon" v-if="icon" class="size-4 shrink-0 text-ink-400" stroke-width="1.75" />
    </div>
    <p class="mt-1.5 text-2xl font-semibold tracking-tight text-ink-900">{{ value }}</p>
    <div class="mt-2 flex items-end justify-between gap-3">
      <p v-if="delta !== null" class="flex items-center gap-1 text-[13px]">
        <component :is="positive ? TrendingUp : TrendingDown" :class="['size-3.5', good ? 'text-success' : 'text-deal']" />
        <span :class="['font-semibold', good ? 'text-success' : 'text-deal']">
          {{ positive ? '+' : '' }}{{ delta.toFixed(0) }}%
        </span>
        <span class="text-ink-500">{{ comparisonLabel }}</span>
      </p>
      <p v-else class="text-[13px] text-ink-500">{{ hint ?? (previous === 0 && current ? 'First sales in this period' : '') }}</p>
      <ZSparkline v-if="trend?.length" :points="trend" class="shrink-0 text-brand-400" />
    </div>
  </div>
</template>
