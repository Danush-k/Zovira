<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { Star } from '@lucide/vue'
import ZCheckbox from '@/components/ui/ZCheckbox.vue'
import ZSwitch from '@/components/ui/ZSwitch.vue'
import type { SearchFacets, SearchParams } from '@/types/search'
import { formatPrice } from '@/utils/format'

const props = defineProps<{ facets: SearchFacets | null; params: SearchParams; lockCategory?: boolean; lockBrand?: boolean }>()
const emit = defineEmits<{ change: [patch: Partial<SearchParams>] }>()

const selectedBrands = computed(() => (props.params.brand ? props.params.brand.split(',') : []))
const showAllBrands = ref(false)
const brandFilter = ref('')
const minPrice = ref<string>('')
const maxPrice = ref<string>('')

watch(
  () => [props.params.minPrice, props.params.maxPrice],
  ([min, max]) => {
    minPrice.value = min != null ? String(min) : ''
    maxPrice.value = max != null ? String(max) : ''
  },
  { immediate: true },
)

const brands = computed(() => {
  const list = (props.facets?.brands ?? []).filter((b) => b.name.toLowerCase().includes(brandFilter.value.toLowerCase()))
  return showAllBrands.value ? list : list.slice(0, 8)
})

function toggleBrand(slug: string, on: boolean) {
  const next = new Set(selectedBrands.value)
  if (on) next.add(slug)
  else next.delete(slug)
  emit('change', { brand: [...next].join(',') || undefined })
}

function applyPrice() {
  const min = Number(minPrice.value)
  const max = Number(maxPrice.value)
  emit('change', {
    minPrice: minPrice.value && min >= 0 ? min : undefined,
    maxPrice: maxPrice.value && max > 0 ? max : undefined,
  })
}

const pricePresets = computed(() => {
  const max = props.facets?.maxPrice ?? 0
  const all = [
    [0, 999],
    [1000, 4999],
    [5000, 19999],
    [20000, 49999],
    [50000, undefined],
  ] as const
  return all.filter(([lo]) => lo <= max)
})
</script>

<template>
  <div class="divide-y divide-ink-150 text-sm">
    <section v-if="!lockCategory && facets?.categories.length" class="pb-5">
      <h3 class="mb-3 font-semibold text-ink-900">Category</h3>
      <ul class="space-y-1">
        <li v-for="c in facets.categories" :key="c.slug">
          <button
            type="button"
            :class="[
              'flex w-full items-center justify-between rounded-md px-2 py-1.5 text-left hover:bg-ink-50',
              params.category === c.slug ? 'font-semibold text-brand-800' : 'text-ink-700',
            ]"
            @click="emit('change', { category: params.category === c.slug ? undefined : c.slug })"
          >
            {{ c.name }}
            <span class="tabular text-xs text-ink-400">{{ c.count }}</span>
          </button>
        </li>
      </ul>
    </section>

    <section class="py-5">
      <h3 class="mb-3 font-semibold text-ink-900">Price</h3>
      <ul class="mb-3 space-y-1">
        <li v-for="[lo, hi] in pricePresets" :key="lo">
          <button
            type="button"
            :class="[
              'w-full rounded-md px-2 py-1.5 text-left hover:bg-ink-50',
              params.minPrice === lo && params.maxPrice === hi ? 'font-semibold text-brand-800' : 'text-ink-700',
            ]"
            @click="emit('change', { minPrice: lo || undefined, maxPrice: hi })"
          >
            {{ hi ? `${formatPrice(lo)} to ${formatPrice(hi)}` : `Over ${formatPrice(lo)}` }}
          </button>
        </li>
      </ul>
      <form class="flex items-center gap-2" @submit.prevent="applyPrice">
        <label class="sr-only" for="min-price">Minimum price</label>
        <input id="min-price" v-model="minPrice" inputmode="numeric" placeholder="Min" class="field-control h-9 w-full text-sm" />
        <span class="text-ink-400">to</span>
        <label class="sr-only" for="max-price">Maximum price</label>
        <input id="max-price" v-model="maxPrice" inputmode="numeric" placeholder="Max" class="field-control h-9 w-full text-sm" />
        <button type="submit" class="h-9 shrink-0 rounded-lg border border-ink-200 px-3 font-semibold text-ink-800 hover:bg-ink-50">Go</button>
      </form>
    </section>

    <section v-if="!lockBrand && facets?.brands.length" class="py-5">
      <h3 class="mb-3 font-semibold text-ink-900">Brand</h3>
      <input
        v-if="facets.brands.length > 8"
        v-model="brandFilter"
        type="search"
        placeholder="Search brands"
        aria-label="Search brands"
        class="field-control mb-3 h-9 text-sm"
      />
      <ul class="space-y-2">
        <li v-for="b in brands" :key="b.slug" class="flex items-center justify-between gap-2">
          <ZCheckbox
            :model-value="selectedBrands.includes(b.slug)"
            :label="b.name"
            @update:model-value="(v: boolean) => toggleBrand(b.slug, v)"
          />
          <span class="tabular text-xs text-ink-400">{{ b.count }}</span>
        </li>
      </ul>
      <button
        v-if="!brandFilter && facets.brands.length > 8"
        type="button"
        class="mt-3 text-[13px] font-semibold text-brand-700 hover:underline"
        @click="showAllBrands = !showAllBrands"
      >
        {{ showAllBrands ? 'Show fewer' : `Show all ${facets.brands.length}` }}
      </button>
    </section>

    <section class="py-5">
      <h3 class="mb-3 font-semibold text-ink-900">Customer rating</h3>
      <ul class="space-y-1">
        <li v-for="r in facets?.ratings ?? []" :key="r.value">
          <button
            type="button"
            :aria-pressed="params.rating === r.value"
            :class="[
              'flex w-full items-center justify-between rounded-md px-2 py-1.5 hover:bg-ink-50',
              params.rating === r.value ? 'bg-brand-50 font-semibold text-brand-800' : 'text-ink-700',
            ]"
            @click="emit('change', { rating: params.rating === r.value ? undefined : r.value })"
          >
            <span class="flex items-center gap-1">
              {{ r.value }}<Star class="size-3.5 fill-star text-star" /> and above
            </span>
            <span class="tabular text-xs text-ink-400">{{ r.count }}</span>
          </button>
        </li>
      </ul>
    </section>

    <section class="py-5">
      <h3 class="mb-3 font-semibold text-ink-900">Discount</h3>
      <ul class="space-y-1">
        <li v-for="d in facets?.discounts ?? []" :key="d.value">
          <button
            type="button"
            :aria-pressed="params.discount === d.value"
            :class="[
              'flex w-full items-center justify-between rounded-md px-2 py-1.5 hover:bg-ink-50',
              params.discount === d.value ? 'bg-brand-50 font-semibold text-brand-800' : 'text-ink-700',
            ]"
            @click="emit('change', { discount: params.discount === d.value ? undefined : d.value })"
          >
            {{ d.value }}% off or more
            <span class="tabular text-xs text-ink-400">{{ d.count }}</span>
          </button>
        </li>
      </ul>
    </section>

    <section class="space-y-4 pt-5">
      <ZSwitch
        :model-value="!!params.inStock"
        label="In stock only"
        @update:model-value="(v: boolean) => emit('change', { inStock: v || undefined })"
      />
      <ZSwitch
        v-if="(facets?.with3dCount ?? 0) > 0 || params.has3d"
        :model-value="!!params.has3d"
        label="Viewable in 3D and AR"
        @update:model-value="(v: boolean) => emit('change', { has3d: v || undefined })"
      />
    </section>
  </div>
</template>
