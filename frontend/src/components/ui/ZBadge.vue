<script setup lang="ts">
export type BadgeTone = 'neutral' | 'brand' | 'accent' | 'success' | 'warning' | 'danger' | 'info' | 'deal' | 'dark'

withDefaults(defineProps<{ tone?: BadgeTone; size?: 'sm' | 'md'; dot?: boolean }>(), {
  tone: 'neutral',
  size: 'md',
})

const tones: Record<BadgeTone, string> = {
  neutral: 'bg-ink-100 text-ink-700',
  brand: 'bg-brand-50 text-brand-800',
  accent: 'bg-accent-50 text-accent-700',
  success: 'bg-success-soft text-success',
  warning: 'bg-warning-soft text-warning',
  danger: 'bg-danger-soft text-danger',
  info: 'bg-info-soft text-info',
  deal: 'bg-deal text-white',
  dark: 'bg-ink-900 text-white',
}
</script>

<template>
  <span
    :class="[
      'inline-flex items-center gap-1.5 rounded-md font-semibold whitespace-nowrap',
      size === 'sm' ? 'px-1.5 py-0.5 text-[11px]' : 'px-2 py-0.5 text-xs',
      tones[tone],
    ]"
  >
    <span v-if="dot" class="size-1.5 rounded-full bg-current" aria-hidden="true" />
    <slot />
  </span>
</template>
