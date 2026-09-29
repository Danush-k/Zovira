<script setup lang="ts">
import { ref, watch } from 'vue'
import { PackagePlus, Search } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZBadge from '@/components/ui/ZBadge.vue'
import ZTabs from '@/components/ui/ZTabs.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import ZPagination from '@/components/ui/ZPagination.vue'
import { sellerApi } from '@/services/seller'
import { errorMessage } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useDebouncedRef } from '@/composables/useDebounce'
import { useToastStore } from '@/stores/toast'
import { formatDate, formatPrice } from '@/utils/format'

const status = ref('ALL')
const search = ref('')
const debounced = useDebouncedRef(search, 300)
const page = ref(0)
const toast = useToastStore()
const busy = ref<number | null>(null)
const { data, loading, error, execute } = useAsync((s: string, q: string, p: number) =>
  sellerApi.products({ status: s, q, page: p, size: 20 }),
)
watch([status, debounced, page], () => void execute(status.value, debounced.value, page.value), { immediate: true })
watch([status, debounced], () => (page.value = 0))

const tabs = [
  { value: 'ALL', label: 'All' },
  { value: 'ACTIVE', label: 'Live' },
  { value: 'INACTIVE', label: 'Unpublished' },
  { value: 'DRAFT', label: 'Drafts' },
  { value: 'ARCHIVED', label: 'Archived' },
]
const tones = { ACTIVE: 'success', INACTIVE: 'neutral', DRAFT: 'warning', ARCHIVED: 'neutral' } as const

async function setStatus(id: number, next: string) {
  busy.value = id
  try {
    await sellerApi.setProductStatus(id, next)
    await execute(status.value, debounced.value, page.value)
    toast.success(next === 'ACTIVE' ? 'Listing published' : 'Listing updated')
  } catch (e) {
    toast.error("Couldn't update the listing", errorMessage(e))
  } finally {
    busy.value = null
  }
}
</script>

<template>
  <div>
    <div class="flex flex-wrap items-center justify-between gap-4">
      <div>
        <h1 class="text-xl font-semibold tracking-tight">Products</h1>
        <p class="mt-1 text-sm text-ink-500">Your listings on Zovira.</p>
      </div>
      <ZButton to="/seller/products/new"><PackagePlus class="size-4" /> Add product</ZButton>
    </div>

    <div class="mt-5 flex flex-wrap items-center gap-3">
      <ZTabs v-model="status" :tabs="tabs" label="Filter listings" class="flex-1" />
      <div class="relative w-full sm:w-64">
        <label for="seller-product-search" class="sr-only">Search your products</label>
        <Search class="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-ink-400" />
        <input id="seller-product-search" v-model="search" type="search" placeholder="Search products" class="field-control h-9 pl-9 text-sm" />
      </div>
    </div>

    <ZErrorState v-if="error" :error="error" @retry="execute(status, debounced, page)" />
    <div v-else-if="loading && !data" class="mt-5 space-y-2"><div v-for="i in 5" :key="i" class="skeleton h-20 rounded-xl" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5">
      <ZEmptyState :icon="PackagePlus" title="No listings here" description="Add your first product to start selling on Zovira." compact>
        <ZButton to="/seller/products/new">Add product</ZButton>
      </ZEmptyState>
    </div>
    <div v-else class="surface mt-5 overflow-x-auto">
      <table class="w-full min-w-[820px] text-sm">
        <thead class="border-b border-ink-150 text-left text-xs tracking-wide text-ink-500 uppercase">
          <tr><th class="p-4 font-semibold">Product</th><th class="p-4 font-semibold">Status</th><th class="p-4 font-semibold">Price</th><th class="p-4 font-semibold">Stock</th><th class="p-4 font-semibold">Sold</th><th class="p-4 font-semibold">Updated</th><th class="p-4" /></tr>
        </thead>
        <tbody class="divide-y divide-ink-100">
          <tr v-for="p in data.content" :key="p.id" :class="busy === p.id && 'opacity-60'">
            <td class="p-4">
              <div class="flex items-center gap-3">
                <img v-if="p.imageUrl" :src="p.imageUrl" alt="" class="size-12 shrink-0 rounded-lg bg-ink-50 object-contain p-1 mix-blend-multiply" />
                <div class="min-w-0">
                  <RouterLink :to="`/seller/products/${p.id}`" class="line-clamp-1 font-medium hover:text-brand-800">{{ p.title }}</RouterLink>
                  <p class="text-[13px] text-ink-500">{{ p.categoryName }}<span v-if="p.brandName"> · {{ p.brandName }}</span> · {{ p.variantCount }} options</p>
                </div>
              </div>
            </td>
            <td class="p-4"><ZBadge :tone="tones[p.status]" size="sm">{{ p.status === 'INACTIVE' ? 'Unpublished' : p.status.charAt(0) + p.status.slice(1).toLowerCase() }}</ZBadge></td>
            <td class="tabular p-4">{{ formatPrice(p.price) }}</td>
            <td class="tabular p-4">
              <span :class="p.totalStock === 0 ? 'font-semibold text-danger' : p.lowStock ? 'font-semibold text-warning' : ''">{{ p.totalStock }}</span>
            </td>
            <td class="tabular p-4">{{ p.soldCount }}</td>
            <td class="p-4 text-ink-500">{{ formatDate(p.updatedAt) }}</td>
            <td class="p-4 text-right">
              <div class="flex justify-end gap-1">
                <ZButton :to="`/seller/products/${p.id}`" variant="ghost" size="sm">Edit</ZButton>
                <ZButton v-if="p.status !== 'ACTIVE'" variant="ghost" size="sm" @click="setStatus(p.id, 'ACTIVE')">Publish</ZButton>
                <ZButton v-else variant="ghost" size="sm" @click="setStatus(p.id, 'INACTIVE')">Unpublish</ZButton>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <ZPagination v-if="data" class="mt-8" :page="data.page" :total-pages="data.totalPages" @update:page="(p) => (page = p)" />
  </div>
</template>
