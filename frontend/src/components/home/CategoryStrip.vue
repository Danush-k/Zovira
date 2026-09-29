<script setup lang="ts">
import type { CategoryNode } from '@/types/catalog'

defineProps<{ categories: CategoryNode[]; loading?: boolean }>()
</script>

<template>
  <section aria-labelledby="shop-by-category">
    <h2 id="shop-by-category" class="section-title mb-4">Shop by category</h2>
    <ul class="scrollbar-none -mx-4 flex gap-3 overflow-x-auto px-4 sm:-mx-6 sm:px-6 lg:mx-0 lg:grid lg:grid-cols-11 lg:gap-4 lg:overflow-visible lg:px-0">
      <template v-if="loading && !categories.length">
        <li v-for="i in 11" :key="i" class="w-24 shrink-0 lg:w-auto">
          <div class="skeleton aspect-square rounded-2xl" />
          <div class="skeleton mx-auto mt-2 h-3 w-16" />
        </li>
      </template>
      <li v-for="category in categories" :key="category.id" class="w-24 shrink-0 lg:w-auto">
        <RouterLink :to="`/c/${category.slug}`" class="group block text-center">
          <span
            class="block aspect-square overflow-hidden rounded-2xl border border-ink-150 bg-white p-2.5 transition-[border-color,box-shadow] group-hover:border-brand-200 group-hover:shadow-raised"
          >
            <img
              v-if="category.imageUrl"
              :src="category.imageUrl"
              :alt="''"
              loading="lazy"
              class="size-full object-contain mix-blend-multiply transition-transform duration-300 group-hover:scale-105"
            />
          </span>
          <span class="mt-2 block text-[13px] leading-tight font-semibold text-ink-800 group-hover:text-brand-800">
            {{ category.name }}
          </span>
        </RouterLink>
      </li>
    </ul>
  </section>
</template>
