<script setup lang="ts">
import { computed, ref } from 'vue'

const props = withDefaults(defineProps<{ name: string; src?: string | null; size?: 'sm' | 'md' | 'lg' }>(), {
  size: 'md',
})

const failed = ref(false)
const initials = computed(() =>
  props.name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]!.toUpperCase())
    .join(''),
)
const sizes = { sm: 'size-8 text-xs', md: 'size-10 text-sm', lg: 'size-16 text-xl' }
</script>

<template>
  <span
    :class="[
      'inline-grid shrink-0 place-items-center overflow-hidden rounded-full bg-brand-100 font-semibold text-brand-800',
      sizes[size],
    ]"
  >
    <img v-if="src && !failed" :src="src" :alt="name" class="size-full object-cover" @error="failed = true" />
    <span v-else aria-hidden="true">{{ initials }}</span>
  </span>
</template>
