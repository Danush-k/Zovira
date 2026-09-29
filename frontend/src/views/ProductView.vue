<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { BadgeCheck, GitCompareArrows, PackageX, RotateCcw, ShieldCheck, Undo2 } from '@lucide/vue'
import ZBreadcrumbs from '@/components/ui/ZBreadcrumbs.vue'
import ZBadge from '@/components/ui/ZBadge.vue'
import ZPrice from '@/components/ui/ZPrice.vue'
import ZRatingStars from '@/components/ui/ZRatingStars.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import ProductGallery from '@/components/product/ProductGallery.vue'
import VariantSelector from '@/components/product/VariantSelector.vue'
import DeliveryEstimator from '@/components/product/DeliveryEstimator.vue'
import SpecTable from '@/components/product/SpecTable.vue'
import SellerCard from '@/components/product/SellerCard.vue'
import { catalogApi } from '@/services/catalog'
import { isStatus } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useCompareStore, COMPARE_LIMIT } from '@/stores/compare'
import { useToastStore } from '@/stores/toast'
import { findVariant, initialSelection, type Selection } from '@/utils/variants'
import { formatCompact } from '@/utils/format'
import type { ProductDetail } from '@/types/catalog'

const route = useRoute()
const compare = useCompareStore()
const toast = useToastStore()

const slug = computed(() => String(route.params.slug))
const { data: product, loading, error, execute } = useAsync((s: string) => catalogApi.product(s))
const selection = ref<Selection>({})

watch(
  slug,
  async (s) => {
    const p = await execute(s)
    if (p) {
      selection.value = initialSelection(p.variants, Number(route.query.variant) || p.defaultVariantId)
      document.title = `${p.title} | Zovira`
    }
  },
  { immediate: true },
)

const variant = computed(() => (product.value ? findVariant(product.value.variants, selection.value) : null))

/** Colour-specific photos when the chosen colour has them, otherwise the shared gallery. */
const images = computed(() => {
  const p = product.value
  if (!p) return []
  const colour = selection.value['Colour']
  if (!colour) return p.images
  const variantColour = new Map(p.variants.map((v) => [v.id, v.options['Colour']]))
  const own = p.images.filter((i) => i.variantId && variantColour.get(i.variantId) === colour)
  return own.length ? own : p.images.filter((i) => !i.variantId)
})

const paragraphs = computed(() => (product.value?.description ?? '').split(/\n{2,}/).filter(Boolean))
const unavailable = computed(() => product.value?.status === 'INACTIVE')

function toggleCompare(p: ProductDetail) {
  const ok = compare.toggle({
    slug: p.slug,
    title: p.title,
    imageUrl: p.images[0]?.url ?? null,
    categorySlug: p.category.slug,
  })
  if (!ok) toast.warning('Compare list is full', `You can compare up to ${COMPARE_LIMIT} products.`)
}
</script>

