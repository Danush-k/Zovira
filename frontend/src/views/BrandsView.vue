<script setup lang="ts">
import { computed } from 'vue'
import ZBreadcrumbs from '@/components/ui/ZBreadcrumbs.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import { catalogApi } from '@/services/catalog'
import { useAsync } from '@/composables/useAsync'

const { data: brands, loading, error, execute } = useAsync(catalogApi.brands, { immediate: true })

const grouped = computed(() => {
  const map = new Map<string, typeof brands.value>()
  for (const b of brands.value ?? []) {
    const letter = /[A-Z]/i.test(b.name[0] ?? '') ? b.name[0]!.toUpperCase() : '#'
    map.set(letter, [...(map.get(letter) ?? []), b])
  }
  return [...map.entries()].sort(([a], [b]) => a.localeCompare(b))
})
</script>

<template>
  <div class="container-page py-8">
    <ZBreadcrumbs :items="[{ label: 'Home', to: '/' }, { label: 'Brands' }]" />
    <h1 class="mt-4 text-2xl font-semibold tracking-tight">All brands</h1>
    <ZErrorState v-if="error" :error="error" @retry="execute" />
    <div v-else-if="loading" class="mt-8 grid gap-3 sm:grid-cols-3 lg:grid-cols-5">
      <div v-for="i in 15" :key="i" class="skeleton h-10" />
    </div>
    <div v-else class="mt-8 space-y-8">
      <section v-for="[letter, list] in grouped" :key="letter" :aria-labelledby="`letter-${letter}`">
        <h2 :id="`letter-${letter}`" class="border-b border-ink-150 pb-2 text-lg font-bold text-brand-800">{{ letter }}</h2>
        <ul class="mt-3 grid gap-x-6 gap-y-2 sm:grid-cols-3 lg:grid-cols-5">
          <li v-for="b in list" :key="b.slug">
            <RouterLink :to="`/brand/${b.slug}`" class="text-sm text-ink-700 hover:text-brand-800 hover:underline">
              {{ b.name }} <span class="text-ink-400">({{ b.productCount }})</span>
            </RouterLink>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>
