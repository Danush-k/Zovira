<script setup lang="ts">
import type { SpecGroup } from '@/types/catalog'

defineProps<{ groups: SpecGroup[] }>()
</script>

<template>
  <div class="divide-y divide-ink-100 overflow-hidden rounded-xl border border-ink-150">
    <section v-for="group in groups" :key="group.group" class="grid gap-3 p-4 sm:grid-cols-[180px_minmax(0,1fr)] sm:p-5">
      <h3 class="text-sm font-semibold text-ink-900">{{ group.group }}</h3>
      <dl class="grid gap-x-6 gap-y-2.5 text-sm sm:grid-cols-[minmax(120px,200px)_minmax(0,1fr)]">
        <template v-for="item in group.items" :key="item.name">
          <dt class="text-ink-500">{{ item.name }}</dt>
          <dd class="text-ink-900">{{ item.value }}</dd>
        </template>
      </dl>
    </section>
  </div>
</template>
