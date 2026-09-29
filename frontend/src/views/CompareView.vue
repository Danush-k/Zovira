<script setup lang="ts">
import { computed, watch } from 'vue'
import { GitCompareArrows, X } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import ZPrice from '@/components/ui/ZPrice.vue'
import ZRatingPill from '@/components/ui/ZRatingPill.vue'
import { catalogApi } from '@/services/catalog'
import { useAsync } from '@/composables/useAsync'
import { useCompareStore } from '@/stores/compare'
import type { ProductDetail } from '@/types/catalog'

const compare = useCompareStore()
const { data: products, loading, error, execute } = useAsync((slugs: string[]) =>
  slugs.length ? catalogApi.compare(slugs) : Promise.resolve([] as ProductDetail[]),
)

watch(
  () => compare.items.map((i) => i.slug).join(','),
  () => void execute(compare.items.map((i) => i.slug)),
  { immediate: true },
)

/** Union of every specification row across the compared products, grouped. */
const rows = computed(() => {
  const list = products.value ?? []
  const groups = new Map<string, Set<string>>()
  for (const p of list) {
    for (const g of p.specifications) {
      const names = groups.get(g.group) ?? new Set<string>()
      g.items.forEach((i) => names.add(i.name))
      groups.set(g.group, names)
    }
  }
  return [...groups.entries()].map(([group, names]) => ({ group, names: [...names] }))
})

function spec(p: ProductDetail, group: string, name: string) {
  return p.specifications.find((g) => g.group === group)?.items.find((i) => i.name === name)?.value ?? '—'
}

function price(p: ProductDetail) {
  const v = p.variants.find((x) => x.id === p.defaultVariantId) ?? p.variants[0]
  return v
}
</script>

<template>
  <div class="container-page py-8">
    <div class="flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-semibold tracking-tight">Compare products</h1>
        <p class="mt-1 text-ink-500">See specifications side by side to find the right fit.</p>
      </div>
      <ZButton v-if="compare.count" variant="ghost" size="sm" @click="compare.clear()">Clear all</ZButton>
    </div>

    <div v-if="!compare.count" class="surface mt-8">
      <ZEmptyState
        :icon="GitCompareArrows"
        title="Nothing to compare yet"
        description="Use Compare on any product page to add up to four products here."
      >
        <ZButton to="/">Browse products</ZButton>
      </ZEmptyState>
    </div>
    <div v-else-if="loading && !products" class="skeleton mt-8 h-96 rounded-xl" />
    <ZErrorState v-else-if="error" :error="error" @retry="execute(compare.items.map((i) => i.slug))" />

    <div v-else-if="products" class="surface mt-8 overflow-x-auto">
      <table class="w-full min-w-[720px] border-collapse text-sm">
        <thead>
          <tr>
            <th class="w-48 p-4 text-left align-bottom text-xs font-semibold tracking-wide text-ink-500 uppercase" scope="col">
              Product
            </th>
            <th v-for="p in products" :key="p.slug" class="p-4 text-left align-top font-normal" scope="col">
              <div class="relative">
                <button
                  type="button"
                  class="absolute -top-1 -right-1 grid size-7 place-items-center rounded-full text-ink-400 hover:bg-ink-100 hover:text-ink-700"
                  :aria-label="`Remove ${p.title} from comparison`"
                  @click="compare.remove(p.slug)"
                >
                  <X class="size-4" />
                </button>
                <RouterLink :to="`/p/${p.slug}`" class="block">
                  <img :src="p.images[0]?.url" :alt="p.title" class="aspect-square w-full rounded-lg bg-ink-50 object-contain p-3 mix-blend-multiply" />
                  <span class="mt-3 line-clamp-2 block font-semibold text-ink-900 hover:text-brand-800">{{ p.title }}</span>
                </RouterLink>
              </div>
            </th>
          </tr>
        </thead>
        <tbody class="divide-y divide-ink-100">
          <tr>
            <th scope="row" class="p-4 text-left font-medium text-ink-500">Price</th>
            <td v-for="p in products" :key="p.slug" class="p-4">
              <ZPrice v-if="price(p)" :price="price(p)!.price" :mrp="price(p)!.mrp" size="sm" />
            </td>
          </tr>
          <tr>
            <th scope="row" class="p-4 text-left font-medium text-ink-500">Rating</th>
            <td v-for="p in products" :key="p.slug" class="p-4">
              <ZRatingPill v-if="p.ratingCount" :value="p.ratingAverage" :count="p.ratingCount" />
              <span v-else class="text-ink-400">No ratings yet</span>
            </td>
          </tr>
          <tr>
            <th scope="row" class="p-4 text-left font-medium text-ink-500">Brand</th>
            <td v-for="p in products" :key="p.slug" class="p-4 text-ink-900">{{ p.brand?.name ?? '—' }}</td>
          </tr>
          <tr>
            <th scope="row" class="p-4 text-left font-medium text-ink-500">Returns</th>
            <td v-for="p in products" :key="p.slug" class="p-4 text-ink-900">
              {{ p.returnable ? `${p.returnWindowDays} days` : 'Not returnable' }}
            </td>
          </tr>
          <tr>
            <th scope="row" class="p-4 text-left font-medium text-ink-500">Warranty</th>
            <td v-for="p in products" :key="p.slug" class="p-4 text-ink-900">{{ p.warranty ?? '—' }}</td>
          </tr>
          <template v-for="row in rows" :key="row.group">
            <tr class="bg-ink-50/70">
              <th :colspan="products.length + 1" scope="rowgroup" class="px-4 py-2.5 text-left text-xs font-semibold tracking-wide text-ink-600 uppercase">
                {{ row.group }}
              </th>
            </tr>
            <tr v-for="name in row.names" :key="`${row.group}-${name}`">
              <th scope="row" class="p-4 text-left font-medium text-ink-500">{{ name }}</th>
              <td v-for="p in products" :key="p.slug" class="p-4 text-ink-900">{{ spec(p, row.group, name) }}</td>
            </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>
