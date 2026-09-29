<script setup lang="ts">
import { computed } from 'vue'
import { Box } from '@lucide/vue'
import ZBadge from '@/components/ui/ZBadge.vue'
import ZPrice from '@/components/ui/ZPrice.vue'
import ZRatingPill from '@/components/ui/ZRatingPill.vue'
import WishlistButton from './WishlistButton.vue'
import type { ProductSummary } from '@/types/catalog'

const props = withDefaults(defineProps<{ product: ProductSummary; eager?: boolean }>(), { eager: false })

const to = computed(() => `/p/${props.product.slug}`)
</script>

<template>
  <article
    class="group relative flex h-full flex-col overflow-hidden rounded-xl border border-ink-150 bg-white transition-[box-shadow,border-color] duration-200 hover:border-ink-200 hover:shadow-raised"
  >
    <RouterLink :to="to" class="flex h-full flex-col focus-visible:outline-offset-[-2px]">
      <div class="relative aspect-square overflow-hidden bg-ink-50">
        <img
          v-if="product.imageUrl"
          :src="product.imageUrl"
          :alt="product.title"
          :loading="eager ? 'eager' : 'lazy'"
          decoding="async"
          class="size-full object-contain p-5 mix-blend-multiply transition-transform duration-500 ease-[cubic-bezier(0.22,1,0.36,1)] group-hover:scale-[1.04]"
        />
        <div class="absolute top-2.5 left-2.5 flex flex-col items-start gap-1.5">
          <ZBadge v-if="product.discountPercent >= 10" tone="deal" size="sm">{{ product.discountPercent }}% off</ZBadge>
          <ZBadge v-if="product.has3dModel" tone="dark" size="sm">
            <Box class="size-3" />
            3D &amp; AR
          </ZBadge>
        </div>
        <p
          v-if="!product.inStock"
          class="absolute inset-x-0 bottom-0 bg-white/90 py-1.5 text-center text-xs font-semibold text-ink-600 backdrop-blur-sm"
        >
          Currently out of stock
        </p>
      </div>

      <div class="flex flex-1 flex-col p-3.5">
        <p v-if="product.brand" class="truncate text-[11px] font-semibold tracking-wide text-ink-500 uppercase">
          {{ product.brand }}
        </p>
        <h3 class="mt-1 line-clamp-2 min-h-[2.6em] text-[14px] leading-[1.3] font-medium text-ink-900">
          {{ product.title }}
        </h3>
        <ZRatingPill v-if="product.ratingCount > 0" class="mt-2" :value="product.ratingAverage" :count="product.ratingCount" />
        <div class="mt-auto pt-2.5">
          <ZPrice :price="product.price" :mrp="product.mrp" size="sm" />
          <p v-if="product.variantCount > 1" class="mt-0.5 text-xs text-ink-500">{{ product.variantCount }} options</p>
        </div>
      </div>
    </RouterLink>
    <div class="absolute top-2 right-2">
      <WishlistButton :product-id="product.id" :variant-id="product.defaultVariantId" :title="product.title" />
    </div>
  </article>
</template>
