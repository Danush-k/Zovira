<script setup lang="ts">
import { AlertCircle, CheckCircle2 } from '@lucide/vue'

withDefaults(defineProps<{ tone?: 'error' | 'success'; message: string }>(), { tone: 'error' })
</script>

<template>
  <div
    :class="[
      'flex items-start gap-2.5 rounded-lg border px-3.5 py-3 text-sm',
      tone === 'error' ? 'border-danger/20 bg-danger-soft text-danger' : 'border-success/20 bg-success-soft text-success',
    ]"
    role="alert"
  >
    <AlertCircle v-if="tone === 'error'" class="mt-0.5 size-4 shrink-0" />
    <CheckCircle2 v-else class="mt-0.5 size-4 shrink-0" />
    <span>{{ message }}</span>
  </div>
</template>
