<script setup lang="ts">
import { useId } from 'vue'

defineProps<{
  label?: string
  description?: string
  disabled?: boolean
}>()

const model = defineModel<boolean>({ default: false })
const id = useId()
</script>

<template>
  <div class="flex items-start justify-between gap-4">
    <div v-if="label" class="min-w-0">
      <label :for="id" class="text-sm font-medium text-ink-800">{{ label }}</label>
      <p v-if="description" class="mt-0.5 text-[13px] text-ink-500">{{ description }}</p>
    </div>
    <button
      :id="id"
      type="button"
      role="switch"
      :aria-checked="model"
      :disabled="disabled"
      :class="[
        'relative inline-flex h-6 w-11 shrink-0 items-center rounded-full transition-colors duration-200 disabled:opacity-50',
        model ? 'bg-brand-700' : 'bg-ink-200',
      ]"
      @click="model = !model"
    >
      <span
        :class="[
          'inline-block size-5 rounded-full bg-white shadow-sm transition-transform duration-200',
          model ? 'translate-x-[22px]' : 'translate-x-0.5',
        ]"
      />
    </button>
  </div>
</template>
