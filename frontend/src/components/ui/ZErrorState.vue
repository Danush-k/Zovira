<script setup lang="ts">
import { computed } from 'vue'
import { AlertTriangle, RotateCw, WifiOff } from '@lucide/vue'
import ZButton from './ZButton.vue'
import { errorMessage, isNetworkError } from '@/services/errors'

const props = withDefaults(
  defineProps<{ error?: unknown; title?: string; description?: string; compact?: boolean }>(),
  { compact: false },
)
defineEmits<{ retry: [] }>()

const offline = computed(() => isNetworkError(props.error))
const heading = computed(
  () => props.title ?? (offline.value ? "You're offline" : 'Something went wrong. Please try again.'),
)
const detail = computed(
  () =>
    props.description ??
    (offline.value
      ? 'Check your internet connection and try again.'
      : props.error
        ? errorMessage(props.error)
        : undefined),
)
</script>

<template>
  <div :class="['flex flex-col items-center text-center', compact ? 'py-8' : 'py-16']" role="alert">
    <div
      :class="[
        'mb-5 grid size-14 place-items-center rounded-2xl',
        offline ? 'bg-ink-100 text-ink-600' : 'bg-danger-soft text-danger',
      ]"
    >
      <WifiOff v-if="offline" class="size-6" stroke-width="1.75" />
      <AlertTriangle v-else class="size-6" stroke-width="1.75" />
    </div>
    <h3 class="text-base font-semibold text-ink-900">{{ heading }}</h3>
    <p v-if="detail" class="mt-1.5 max-w-sm text-sm text-ink-500">{{ detail }}</p>
    <ZButton variant="outline" size="sm" class="mt-5" @click="$emit('retry')">
      <RotateCw class="size-4" />
      Try again
    </ZButton>
  </div>
</template>
