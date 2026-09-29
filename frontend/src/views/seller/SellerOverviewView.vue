<script setup lang="ts">
import { computed } from 'vue'
import { IndianRupee, Package, PackageX, RotateCcw, ShoppingCart, Star } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import ZStatTile from '@/components/charts/ZStatTile.vue'
import ZAreaChart from '@/components/charts/ZAreaChart.vue'
import ZBarList from '@/components/charts/ZBarList.vue'
import OrderStatusBadge from '@/components/order/OrderStatusBadge.vue'
import { sellerApi } from '@/services/seller'
import { useAsync } from '@/composables/useAsync'
import { formatCompact, formatDate, formatPrice, pluralize } from '@/utils/format'

const { data, loading, error, execute } = useAsync(sellerApi.dashboard, { immediate: true })

const money = (v: number) => (v >= 100000 ? `₹${formatCompact(v)}` : formatPrice(v))
const series = computed(() =>
  (data.value?.revenueSeries ?? []).map((p) => ({
    label: formatDate(p.date, { day: 'numeric', month: 'short' }),
    value: Number(p.revenue),
    secondary: p.orders,
  })),
)
const trend = computed(() => (data.value?.revenueSeries ?? []).map((p) => Number(p.revenue)))
</script>

<template>
  <div>
    <ZErrorState v-if="error" :error="error" @retry="execute" />
    <div v-else-if="loading && !data" class="space-y-6">
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><div v-for="i in 4" :key="i" class="skeleton h-28 rounded-xl" /></div>
      <div class="skeleton h-72 rounded-xl" />
    </div>

    <div v-else-if="data" class="space-y-6">
      <section aria-label="Key metrics" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <ZStatTile
          label="Revenue (30 days)"
          :value="money(data.metrics.revenue30d)"
          :current="data.metrics.revenue30d"
          :previous="data.metrics.revenuePrevious30d"
          :trend="trend"
          :icon="IndianRupee"
        />
        <ZStatTile
          label="Orders (30 days)"
          :value="String(data.metrics.orders30d)"
          :current="data.metrics.orders30d"
          :previous="data.metrics.ordersPrevious30d"
          :icon="ShoppingCart"
          :hint="`${pluralize(data.metrics.unitsSold30d, 'unit')} sold`"
        />
        <ZStatTile
          label="Average order value"
          :value="formatPrice(data.metrics.averageOrderValue)"
          :icon="Package"
          :hint="`Lifetime ${money(data.metrics.lifetimeRevenue)}`"
        />
        <ZStatTile
          label="Store rating"
          :value="data.metrics.ratingCount ? Number(data.metrics.ratingAverage).toFixed(1) : '—'"
          :icon="Star"
          :hint="data.metrics.ratingCount ? `${formatCompact(data.metrics.ratingCount)} ratings` : 'No ratings yet'"
        />
      </section>

      <section class="grid gap-4 sm:grid-cols-3">
        <RouterLink to="/seller/orders" class="surface flex items-center gap-3 p-4 transition-shadow hover:shadow-raised">
          <span class="grid size-10 place-items-center rounded-xl bg-brand-50 text-brand-700"><Package class="size-5" stroke-width="1.75" /></span>
          <span><span class="block text-xl font-semibold">{{ data.metrics.pendingShipments }}</span><span class="text-[13px] text-ink-500">Shipments to prepare</span></span>
        </RouterLink>
        <RouterLink to="/seller/returns" class="surface flex items-center gap-3 p-4 transition-shadow hover:shadow-raised">
          <span class="grid size-10 place-items-center rounded-xl bg-warning-soft text-warning"><RotateCcw class="size-5" stroke-width="1.75" /></span>
          <span><span class="block text-xl font-semibold">{{ data.metrics.openReturns }}</span><span class="text-[13px] text-ink-500">Returns to review</span></span>
        </RouterLink>
        <RouterLink to="/seller/inventory?low=true" class="surface flex items-center gap-3 p-4 transition-shadow hover:shadow-raised">
          <span class="grid size-10 place-items-center rounded-xl bg-danger-soft text-danger"><PackageX class="size-5" stroke-width="1.75" /></span>
          <span><span class="block text-xl font-semibold">{{ data.metrics.outOfStock }}</span><span class="text-[13px] text-ink-500">Out of stock listings</span></span>
        </RouterLink>
      </section>

      <section class="surface p-5">
        <div class="mb-4 flex items-baseline justify-between gap-4">
          <div>
            <h2 class="font-semibold">Revenue</h2>
            <p class="text-[13px] text-ink-500">Daily earnings over the last 30 days</p>
          </div>
          <p class="tabular text-lg font-semibold">{{ money(data.metrics.revenue30d) }}</p>
        </div>
        <ZAreaChart :points="series" :format="money" value-label="Revenue" secondary-label="Orders" />
      </section>

      <div class="grid gap-6 lg:grid-cols-2">
        <section class="surface p-5">
          <h2 class="font-semibold">Top products</h2>
          <p class="mb-4 text-[13px] text-ink-500">By revenue in the last 30 days</p>
          <ZBarList
            v-if="data.topProducts.length"
            :rows="data.topProducts.map((p) => ({ key: p.productId, label: p.title, value: Number(p.revenue), meta: pluralize(p.units, 'unit') + ' sold', imageUrl: p.imageUrl, to: `/p/${p.slug}` }))"
            :format="money"
          />
          <p v-else class="py-6 text-center text-sm text-ink-500">No sales in this period yet.</p>
        </section>

        <section class="surface p-5">
          <div class="mb-4 flex items-baseline justify-between">
            <h2 class="font-semibold">Running low</h2>
            <RouterLink to="/seller/inventory?low=true" class="text-[13px] font-semibold text-brand-700 hover:underline">Manage stock</RouterLink>
          </div>
          <ul v-if="data.lowStock.length" class="divide-y divide-ink-100">
            <li v-for="row in data.lowStock" :key="row.variantId" class="flex items-center gap-3 py-2.5">
              <img v-if="row.imageUrl" :src="row.imageUrl" alt="" class="size-10 rounded-lg bg-ink-50 object-contain p-1 mix-blend-multiply" />
              <div class="min-w-0 flex-1 text-sm">
                <p class="line-clamp-1 font-medium">{{ row.productTitle }}</p>
                <p class="text-ink-500">{{ row.variantName }} · {{ row.sku }}</p>
              </div>
              <span :class="['tabular text-sm font-semibold', row.available === 0 ? 'text-danger' : 'text-warning']">
                {{ row.available }} left
              </span>
            </li>
          </ul>
          <p v-else class="py-6 text-center text-sm text-ink-500">Every listing is comfortably in stock.</p>
        </section>
      </div>

      <section class="surface p-5">
        <div class="mb-4 flex items-baseline justify-between">
          <h2 class="font-semibold">Latest orders</h2>
          <ZButton to="/seller/orders" variant="ghost" size="sm">View all</ZButton>
        </div>
        <ul v-if="data.recentOrders.length" class="divide-y divide-ink-100">
          <li v-for="o in data.recentOrders" :key="o.shipmentId" class="flex flex-wrap items-center gap-4 py-3 text-sm">
            <span class="font-mono text-[13px] font-semibold">{{ o.orderNumber }}</span>
            <span class="text-ink-600">{{ o.customerName }} · {{ o.city }}</span>
            <OrderStatusBadge :status="o.status" />
            <span class="tabular ml-auto font-semibold">{{ formatPrice(o.itemsTotal) }}</span>
          </li>
        </ul>
        <p v-else class="py-6 text-center text-sm text-ink-500">No orders yet.</p>
      </section>
    </div>
  </div>
</template>
