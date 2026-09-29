<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter, type LocationQuery } from 'vue-router'
import { SearchX, SlidersHorizontal, Store as StoreIcon, X } from '@lucide/vue'
import ZBreadcrumbs, { type Crumb } from '@/components/ui/ZBreadcrumbs.vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZDrawer from '@/components/ui/ZDrawer.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import ZPagination from '@/components/ui/ZPagination.vue'
import ZRatingPill from '@/components/ui/ZRatingPill.vue'
import ZSelect from '@/components/ui/ZSelect.vue'
import ProductCard from '@/components/product/ProductCard.vue'
import ProductCardSkeleton from '@/components/product/ProductCardSkeleton.vue'
import FilterPanel from '@/components/search/FilterPanel.vue'
import { searchApi } from '@/services/search'
import { catalogApi } from '@/services/catalog'
import { useAsync } from '@/composables/useAsync'
import { useCatalogStore } from '@/stores/catalog'
import type { SearchParams } from '@/types/search'
import type { Brand, Store } from '@/types/catalog'
import { formatDate, formatPrice, pluralize } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const catalog = useCatalogStore()
const filtersOpen = ref(false)

const mode = computed(() => route.name as 'search' | 'category' | 'brand' | 'store')
const slug = computed(() => (route.params.slug ? String(route.params.slug) : undefined))

const num = (v: unknown) => (typeof v === 'string' && v !== '' && !isNaN(Number(v)) ? Number(v) : undefined)
const str = (v: unknown) => (typeof v === 'string' && v !== '' ? v : undefined)

function paramsFrom(query: LocationQuery): SearchParams {
  return {
    q: str(query.q),
    category: mode.value === 'category' ? slug.value : str(query.category),
    brand: mode.value === 'brand' ? slug.value : str(query.brand),
    seller: mode.value === 'store' ? slug.value : undefined,
    minPrice: num(query.minPrice),
    maxPrice: num(query.maxPrice),
    rating: num(query.rating),
    discount: num(query.discount),
    inStock: query.inStock === 'true' || undefined,
    has3d: query.has3d === 'true' || undefined,
    sort: str(query.sort),
    page: num(query.page) ?? 0,
    size: 24,
  }
}

const params = computed(() => paramsFrom(route.query))
const { data, loading, error, execute } = useAsync((p: SearchParams) => searchApi.search(p))
const brandInfo = ref<Brand | null>(null)
const storeInfo = ref<Store | null>(null)

watch(
  () => route.fullPath,
  () => {
    if (!['search', 'category', 'brand', 'store'].includes(String(route.name))) return
    void execute(params.value)
  },
  { immediate: true },
)

watch(
  [mode, slug],
  async ([m, s]) => {
    brandInfo.value = null
    storeInfo.value = null
    if (m === 'brand' && s) brandInfo.value = await catalogApi.brand(s).catch(() => null)
    if (m === 'store' && s) storeInfo.value = await catalogApi.store(s).catch(() => null)
    if (m === 'category') await catalog.loadCategories().catch(() => undefined)
  },
  { immediate: true },
)

const category = computed(() => (mode.value === 'category' && slug.value ? catalog.findBySlug(slug.value) : null))

const heading = computed(() => {
  if (mode.value === 'category') return category.value?.name ?? 'Category'
  if (mode.value === 'brand') return brandInfo.value?.name ?? 'Brand'
  if (mode.value === 'store') return storeInfo.value?.seller.storeName ?? 'Store'
  if (params.value.q) return `Results for “${params.value.q}”`
  if (params.value.has3d) return 'Explore in 3D and AR'
  if (params.value.sort === 'discount') return "Today's deals"
  return 'All products'
})

const crumbs = computed<Crumb[]>(() => {
  const items: Crumb[] = [{ label: 'Home', to: '/' }]
  if (category.value?.parent) items.push({ label: category.value.parent.name, to: `/c/${category.value.parent.slug}` })
  items.push({ label: heading.value })
  return items
})

