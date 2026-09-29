<script setup lang="ts">
import { computed } from 'vue'
import { ChevronLeft, ChevronRight } from '@lucide/vue'

const props = defineProps<{ page: number; totalPages: number }>()
const emit = defineEmits<{ 'update:page': [page: number] }>()

/** Zero-based page indexes to render, with -1 marking an ellipsis. */
const pages = computed<number[]>(() => {
  const total = props.totalPages
  const current = props.page
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const set = new Set([0, total - 1, current - 1, current, current + 1].filter((p) => p >= 0 && p < total))
  const sorted = [...set].sort((a, b) => a - b)
  const result: number[] = []
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1]! > 1) result.push(-1)
    result.push(p)
  })
  return result
})

function go(p: number) {
  if (p >= 0 && p < props.totalPages && p !== props.page) emit('update:page', p)
}
</script>

<template>
  <nav v-if="totalPages > 1" class="flex items-center justify-center gap-1" aria-label="Pagination">
    <button
      type="button"
      class="grid size-9 place-items-center rounded-lg text-ink-600 hover:bg-ink-100 disabled:opacity-40"
      :disabled="page === 0"
      aria-label="Previous page"
      @click="go(page - 1)"
    >
      <ChevronLeft class="size-4" />
    </button>
    <template v-for="(p, i) in pages" :key="`${p}-${i}`">
      <span v-if="p === -1" class="px-1 text-ink-400" aria-hidden="true">...</span>
      <button
        v-else
        type="button"
        :aria-current="p === page ? 'page' : undefined"
        :class="[
          'tabular h-9 min-w-9 rounded-lg px-2 text-sm font-semibold transition-colors',
          p === page ? 'bg-ink-900 text-white' : 'text-ink-700 hover:bg-ink-100',
        ]"
        @click="go(p)"
      >
        {{ p + 1 }}
      </button>
    </template>
    <button
      type="button"
      class="grid size-9 place-items-center rounded-lg text-ink-600 hover:bg-ink-100 disabled:opacity-40"
      :disabled="page >= totalPages - 1"
      aria-label="Next page"
      @click="go(page + 1)"
    >
      <ChevronRight class="size-4" />
    </button>
  </nav>
</template>
