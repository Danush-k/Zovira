<script setup lang="ts">
import { ChevronRight } from '@lucide/vue'
import type { RouteLocationRaw } from 'vue-router'

export interface Crumb {
  label: string
  to?: RouteLocationRaw
}

defineProps<{ items: Crumb[] }>()
</script>

<template>
  <nav aria-label="Breadcrumb" class="min-w-0">
    <ol class="flex min-w-0 items-center gap-1.5 text-[13px] text-ink-500">
      <li v-for="(item, i) in items" :key="i" class="flex min-w-0 items-center gap-1.5">
        <ChevronRight v-if="i > 0" class="size-3.5 shrink-0 text-ink-300" aria-hidden="true" />
        <RouterLink v-if="item.to && i < items.length - 1" :to="item.to" class="truncate hover:text-ink-900">
          {{ item.label }}
        </RouterLink>
        <span v-else class="truncate font-medium text-ink-800" :aria-current="i === items.length - 1 ? 'page' : undefined">
          {{ item.label }}
        </span>
      </li>
    </ol>
  </nav>
</template>
