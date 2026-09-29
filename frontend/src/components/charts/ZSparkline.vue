<script setup lang="ts">
import { computed } from 'vue'

/** A 12-point trend indicator for stat tiles. Decorative: the value beside it carries the meaning. */
const props = withDefaults(defineProps<{ points: number[]; width?: number; height?: number }>(), {
  width: 96,
  height: 28,
})

const path = computed(() => {
  const values = props.points.slice(-12)
  if (values.length < 2) return ''
  const max = Math.max(...values)
  const min = Math.min(...values)
  const span = max - min || 1
  const step = props.width / (values.length - 1)
  return values
    .map((v, i) => `${i === 0 ? 'M' : 'L'}${(i * step).toFixed(1)},${(props.height - 2 - ((v - min) / span) * (props.height - 4)).toFixed(1)}`)
    .join(' ')
})
</script>

<template>
  <svg v-if="path" :width="width" :height="height" :viewBox="`0 0 ${width} ${height}`" fill="none" aria-hidden="true">
    <path :d="path" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
  </svg>
</template>
