<script setup lang="ts">
import { Minus, Plus } from '@lucide/vue'

const props = withDefaults(
  defineProps<{ min?: number; max?: number; disabled?: boolean; size?: 'sm' | 'md'; label?: string }>(),
  { min: 1, max: 10, size: 'md', label: 'Quantity' },
)

const model = defineModel<number>({ required: true })

function step(delta: number) {
  const next = Math.min(props.max, Math.max(props.min, model.value + delta))
  if (next !== model.value) model.value = next
}
</script>

<template>
  <div
    role="group"
    :aria-label="label"
    :class="[
      'inline-flex items-center rounded-lg border border-ink-200 bg-white',
      size === 'sm' ? 'h-8' : 'h-10',
      disabled && 'opacity-60',
    ]"
  >
    <button
      type="button"
      :class="['grid h-full place-items-center rounded-l-lg text-ink-600 hover:bg-ink-50 disabled:opacity-40', size === 'sm' ? 'w-8' : 'w-10']"
      :disabled="disabled || model <= min"
      :aria-label="`Decrease ${label.toLowerCase()}`"
      @click="step(-1)"
    >
      <Minus class="size-4" />
    </button>
    <span
      :class="['tabular min-w-8 text-center font-semibold text-ink-900', size === 'sm' ? 'text-[13px]' : 'text-sm']"
      aria-live="polite"
    >
      {{ model }}
    </span>
    <button
      type="button"
      :class="['grid h-full place-items-center rounded-r-lg text-ink-600 hover:bg-ink-50 disabled:opacity-40', size === 'sm' ? 'w-8' : 'w-10']"
      :disabled="disabled || model >= max"
      :aria-label="`Increase ${label.toLowerCase()}`"
      @click="step(1)"
    >
      <Plus class="size-4" />
    </button>
  </div>
</template>
