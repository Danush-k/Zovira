<script setup lang="ts">
import { ArrowRight, Box, Move3d, ScanLine } from '@lucide/vue'
import ZPrice from '@/components/ui/ZPrice.vue'
import type { ProductSummary } from '@/types/catalog'

defineProps<{ products: ProductSummary[] }>()
</script>

<template>
  <section v-if="products.length" class="overflow-hidden rounded-2xl bg-brand-950 text-white" aria-labelledby="immersive-title">
    <div class="grid gap-8 p-6 sm:p-10 lg:grid-cols-[320px_minmax(0,1fr)] lg:gap-10">
      <div class="flex flex-col">
        <p class="inline-flex w-fit items-center gap-2 rounded-full bg-white/10 px-3 py-1 text-xs font-semibold text-accent-300">
          <Box class="size-3.5" />
          3D and AR
        </p>
        <h2 id="immersive-title" class="mt-4 text-2xl leading-tight font-bold tracking-tight sm:text-[1.75rem]">
          Try it in your space before it arrives
        </h2>
        <p class="mt-3 text-[15px] leading-relaxed text-brand-100/80">
          Spin products around in 3D, check every detail, then place them in your room with augmented reality.
        </p>
        <ul class="mt-6 space-y-3 text-sm text-brand-50/90">
          <li class="flex items-center gap-3"><Move3d class="size-4 text-accent-300" /> Rotate, zoom and inspect</li>
          <li class="flex items-center gap-3"><ScanLine class="size-4 text-accent-300" /> True-to-size AR placement</li>
        </ul>
        <RouterLink
          to="/search?has3d=true"
          class="group mt-8 inline-flex w-fit items-center gap-2 rounded-xl bg-white px-5 py-3 text-sm font-semibold text-brand-900 hover:bg-brand-50"
        >
          Explore the collection
          <ArrowRight class="size-4 transition-transform group-hover:translate-x-0.5" />
        </RouterLink>
      </div>
      <ul class="scrollbar-none -mx-6 flex snap-x gap-4 overflow-x-auto px-6 sm:-mx-10 sm:px-10 lg:mx-0 lg:grid lg:grid-cols-4 lg:overflow-visible lg:px-0">
        <li v-for="product in products.slice(0, 8)" :key="product.id" class="w-[62%] shrink-0 snap-start sm:w-[38%] lg:w-auto">
          <RouterLink :to="`/p/${product.slug}`" class="group block">
            <span class="relative block aspect-square overflow-hidden rounded-xl bg-[#f3f3f0]">
              <img
                v-if="product.imageUrl"
                :src="product.imageUrl"
                :alt="product.title"
                loading="lazy"
                class="size-full object-contain transition-transform duration-500 group-hover:scale-105"
              />
              <span class="absolute top-2 left-2 rounded-md bg-ink-950/80 px-1.5 py-0.5 text-[10px] font-bold tracking-wide text-white">
                3D
              </span>
            </span>
            <span class="mt-3 line-clamp-1 block text-sm font-medium text-white/95">{{ product.title }}</span>
            <ZPrice :price="product.price" :mrp="product.mrp" size="sm" :show-discount="false" class="mt-1 [&_span:first-child]:text-white [&_.line-through]:text-white/45" />
          </RouterLink>
        </li>
      </ul>
    </div>
  </section>
</template>
