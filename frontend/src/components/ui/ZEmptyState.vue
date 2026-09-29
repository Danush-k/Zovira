<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(defineProps<{ icon?: Component; title: string; description?: string; compact?: boolean }>(), {
  compact: false,
})
</script>

<template>
  <div :class="['flex flex-col items-center text-center', compact ? 'py-8' : 'py-16']">
    <div
      v-if="icon"
      class="relative mb-5 grid size-16 place-items-center rounded-2xl bg-brand-50 text-brand-700 ring-8 ring-brand-50/50"
    >
      <component :is="icon" class="size-7" stroke-width="1.75" aria-hidden="true" />
    </div>
    <h3 class="text-base font-semibold text-ink-900">{{ title }}</h3>
    <p v-if="description" class="mt-1.5 max-w-sm text-sm text-ink-500">{{ description }}</p>
    <div v-if="$slots.default" class="mt-6 flex flex-wrap justify-center gap-2">
      <slot />
    </div>
  </div>
</template>
