<script setup lang="ts">
import { computed } from 'vue'
import { Check } from '@lucide/vue'

const props = defineProps<{ password: string }>()

const rules = computed(() => [
  { label: 'At least 8 characters', ok: props.password.length >= 8 },
  { label: 'A letter and a number', ok: /[A-Za-z]/.test(props.password) && /\d/.test(props.password) },
  {
    label: 'Upper and lower case or a symbol',
    ok: (/[a-z]/.test(props.password) && /[A-Z]/.test(props.password)) || /[^A-Za-z0-9]/.test(props.password),
  },
])

const score = computed(() => rules.value.filter((r) => r.ok).length)
const label = computed(() => ['Too weak', 'Weak', 'Good', 'Strong'][score.value])
const barColor = computed(() => ['bg-ink-200', 'bg-danger', 'bg-accent-500', 'bg-success'][score.value])
</script>

<template>
  <div v-if="password" class="mt-2.5" aria-live="polite">
    <div class="flex items-center gap-3">
      <div class="flex flex-1 gap-1">
        <span
          v-for="i in 3"
          :key="i"
          :class="['h-1 flex-1 rounded-full transition-colors', i <= score ? barColor : 'bg-ink-150']"
        />
      </div>
      <span class="text-xs font-medium text-ink-600">{{ label }}</span>
    </div>
    <ul class="mt-2 grid gap-1 text-xs">
      <li
        v-for="rule in rules"
        :key="rule.label"
        :class="['flex items-center gap-1.5', rule.ok ? 'text-success' : 'text-ink-500']"
      >
        <Check :class="['size-3.5', rule.ok ? 'opacity-100' : 'opacity-30']" />
        {{ rule.label }}
      </li>
    </ul>
  </div>
</template>
