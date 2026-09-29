<script setup lang="ts">
import { computed } from 'vue'
import HeroCarousel from '@/components/home/HeroCarousel.vue'
import CategoryStrip from '@/components/home/CategoryStrip.vue'
import BrandShowcase from '@/components/home/BrandShowcase.vue'
import ImmersiveBand from '@/components/home/ImmersiveBand.vue'
import ProductRail from '@/components/product/ProductRail.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import { catalogApi } from '@/services/catalog'
import { useAsync } from '@/composables/useAsync'

const { data: home, loading, error, execute } = useAsync(catalogApi.home, { immediate: true })
const categories = computed(() => home.value?.categories ?? [])
</script>

<template>
  <div class="container-page space-y-12 py-5 sm:py-8">
    <ZErrorState v-if="error && !home" :error="error" @retry="execute" />

    <template v-else>
      <div v-if="loading && !home" class="skeleton h-[340px] rounded-2xl lg:h-[400px]" />
      <HeroCarousel v-else-if="home" :slides="home.hero" />

      <CategoryStrip :categories="categories" :loading="loading" />

      <ProductRail
        title="Today's deals"
        subtitle="Top discounts across the store, updated daily"
        :products="home?.deals ?? null"
        :loading="loading"
        view-all-to="/search?sort=discount"
      />

      <ProductRail
        title="Best sellers"
        subtitle="What shoppers are buying most"
        :products="home?.bestSellers ?? null"
        :loading="loading"
        view-all-to="/search?sort=popular"
      />

      <ImmersiveBand v-if="home" :products="home.immersive" />

      <ProductRail
        title="Trending now"
        subtitle="Most viewed this week"
        :products="home?.trending ?? null"
        :loading="loading"
      />

      <BrandShowcase v-if="home" :brands="home.brands" />

      <ProductRail
        title="New arrivals"
        subtitle="Just landed on Zovira"
        :products="home?.newArrivals ?? null"
        :loading="loading"
        view-all-to="/search?sort=newest"
      />

      <ProductRail
        title="Top rated"
        subtitle="Loved by verified buyers"
        :products="home?.topRated ?? null"
        :loading="loading"
        view-all-to="/search?sort=rating"
      />
    </template>
  </div>
</template>
