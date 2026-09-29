<script setup lang="ts">
defineProps<{ inverse?: boolean }>()
</script>

<template>
  <svg viewBox="0 0 32 32" fill="none" aria-hidden="true">
    <rect width="32" height="32" rx="8" :fill="inverse ? '#FFFFFF' : '#155A50'" />
    <path
      d="M9.5 10h13L9.5 22h9"
      :stroke="inverse ? '#155A50' : '#FFFFFF'"
      stroke-width="3.2"
      stroke-linecap="round"
      stroke-linejoin="round"
    />
    <circle cx="23.2" cy="22" r="2" fill="#EE9B2A" />
  </svg>
</template>
