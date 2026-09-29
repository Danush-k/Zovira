<script setup lang="ts">
import { computed, ref } from 'vue'

/**
 * Single-series trend over time. One hue (brand), 2px line, 10% area wash, hairline grid,
 * crosshair tooltip on hover or keyboard focus, and a table view for screen readers.
 */
export interface AreaPoint {
  label: string
  value: number
  secondary?: number
}

const props = withDefaults(
  defineProps<{
    points: AreaPoint[]
    format?: (value: number) => string
    secondaryLabel?: string
    valueLabel?: string
    height?: number
  }>(),
  { height: 220, valueLabel: 'Value', format: (v: number) => String(v) },
)

const W = 720
const PAD = { top: 16, right: 16, bottom: 28, left: 52 }
const active = ref<number | null>(null)
const svg = ref<SVGSVGElement | null>(null)

const inner = computed(() => ({ w: W - PAD.left - PAD.right, h: props.height - PAD.top - PAD.bottom }))
const max = computed(() => {
  const peak = Math.max(...props.points.map((p) => p.value), 1)
  const magnitude = 10 ** Math.floor(Math.log10(peak))
  return Math.ceil(peak / magnitude) * magnitude
})
const ticks = computed(() => [0, 0.25, 0.5, 0.75, 1].map((f) => max.value * f))

const x = (i: number) => PAD.left + (props.points.length === 1 ? inner.value.w / 2 : (i / (props.points.length - 1)) * inner.value.w)
const y = (v: number) => PAD.top + inner.value.h - (v / max.value) * inner.value.h

const line = computed(() => props.points.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(p.value).toFixed(1)}`).join(' '))
const area = computed(() =>
  props.points.length
    ? `${line.value} L${x(props.points.length - 1).toFixed(1)},${(PAD.top + inner.value.h).toFixed(1)} L${x(0).toFixed(1)},${(PAD.top + inner.value.h).toFixed(1)} Z`
    : '',
)

/** Labels every ~6th point so the axis never collides. */
const axisLabels = computed(() => {
  const stride = Math.max(1, Math.ceil(props.points.length / 6))
  return props.points.map((p, i) => ({ ...p, i })).filter((_, i) => i % stride === 0 || i === props.points.length - 1)
})

function onMove(event: PointerEvent) {
  const rect = svg.value?.getBoundingClientRect()
  if (!rect || !props.points.length) return
  const ratio = (event.clientX - rect.left) / rect.width
  const index = Math.round(((ratio * W - PAD.left) / inner.value.w) * (props.points.length - 1))
  active.value = Math.min(props.points.length - 1, Math.max(0, index))
}

function onKey(event: KeyboardEvent) {
  if (!['ArrowLeft', 'ArrowRight'].includes(event.key)) return
  event.preventDefault()
  const next = (active.value ?? 0) + (event.key === 'ArrowRight' ? 1 : -1)
  active.value = Math.min(props.points.length - 1, Math.max(0, next))
}

const point = computed(() => (active.value === null ? null : props.points[active.value]))
</script>

<template>
  <figure class="relative">
    <svg
      ref="svg"
      :viewBox="`0 0 ${W} ${height}`"
      class="w-full touch-none"
      role="img"
      :aria-label="`${valueLabel} over the last ${points.length} days`"
      tabindex="0"
      @pointermove="onMove"
      @pointerleave="active = null"
      @keydown="onKey"
      @focus="active = points.length - 1"
      @blur="active = null"
    >
      <line
        v-for="(t, i) in ticks"
        :key="i"
        :x1="PAD.left"
        :x2="W - PAD.right"
        :y1="y(t)"
        :y2="y(t)"
        stroke="var(--color-ink-150)"
        stroke-width="1"
      />
      <text
        v-for="(t, i) in ticks"
        :key="`t${i}`"
        :x="PAD.left - 8"
        :y="y(t) + 4"
        text-anchor="end"
        class="tabular fill-ink-400 text-[11px]"
      >
        {{ format(t) }}
      </text>
      <text
        v-for="p in axisLabels"
        :key="`x${p.i}`"
        :x="x(p.i)"
        :y="height - 8"
        text-anchor="middle"
        class="fill-ink-400 text-[11px]"
      >
        {{ p.label }}
      </text>

      <path :d="area" fill="var(--color-brand-600)" fill-opacity="0.1" />
      <path :d="line" fill="none" stroke="var(--color-brand-600)" stroke-width="2" stroke-linejoin="round" stroke-linecap="round" />

      <template v-if="active !== null && point">
        <line :x1="x(active)" :x2="x(active)" :y1="PAD.top" :y2="PAD.top + inner.h" stroke="var(--color-ink-300)" stroke-width="1" />
        <circle :cx="x(active)" :cy="y(point.value)" r="5" fill="var(--color-brand-600)" stroke="white" stroke-width="2" />
      </template>
      <circle
        v-else-if="points.length"
        :cx="x(points.length - 1)"
        :cy="y(points[points.length - 1]!.value)"
        r="4"
        fill="var(--color-brand-600)"
        stroke="white"
        stroke-width="2"
      />
    </svg>

    <div
      v-if="point"
      class="pointer-events-none absolute top-3 rounded-lg border border-ink-150 bg-white px-3 py-2 text-[13px] shadow-raised"
      :style="{ left: `min(calc(${((x(active!) / W) * 100).toFixed(1)}% + 12px), calc(100% - 160px))` }"
      role="status"
    >
      <p class="font-semibold text-ink-900">{{ point.label }}</p>
      <p class="tabular text-ink-700">{{ valueLabel }}: {{ format(point.value) }}</p>
      <p v-if="point.secondary !== undefined" class="tabular text-ink-500">{{ secondaryLabel }}: {{ point.secondary }}</p>
    </div>

    <figcaption class="sr-only">
      <table>
        <caption>{{ valueLabel }} by day</caption>
        <thead><tr><th>Day</th><th>{{ valueLabel }}</th></tr></thead>
        <tbody>
          <tr v-for="p in points" :key="p.label"><td>{{ p.label }}</td><td>{{ format(p.value) }}</td></tr>
        </tbody>
      </table>
    </figcaption>
  </figure>
</template>
