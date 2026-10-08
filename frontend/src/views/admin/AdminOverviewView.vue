<script setup>
import { computed } from "vue";
import { adminApi } from "@/services/admin";
import { useAsync } from "@/composables/useAsync";
import { formatCompact, formatDate, formatPrice } from "@/utils/format";
const { data, loading, error, execute } = useAsync(adminApi.overview, { immediate: true });
const money = (v) => v >= 1e5 ? `\u20B9${formatCompact(v)}` : formatPrice(v);
const revenue = computed(
  () => (data.value?.revenueSeries ?? []).map((p) => ({
    label: formatDate(p.date, { day: "numeric", month: "short" }),
    value: Number(p.value),
    secondary: p.count
  }))
);
const signups = computed(
  () => (data.value?.signupSeries ?? []).map((p) => ({ label: formatDate(p.date, { day: "numeric", month: "short" }), value: p.count }))
);
</script>

<template>
  <div>
    <ZErrorState v-if="error" :error="error" @retry="execute" />
    <div v-else-if="loading && !data" class="space-y-6">
      <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><div v-for="i in 4" :key="i" class="skeleton h-28 rounded-xl" /></div>
      <div class="skeleton h-72 rounded-xl" />
    </div>

    <div v-else-if="data" class="space-y-6">
      <section v-if="data.alerts.length" class="surface border-l-4 border-l-accent-500 p-4">
        <p class="flex items-center gap-2 text-sm font-semibold"><AlertTriangle class="size-4 text-accent-600" /> Needs attention</p>
        <ul class="mt-3 flex flex-wrap gap-2">
          <li v-for="a in data.alerts" :key="a.kind">
            <RouterLink :to="a.link" class="group inline-flex items-center gap-2 rounded-lg bg-ink-50 px-3 py-1.5 text-[13px] font-medium hover:bg-ink-100">
              <span class="tabular font-bold text-ink-900">{{ a.count }}</span> {{ a.message }}
              <ArrowRight class="size-3.5 text-ink-400 transition-transform group-hover:translate-x-0.5" />
            </RouterLink>
          </li>
        </ul>
      </section>

      <section aria-label="Key metrics" class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <ZStatTile label="Revenue (30 days)" :value="money(data.metrics.revenue30d)" :current="data.metrics.revenue30d" :previous="data.metrics.revenuePrevious30d" :trend="data.revenueSeries.map((p) => Number(p.value))" :icon="IndianRupee" />
        <ZStatTile label="Orders (30 days)" :value="String(data.metrics.orders30d)" :current="data.metrics.orders30d" :previous="data.metrics.ordersPrevious30d" :icon="ShoppingBag" :hint="`Average ${formatPrice(data.metrics.averageOrderValue)}`" />
        <ZStatTile label="New customers" :value="String(data.metrics.newUsers30d)" :current="data.metrics.newUsers30d" :previous="data.metrics.newUsersPrevious30d" :icon="UserPlus" :hint="`${data.metrics.totalUsers} total`" />
        <ZStatTile label="Refunds (30 days)" :value="money(data.metrics.refunds30d)" :icon="Package" :up-is-good="false" :hint="`Lifetime revenue ${money(data.metrics.lifetimeRevenue)}`" />
      </section>

      <section class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <div class="surface p-4"><p class="text-[13px] text-ink-500">Active listings</p><p class="mt-1 text-xl font-semibold">{{ data.metrics.activeProducts }}</p></div>
        <div class="surface p-4"><p class="text-[13px] text-ink-500">Approved sellers</p><p class="mt-1 text-xl font-semibold">{{ data.metrics.totalSellers }}</p></div>
        <div class="surface p-4"><p class="text-[13px] text-ink-500">Customers who ordered</p><p class="mt-1 text-xl font-semibold">{{ data.metrics.conversionRate }}%</p></div>
        <div class="surface p-4"><p class="text-[13px] text-ink-500">Shipments pending</p><p class="mt-1 text-xl font-semibold">{{ data.metrics.unshippedOrders }}</p></div>
      </section>

      <section class="surface p-5">
        <div class="mb-4 flex items-baseline justify-between gap-4">
          <div><h2 class="font-semibold">Revenue</h2><p class="text-[13px] text-ink-500">Settled order value per day</p></div>
          <p class="tabular text-lg font-semibold">{{ money(data.metrics.revenue30d) }}</p>
        </div>
        <ZAreaChart :points="revenue" :format="money" value-label="Revenue" secondary-label="Orders" />
      </section>

      <div class="grid gap-6 lg:grid-cols-2">
        <section class="surface p-5">
          <h2 class="font-semibold">New customers</h2>
          <p class="mb-4 text-[13px] text-ink-500">Sign-ups per day</p>
          <ZAreaChart :points="signups" :format="(v) => String(Math.round(v))" value-label="Sign-ups" :height="180" />
        </section>
        <section class="surface p-5">
          <h2 class="font-semibold">Revenue by category</h2>
          <p class="mb-4 text-[13px] text-ink-500">Last 30 days</p>
          <ZBarList
            v-if="data.categoryShare.length"
            :rows="data.categoryShare.map((c) => ({ key: c.slug, label: c.name, value: Number(c.revenue), meta: `${c.units} units`, to: `/c/${c.slug}` }))"
            :format="money"
          />
          <p v-else class="py-6 text-center text-sm text-ink-500">No sales in this period.</p>
        </section>
      </div>

      <section class="surface p-5">
        <div class="mb-4 flex items-baseline justify-between">
          <h2 class="font-semibold">Top sellers</h2>
          <RouterLink to="/admin/sellers" class="text-[13px] font-semibold text-brand-700 hover:underline">All sellers</RouterLink>
        </div>
        <ul class="divide-y divide-ink-100">
          <li v-for="s in data.topSellers" :key="s.id" class="flex flex-wrap items-center gap-4 py-3">
            <span class="grid size-9 place-items-center rounded-lg bg-brand-50 text-brand-700"><Store class="size-4" stroke-width="1.75" /></span>
            <div class="min-w-0 flex-1">
              <RouterLink :to="`/store/${s.slug}`" class="font-medium hover:text-brand-800">{{ s.storeName }}</RouterLink>
              <p class="text-[13px] text-ink-500">{{ s.orders }} orders</p>
            </div>
            <ZRatingPill v-if="s.ratingCount" :value="s.ratingAverage" :count="s.ratingCount" />
            <span class="tabular font-semibold">{{ money(s.revenue) }}</span>
          </li>
        </ul>
      </section>
    </div>
  </div>
</template>