const sortOptions = computed(() => [
  ...(params.value.q ? [{ value: 'relevance', label: 'Relevance' }] : []),
  { value: 'popular', label: 'Popularity' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
  { value: 'newest', label: 'Newest first' },
  { value: 'rating', label: 'Customer rating' },
  { value: 'discount', label: 'Discount' },
])
const currentSort = computed(() => (data.value?.sort ?? 'POPULAR').toLowerCase())

function update(patch: Partial<SearchParams>) {
  const query: Record<string, string> = {}
  const merged = { ...params.value, ...patch, page: 'page' in patch ? patch.page : 0 }
  for (const [key, value] of Object.entries(merged)) {
    if (value === undefined || value === null || value === '' || value === false) continue
    if (key === 'size' || (key === 'page' && value === 0)) continue
    if ((key === 'category' && mode.value === 'category') || (key === 'brand' && mode.value === 'brand')) continue
    if (key === 'seller') continue
    query[key] = String(value)
  }
  void router.push({ query })
  if ('page' in patch) window.scrollTo({ top: 0, behavior: 'smooth' })
}

const chips = computed(() => {
  const p = params.value
  const out: { label: string; clear: Partial<SearchParams> }[] = []
  if (p.category && mode.value !== 'category') out.push({ label: catalog.findBySlug(p.category)?.name ?? p.category, clear: { category: undefined } })
  if (p.brand && mode.value !== 'brand')
    p.brand.split(',').forEach((b) => {
      const name = data.value?.facets.brands.find((f) => f.slug === b)?.name ?? b
      out.push({ label: name, clear: { brand: p.brand!.split(',').filter((x) => x !== b).join(',') || undefined } })
    })
  if (p.minPrice != null || p.maxPrice != null)
    out.push({
      label: p.maxPrice != null ? `${formatPrice(p.minPrice ?? 0)} to ${formatPrice(p.maxPrice)}` : `Over ${formatPrice(p.minPrice!)}`,
      clear: { minPrice: undefined, maxPrice: undefined },
    })
  if (p.rating) out.push({ label: `${p.rating} stars and above`, clear: { rating: undefined } })
  if (p.discount) out.push({ label: `${p.discount}% off or more`, clear: { discount: undefined } })
  if (p.inStock) out.push({ label: 'In stock', clear: { inStock: undefined } })
  if (p.has3d) out.push({ label: '3D and AR', clear: { has3d: undefined } })
  return out
})

function clearAll() {
  update({ category: mode.value === 'category' ? params.value.category : undefined, brand: mode.value === 'brand' ? params.value.brand : undefined, minPrice: undefined, maxPrice: undefined, rating: undefined, discount: undefined, inStock: undefined, has3d: undefined })
}
</script>

<template>
  <div class="container-page py-5 sm:py-8">
    <ZBreadcrumbs :items="crumbs" />

    <header class="mt-4">
      <div v-if="mode === 'store' && storeInfo" class="surface mb-6 flex flex-wrap items-center gap-4 p-5">
        <span class="grid size-14 place-items-center rounded-xl bg-brand-50 text-brand-700">
          <StoreIcon class="size-7" stroke-width="1.75" />
        </span>
        <div class="min-w-0 flex-1">
          <h1 class="text-xl font-semibold tracking-tight">{{ storeInfo.seller.storeName }}</h1>
          <p class="mt-1 max-w-2xl text-sm text-ink-600">{{ storeInfo.description }}</p>
          <p class="mt-2 flex flex-wrap items-center gap-3 text-[13px] text-ink-500">
            <ZRatingPill v-if="storeInfo.seller.ratingCount" :value="storeInfo.seller.ratingAverage" :count="storeInfo.seller.ratingCount" />
            <span>{{ storeInfo.seller.city }}, {{ storeInfo.seller.state }}</span>
            <span>Selling since {{ formatDate(storeInfo.memberSince, { month: 'short', year: 'numeric' }) }}</span>
          </p>
        </div>
      </div>
      <template v-else>
        <h1 class="text-2xl font-semibold tracking-tight sm:text-[1.75rem]">{{ heading }}</h1>
        <p v-if="category?.description" class="mt-1 text-ink-500">{{ category.description }}</p>
        <p v-else-if="brandInfo?.description" class="mt-1 text-ink-500">{{ brandInfo.description }}</p>
      </template>

      <nav v-if="category?.children.length" class="scrollbar-none mt-4 flex gap-2 overflow-x-auto" aria-label="Subcategories">
        <RouterLink
          v-for="child in category.children"
          :key="child.id"
          :to="`/c/${child.slug}`"
          class="shrink-0 rounded-full border border-ink-200 bg-white px-4 py-1.5 text-[13px] font-semibold text-ink-700 hover:border-brand-300 hover:text-brand-800"
        >
          {{ child.name }}
        </RouterLink>
      </nav>
    </header>

    <div class="mt-6 grid gap-8 lg:grid-cols-[250px_minmax(0,1fr)]">
      <aside class="hidden lg:block" aria-label="Filters">
        <div class="sticky top-36">
          <FilterPanel
            :facets="data?.facets ?? null"
            :params="params"
            :lock-category="mode === 'category'"
            :lock-brand="mode === 'brand'"
            @change="update"
          />
        </div>
      </aside>

      <section class="min-w-0" aria-live="polite">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <p class="text-sm text-ink-600">
            <template v-if="data">{{ pluralize(data.results.totalElements, 'product') }}</template>
            <span v-else class="skeleton inline-block h-4 w-24 align-middle" />
          </p>
          <div class="flex items-center gap-2">
            <ZButton variant="outline" size="sm" class="lg:hidden" @click="filtersOpen = true">
              <SlidersHorizontal class="size-4" />
              Filters<span v-if="chips.length"> ({{ chips.length }})</span>
            </ZButton>
            <ZSelect
              :model-value="currentSort"
              :options="sortOptions"
              size="sm"
              aria-label="Sort by"
              class="w-48"
              @update:model-value="(v) => update({ sort: String(v) })"
            />
          </div>
        </div>

        <div v-if="chips.length" class="mt-3 flex flex-wrap items-center gap-2">
          <button
            v-for="chip in chips"
            :key="chip.label"
            type="button"
            class="inline-flex items-center gap-1.5 rounded-full bg-brand-50 py-1 pr-2 pl-3 text-[13px] font-semibold text-brand-800 hover:bg-brand-100"
            :aria-label="`Remove filter ${chip.label}`"
            @click="update(chip.clear)"
          >
            {{ chip.label }}
            <X class="size-3.5" />
          </button>
          <button type="button" class="text-[13px] font-semibold text-ink-500 hover:text-ink-800" @click="clearAll">
            Clear all
          </button>
        </div>

        <ZErrorState v-if="error && !data" :error="error" @retry="execute(params)" />
        <div v-else-if="loading && !data" class="mt-5 grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 xl:grid-cols-4">
          <ProductCardSkeleton v-for="i in 8" :key="i" />
        </div>
        <ZEmptyState
          v-else-if="data && !data.results.content.length"
          :icon="SearchX"
          :title="params.q ? `No results for “${params.q}”` : 'No products match these filters'"
          description="Try different keywords, check the spelling, or remove some filters."
        >
          <ZButton v-if="chips.length" variant="outline" @click="clearAll">Clear filters</ZButton>
          <ZButton to="/">Back to home</ZButton>
        </ZEmptyState>
        <template v-else-if="data">
          <ul :class="['mt-5 grid grid-cols-2 gap-3 transition-opacity sm:gap-4 md:grid-cols-3 xl:grid-cols-4', loading && 'opacity-60']">
            <li v-for="(product, i) in data.results.content" :key="product.id">
              <ProductCard :product="product" :eager="i < 4" />
            </li>
          </ul>
          <ZPagination
            class="mt-10"
            :page="data.results.page"
            :total-pages="data.results.totalPages"
            @update:page="(p) => update({ page: p })"
          />
        </template>
      </section>
    </div>

    <ZDrawer v-model:open="filtersOpen" side="left" width="max-w-sm" title="Filters">
      <div class="p-5">
        <FilterPanel
          :facets="data?.facets ?? null"
          :params="params"
          :lock-category="mode === 'category'"
          :lock-brand="mode === 'brand'"
          @change="update"
        />
      </div>
      <template #footer>
        <ZButton block @click="filtersOpen = false">
          Show {{ data ? pluralize(data.results.totalElements, 'product') : 'results' }}
        </ZButton>
      </template>
    </ZDrawer>
  </div>
</template>
