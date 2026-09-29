<script setup lang="ts">
import { computed } from 'vue'
import { RouterLink, type RouteLocationRaw } from 'vue-router'
import ZSpinner from './ZSpinner.vue'

export type ButtonVariant = 'primary' | 'accent' | 'secondary' | 'outline' | 'ghost' | 'danger' | 'link'
export type ButtonSize = 'sm' | 'md' | 'lg'

const props = withDefaults(
  defineProps<{
    variant?: ButtonVariant
    size?: ButtonSize
    type?: 'button' | 'submit' | 'reset'
    to?: RouteLocationRaw
    href?: string
    loading?: boolean
    disabled?: boolean
    block?: boolean
    iconOnly?: boolean
  }>(),
  { variant: 'primary', size: 'md', type: 'button' },
)

const variantClasses: Record<ButtonVariant, string> = {
  primary: 'bg-brand-700 text-white shadow-[inset_0_1px_0_rgb(255_255_255/0.08)] hover:bg-brand-800',
  accent: 'bg-accent-500 text-ink-950 hover:bg-accent-400',
  secondary: 'bg-brand-50 text-brand-800 hover:bg-brand-100',
  outline: 'border border-ink-200 bg-white text-ink-800 hover:border-ink-300 hover:bg-ink-50',
  ghost: 'text-ink-700 hover:bg-ink-100 hover:text-ink-900',
  danger: 'bg-danger text-white hover:bg-danger/90',
  link: 'text-brand-700 underline-offset-4 hover:underline',
}

const sizeClasses: Record<ButtonSize, string> = {
  sm: 'h-8 px-3 text-[13px] gap-1.5',
  md: 'h-10 px-4 text-sm gap-2',
  lg: 'h-12 px-6 text-[15px] gap-2',
}

const iconOnlySizes: Record<ButtonSize, string> = {
  sm: 'size-8',
  md: 'size-10',
  lg: 'size-12',
}

const classes = computed(() => [
  'relative inline-flex shrink-0 items-center justify-center whitespace-nowrap rounded-lg font-semibold select-none',
  'transition-[background-color,border-color,color,box-shadow,transform] duration-150 active:translate-y-px',
  'disabled:pointer-events-none disabled:opacity-50 aria-disabled:pointer-events-none aria-disabled:opacity-50',
  variantClasses[props.variant],
  props.variant === 'link' ? 'h-auto px-0' : props.iconOnly ? iconOnlySizes[props.size] : sizeClasses[props.size],
  props.block && 'w-full',
])
</script>

<template>
  <RouterLink v-if="to" :to="to" :class="classes" :aria-disabled="disabled || undefined">
    <slot />
  </RouterLink>
  <a v-else-if="href" :href="href" :class="classes" :aria-disabled="disabled || undefined">
    <slot />
  </a>
  <button v-else :type="type" :class="classes" :disabled="disabled || loading" :aria-busy="loading || undefined">
    <span v-if="loading" class="absolute inset-0 grid place-items-center">
      <ZSpinner class="size-4" />
    </span>
    <span :class="['inline-flex items-center gap-[inherit]', loading && 'invisible']">
      <slot />
    </span>
  </button>
</template>