<template>
  <div class="container-page py-5 sm:py-8">
    <div v-if="loading && !product" class="grid gap-10 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)]" aria-busy="true">
      <div class="skeleton aspect-square rounded-2xl" />
      <div class="space-y-4">
        <div class="skeleton h-4 w-24" />
        <div class="skeleton h-8 w-full" />
        <div class="skeleton h-8 w-2/3" />
        <div class="skeleton h-10 w-40" />
        <div class="skeleton h-32 w-full" />
      </div>
    </div>

    <ZEmptyState
      v-else-if="error && isStatus(error, 404)"
      :icon="PackageX"
      title="This product isn't available"
      description="It may have been removed or the link is incorrect."
    >
      <RouterLink to="/" class="link">Continue shopping</RouterLink>
    </ZEmptyState>
    <ZErrorState v-else-if="error" :error="error" @retry="execute(slug)" />

    <template v-else-if="product">
      <ZBreadcrumbs
        :items="[
          { label: 'Home', to: '/' },
          ...product.breadcrumbs.map((c) => ({ label: c.name, to: `/c/${c.slug}` })),
          { label: product.title },
        ]"
      />

      <div class="mt-5 grid gap-8 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)] lg:gap-12">
        <div class="lg:sticky lg:top-40 lg:self-start">
          <ProductGallery :images="images" :title="product.title" />
        </div>

        <div class="min-w-0">
          <RouterLink
            v-if="product.brand"
            :to="`/brand/${product.brand.slug}`"
            class="text-[13px] font-bold tracking-wide text-brand-700 uppercase hover:underline"
          >
            {{ product.brand.name }}
          </RouterLink>
          <h1 class="mt-1.5 text-[1.45rem] leading-snug font-semibold tracking-tight text-ink-950 sm:text-[1.7rem]">
            {{ product.title }}
          </h1>
          <a
            v-if="product.ratingCount > 0"
            href="#reviews"
            class="mt-2.5 inline-flex items-center gap-2 text-sm text-ink-600 hover:text-ink-900"
          >
            <ZRatingStars :value="Number(product.ratingAverage)" />
            <span class="font-semibold text-ink-900">{{ Number(product.ratingAverage).toFixed(1) }}</span>
            <span>({{ formatCompact(product.ratingCount) }} ratings)</span>
          </a>

          <div class="mt-5 border-t border-ink-100 pt-5">
            <ZPrice v-if="variant" :price="variant.price" :mrp="variant.mrp" size="xl" />
            <p class="mt-1.5 text-[13px] text-ink-500">{{ $t('common.inclusiveOfTaxes') }}</p>
          </div>

          <p v-if="product.shortDescription" class="mt-5 leading-relaxed text-ink-700">{{ product.shortDescription }}</p>

          <VariantSelector
            v-if="product.attributes.length"
            v-model="selection"
            class="mt-6"
            :attributes="product.attributes"
            :variants="product.variants"
          />

          <div class="mt-6">
            <ZBadge v-if="unavailable" tone="neutral">Currently unavailable</ZBadge>
            <p v-else-if="!variant || variant.stockStatus === 'OUT_OF_STOCK'" class="font-semibold text-danger">
              Out of stock
            </p>
            <p v-else-if="variant.stockStatus === 'LOW_STOCK'" class="font-semibold text-warning">
              Only {{ variant.lowStockQuantity }} left in stock
            </p>
            <p v-else class="font-semibold text-success">In stock</p>
          </div>

          <div class="mt-6 space-y-4">
            <DeliveryEstimator :cod-available="product.codAvailable" :price="variant?.price ?? 0" />
            <SellerCard :seller="product.seller" />
          </div>

          <ul class="mt-6 grid grid-cols-2 gap-3 text-[13px] text-ink-700 sm:grid-cols-4 lg:grid-cols-2 xl:grid-cols-4">
            <li class="flex flex-col items-center gap-1.5 rounded-xl bg-ink-50 p-3 text-center">
              <component :is="product.returnable ? RotateCcw : Undo2" class="size-5 text-brand-700" stroke-width="1.75" />
              {{ product.returnable ? `${product.returnWindowDays}-day returns` : 'Non-returnable' }}
            </li>
            <li class="flex flex-col items-center gap-1.5 rounded-xl bg-ink-50 p-3 text-center">
              <ShieldCheck class="size-5 text-brand-700" stroke-width="1.75" />
              {{ product.warranty ? 'Warranty included' : 'Secure payments' }}
            </li>
            <li class="flex flex-col items-center gap-1.5 rounded-xl bg-ink-50 p-3 text-center">
              <BadgeCheck class="size-5 text-brand-700" stroke-width="1.75" />
              Genuine product
            </li>
            <li class="flex flex-col items-center gap-1.5 rounded-xl bg-ink-50 p-3 text-center">
              <button
                type="button"
                class="flex flex-col items-center gap-1.5"
                :aria-pressed="compare.has(product.slug)"
                @click="toggleCompare(product)"
              >
                <GitCompareArrows class="size-5 text-brand-700" stroke-width="1.75" />
                {{ compare.has(product.slug) ? 'Added to compare' : 'Compare' }}
              </button>
            </li>
          </ul>
        </div>
      </div>

      <div class="mt-14 grid gap-12 lg:grid-cols-[minmax(0,7fr)_minmax(0,5fr)]">
        <div class="min-w-0 space-y-12">
          <section v-if="product.highlights.length" aria-labelledby="highlights">
            <h2 id="highlights" class="section-title">Highlights</h2>
            <ul class="mt-4 grid gap-2.5 sm:grid-cols-2">
              <li v-for="h in product.highlights" :key="h" class="flex gap-2.5 text-[15px] text-ink-700">
                <span class="mt-2 size-1.5 shrink-0 rounded-full bg-brand-600" aria-hidden="true" />
                {{ h }}
              </li>
            </ul>
          </section>

          <section v-if="paragraphs.length" aria-labelledby="description">
            <h2 id="description" class="section-title">About this product</h2>
            <p v-for="(p, i) in paragraphs" :key="i" class="mt-4 leading-relaxed whitespace-pre-line text-ink-700">{{ p }}</p>
          </section>

          <section v-if="product.specifications.length" aria-labelledby="specifications">
            <h2 id="specifications" class="section-title mb-4">Specifications</h2>
            <SpecTable :groups="product.specifications" />
            <p v-if="product.warranty" class="mt-3 text-sm text-ink-500">Warranty: {{ product.warranty }}</p>
          </section>
        </div>
        <aside class="min-w-0" />
      </div>
    </template>
  </div>
</template>
