<script setup lang="ts">
import { computed } from 'vue'

/** Horizontal magnitude comparison for named rows. Single hue; value labelled at the bar tip. */
export interface BarRow {
  key: string | number
  label: string
  value: number
  meta?: string
  imageUrl?: string | null
  to?: string
}

const props = defineProps<{ rows: BarRow[]; format: (value: number) => string }>()
const max = computed(() => Math.max(...props.rows.map((r) => r.value), 1))
</script>

<template>
  <ul class="space-y-3">
    <li v-for="row in rows" :key="row.key" class="flex items-center gap-3">
      <span v-if="row.imageUrl !== undefined" class="size-10 shrink-0 overflow-hidden rounded-lg bg-ink-50">
        <img v-if="row.imageUrl" :src="row.imageUrl" alt="" class="size-full object-contain p-1 mix-blend-multiply" />
      </span>
      <span class="min-w-0 flex-1">
        <span class="flex items-baseline justify-between gap-3">
          <component :is="row.to ? 'RouterLink' : 'span'" :to="row.to" class="line-clamp-1 text-sm font-medium text-ink-900">
            {{ row.label }}
          </component>
          <span class="tabular shrink-0 text-sm font-semibold text-ink-900">{{ format(row.value) }}</span>
        </span>
        <span class="mt-1.5 block h-2 rounded-full bg-ink-100">
          <span
            class="block h-full rounded-full bg-brand-600"
            :style="{ width: `${Math.max(4, (row.value / max) * 100)}%` }"
          />
        </span>
        <span v-if="row.meta" class="mt-1 block text-xs text-ink-500">{{ row.meta }}</span>
      </span>
    </li>
  </ul>
</template>
