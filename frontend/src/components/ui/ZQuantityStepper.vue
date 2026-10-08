<script setup>
import { Minus, Plus } from '@lucide/vue';
const props = defineProps({
    min: { default: 1 },
    max: { default: 10 },
    disabled: { type: Boolean, default: false },
    size: { default: 'md' },
    label: { default: 'Quantity' }
});
const model = defineModel({ required: true });
function step(delta) {
    const next = Math.min(props.max, Math.max(props.min, model.value + delta));
    if (next !== model.value)
        model.value = next;
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
