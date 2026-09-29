<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Package, RotateCw } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZTabs from '@/components/ui/ZTabs.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import ZPagination from '@/components/ui/ZPagination.vue'
import OrderStatusBadge from '@/components/order/OrderStatusBadge.vue'
import { ordersApi } from '@/services/orders'
import { errorMessage } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import { formatDate, formatPrice } from '@/utils/format'

const filter = ref('all')
const page = ref(0)
const cart = useCartStore()
const toast = useToastStore()
const router = useRouter()
const { data, loading, error, execute } = useAsync((f: string, p: number) => ordersApi.list(f, p))

watch([filter, page], () => void execute(filter.value, page.value), { immediate: true })
watch(filter, () => (page.value = 0))

const tabs = [
  { value: 'all', label: 'All orders' },
  { value: 'active', label: 'In progress' },
  { value: 'delivered', label: 'Delivered' },
  { value: 'cancelled', label: 'Cancelled' },
]

async function buyAgain(orderNumber: string) {
  try {
    const result = await ordersApi.reorder(orderNumber)
    await cart.load()
    if (result.unavailable.length) toast.warning('Some items are unavailable', result.unavailable.join(', '))
    if (result.added) await router.push('/cart')
  } catch (e) {
    toast.error("Couldn't add items to cart", errorMessage(e))
  }
}
</script>

<template>
  <div>
    <h1 class="text-2xl font-semibold tracking-tight">Your orders</h1>
    <ZTabs v-model="filter" :tabs="tabs" label="Filter orders" class="mt-5" />

    <ZErrorState v-if="error" :error="error" @retry="execute(filter, page)" />
    <div v-else-if="loading && !data" class="mt-5 space-y-4"><div v-for="i in 3" :key="i" class="skeleton h-36 rounded-xl" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5">
      <ZEmptyState :icon="Package" title="No orders here yet" description="When you place an order, you can track it here.">
        <ZButton to="/">Start shopping</ZButton>
      </ZEmptyState>
    </div>
    <ul v-else :class="['mt-5 space-y-4', loading && 'opacity-60']">
      <li v-for="o in data.content" :key="o.orderNumber" class="surface overflow-hidden">
        <div class="flex flex-wrap items-center gap-x-8 gap-y-2 border-b border-ink-100 bg-ink-50/70 px-5 py-3 text-[13px]">
          <div><p class="text-ink-500">Placed</p><p class="font-semibold text-ink-900">{{ formatDate(o.placedAt) }}</p></div>
          <div><p class="text-ink-500">Total</p><p class="tabular font-semibold text-ink-900">{{ formatPrice(o.totalAmount) }}</p></div>
          <div><p class="text-ink-500">Order</p><p class="font-mono font-semibold text-ink-900">{{ o.orderNumber }}</p></div>
          <RouterLink :to="`/account/orders/${o.orderNumber}`" class="ml-auto font-semibold text-brand-700 hover:underline">View details</RouterLink>
        </div>
        <div class="flex flex-wrap items-center gap-5 p-5">
          <div class="flex -space-x-3">
            <span v-for="(t, i) in o.thumbnails" :key="i" class="size-16 overflow-hidden rounded-xl border-2 border-white bg-ink-50">
              <img :src="t" alt="" class="size-full object-contain p-1.5 mix-blend-multiply" />
            </span>
          </div>
          <div class="min-w-0 flex-1">
            <OrderStatusBadge :status="o.status" />
            <p class="mt-1.5 line-clamp-1 font-medium">{{ o.firstItemTitle }}<span v-if="o.itemCount > 1" class="text-ink-500"> and {{ o.itemCount - 1 }} more</span></p>
            <p class="text-[13px] text-ink-500">
              <template v-if="o.status === 'DELIVERED' && o.deliveredAt">Delivered {{ formatDate(o.deliveredAt) }}</template>
              <template v-else-if="['CONFIRMED', 'PROCESSING', 'SHIPPED', 'OUT_FOR_DELIVERY'].includes(o.status) && o.estimatedDeliveryDate">
                Arriving by {{ formatDate(o.estimatedDeliveryDate, { weekday: 'short', day: 'numeric', month: 'short' }) }}
              </template>
            </p>
          </div>
          <div class="flex gap-2">
            <ZButton :to="`/account/orders/${o.orderNumber}`" variant="outline" size="sm">Track order</ZButton>
            <ZButton v-if="['DELIVERED', 'CANCELLED', 'RETURNED'].includes(o.status)" variant="secondary" size="sm" @click="buyAgain(o.orderNumber)">
              <RotateCw class="size-3.5" /> Buy again
            </ZButton>
          </div>
        </div>
      </li>
    </ul>
    <ZPagination v-if="data" class="mt-8" :page="data.page" :total-pages="data.totalPages" @update:page="(p) => (page = p)" />
  </div>
</template>
