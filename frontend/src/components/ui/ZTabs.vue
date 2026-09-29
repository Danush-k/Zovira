<script setup lang="ts">
import { ref } from 'vue'

export interface TabItem {
  value: string
  label: string
  count?: number
}

defineProps<{ tabs: TabItem[]; label?: string }>()
const model = defineModel<string>({ required: true })
const tabRefs = ref<HTMLButtonElement[]>([])

function onKeydown(e: KeyboardEvent, index: number, tabs: TabItem[]) {
  const delta = e.key === 'ArrowRight' ? 1 : e.key === 'ArrowLeft' ? -1 : 0
  if (!delta) return
  e.preventDefault()
  const next = (index + delta + tabs.length) % tabs.length
  model.value = tabs[next]!.value
  tabRefs.value[next]?.focus()
}
</script>

<template>
  <div
    role="tablist"
    :aria-label="label"
    class="scrollbar-none flex gap-1 overflow-x-auto border-b border-ink-150"
  >
    <button
      v-for="(tab, i) in tabs"
      :key="tab.value"
      ref="tabRefs"
      type="button"
      role="tab"
      :aria-selected="model === tab.value"
      :tabindex="model === tab.value ? 0 : -1"
      :class="[
        'relative -mb-px flex shrink-0 items-center gap-2 border-b-2 px-3 py-2.5 text-sm font-semibold transition-colors',
        model === tab.value
          ? 'border-brand-700 text-brand-800'
          : 'border-transparent text-ink-500 hover:text-ink-800',
      ]"
      @click="model = tab.value"
      @keydown="onKeydown($event, i, tabs)"
    >
      {{ tab.label }}
      <span
        v-if="tab.count !== undefined"
        :class="[
          'tabular rounded-full px-1.5 text-[11px] leading-5',
          model === tab.value ? 'bg-brand-50 text-brand-800' : 'bg-ink-100 text-ink-600',
        ]"
      >
        {{ tab.count }}
      </span>
    </button>
  </div>
</template>
