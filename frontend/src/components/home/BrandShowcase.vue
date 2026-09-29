<script setup lang="ts">
import type { Brand } from '@/types/catalog'

defineProps<{ brands: Brand[] }>()
</script>

<template>
  <section v-if="brands.length" aria-labelledby="brand-showcase">
    <div class="mb-4 flex items-end justify-between">
      <div>
        <h2 id="brand-showcase" class="section-title">Brands you love</h2>
        <p class="mt-0.5 text-sm text-ink-500">Authentic products from official and authorised sellers.</p>
      </div>
      <RouterLink to="/brands" class="text-sm font-semibold text-brand-700 hover:text-brand-800">All brands</RouterLink>
    </div>
    <ul class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      <li v-for="brand in brands" :key="brand.id">
        <RouterLink
          :to="`/brand/${brand.slug}`"
          class="group flex items-center gap-3 rounded-xl border border-ink-150 bg-white p-3 transition-shadow hover:shadow-raised"
        >
          <span class="grid size-14 shrink-0 place-items-center overflow-hidden rounded-lg bg-ink-50">
            <img
              v-if="brand.imageUrl"
              :src="brand.imageUrl"
              alt=""
              loading="lazy"
              class="size-full object-contain p-1.5 mix-blend-multiply transition-transform duration-300 group-hover:scale-105"
            />
          </span>
          <span class="min-w-0">
            <span class="block truncate text-[15px] font-bold tracking-tight text-ink-900">{{ brand.name }}</span>
            <span class="text-xs text-ink-500">{{ brand.productCount }} {{ brand.productCount === 1 ? 'product' : 'products' }}</span>
          </span>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>
