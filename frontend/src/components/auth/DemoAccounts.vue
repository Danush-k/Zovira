<script setup lang="ts">
import { KeyRound } from '@lucide/vue'

/**
 * Shortcut for evaluating a seeded development environment. Rendered only when
 * VITE_DEMO_MODE=true, which must never be set for a production build.
 */
const emit = defineEmits<{ pick: [email: string, password: string] }>()

const accounts = [
  { role: 'Customer', email: 'aarav@zovira.test', password: 'Customer@123' },
  { role: 'Seller', email: 'seller.northwind@zovira.test', password: 'Seller@123' },
  { role: 'Admin', email: 'admin@zovira.test', password: 'Admin@12345' },
]
</script>

<template>
  <div class="mt-8 rounded-xl border border-dashed border-ink-200 bg-ink-50 p-4">
    <p class="flex items-center gap-2 text-[13px] font-semibold text-ink-700">
      <KeyRound class="size-4 text-ink-500" />
      Demo accounts
    </p>
    <div class="mt-3 grid grid-cols-3 gap-2">
      <button
        v-for="a in accounts"
        :key="a.email"
        type="button"
        class="rounded-lg border border-ink-200 bg-white px-2 py-2 text-xs font-semibold text-ink-700 hover:border-brand-300 hover:text-brand-800"
        @click="emit('pick', a.email, a.password)"
      >
        {{ a.role }}
      </button>
    </div>
  </div>
</template>
