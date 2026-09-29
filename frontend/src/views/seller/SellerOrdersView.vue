<script setup lang="ts">
import { ref, watch } from 'vue'
import { Package, Truck } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZDialog from '@/components/ui/ZDialog.vue'
import ZInput from '@/components/ui/ZInput.vue'
import ZTabs from '@/components/ui/ZTabs.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import ZPagination from '@/components/ui/ZPagination.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import OrderStatusBadge from '@/components/order/OrderStatusBadge.vue'
import { sellerApi } from '@/services/seller'
import { errorMessage } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useToastStore } from '@/stores/toast'
import { formatDate, formatPrice } from '@/utils/format'
import type { SellerOrderRow } from '@/types/seller'

const filter = ref('open')
const page = ref(0)
const toast = useToastStore()
const { data, loading, error, execute } = useAsync((f: string, p: number) => sellerApi.orders(f, p))
watch([filter, page], () => void execute(filter.value, page.value), { immediate: true })
watch(filter, () => (page.value = 0))

const tabs = [
  { value: 'open', label: 'To fulfil' },
  { value: 'shipped', label: 'In transit' },
  { value: 'delivered', label: 'Delivered' },
  { value: 'all', label: 'All' },
]

const shipDialog = ref(false)
const target = ref<SellerOrderRow | null>(null)
const ship = ref({ carrier: '', trackingNumber: '' })
const shipError = ref<string | null>(null)
const busy = ref<number | null>(null)

const NEXT: Record<string, { status: string; label: string }> = {
  PENDING: { status: 'PROCESSING', label: 'Start preparing' },
  PROCESSING: { status: 'SHIPPED', label: 'Mark as shipped' },
  SHIPPED: { status: 'OUT_FOR_DELIVERY', label: 'Out for delivery' },
  OUT_FOR_DELIVERY: { status: 'DELIVERED', label: 'Mark as delivered' },
}

async function advance(row: SellerOrderRow) {
  const next = NEXT[row.status]
  if (!next) return
  if (next.status === 'SHIPPED') {
    target.value = row
    ship.value = { carrier: row.carrier ?? '', trackingNumber: row.trackingNumber ?? '' }
    shipError.value = null
    shipDialog.value = true
    return
  }
  busy.value = row.shipmentId
  try {
    await sellerApi.updateShipment(row.shipmentId, { status: next.status })
    await execute(filter.value, page.value)
    toast.success('Shipment updated')
  } catch (e) {
    toast.error("Couldn't update the shipment", errorMessage(e))
  } finally {
    busy.value = null
  }
}

async function confirmShip() {
  if (!target.value) return
  shipError.value = null
  if (!ship.value.carrier.trim() || !ship.value.trackingNumber.trim()) {
    shipError.value = 'Enter the carrier and tracking number'
    return
  }
  busy.value = target.value.shipmentId
  try {
    await sellerApi.updateShipment(target.value.shipmentId, { status: 'SHIPPED', ...ship.value })
    shipDialog.value = false
    await execute(filter.value, page.value)
    toast.success('Marked as shipped', 'The customer can now track the package.')
  } catch (e) {
    shipError.value = errorMessage(e)
  } finally {
    busy.value = null
  }
}
</script>

<template>
  <div>
    <h1 class="text-xl font-semibold tracking-tight">Orders</h1>
    <p class="mt-1 text-sm text-ink-500">Shipments containing your products.</p>
    <ZTabs v-model="filter" :tabs="tabs" label="Filter shipments" class="mt-5" />

    <ZErrorState v-if="error" :error="error" @retry="execute(filter, page)" />
    <div v-else-if="loading && !data" class="mt-5 space-y-3"><div v-for="i in 3" :key="i" class="skeleton h-32 rounded-xl" /></div>
    <div v-else-if="!data?.content.length" class="surface mt-5">
      <ZEmptyState :icon="Package" title="Nothing to fulfil" description="New orders for your products will appear here." compact />
    </div>
    <ul v-else :class="['mt-5 space-y-4', loading && 'opacity-60']">
      <li v-for="row in data.content" :key="row.shipmentId" class="surface overflow-hidden">
        <header class="flex flex-wrap items-center gap-x-6 gap-y-2 border-b border-ink-100 bg-ink-50/70 px-5 py-3 text-[13px]">
          <span class="font-mono font-semibold">{{ row.orderNumber }}</span>
          <OrderStatusBadge :status="row.status" />
          <span class="text-ink-600">{{ row.customerName }} · {{ row.city }}, {{ row.state }} {{ row.pincode }}</span>
          <span class="text-ink-500">Placed {{ formatDate(row.placedAt) }}</span>
          <span v-if="row.paymentMethod === 'COD'" class="rounded bg-accent-50 px-1.5 py-0.5 font-semibold text-accent-700">COD</span>
          <span class="tabular ml-auto font-semibold">{{ formatPrice(row.itemsTotal) }}</span>
        </header>
        <div class="flex flex-wrap items-center gap-4 p-5">
          <ul class="min-w-0 flex-1 space-y-2">
            <li v-for="item in row.items" :key="item.id" class="flex items-center gap-3 text-sm">
              <img v-if="item.imageUrl" :src="item.imageUrl" alt="" class="size-12 rounded-lg bg-ink-50 object-contain p-1 mix-blend-multiply" />
              <span class="min-w-0">
                <span class="line-clamp-1 block font-medium">{{ item.title }}</span>
                <span class="text-ink-500">{{ item.sku }} · Qty {{ item.quantity }}</span>
              </span>
            </li>
          </ul>
          <div class="text-right text-[13px]">
            <p v-if="row.trackingNumber" class="text-ink-600">{{ row.carrier }} <span class="font-mono">{{ row.trackingNumber }}</span></p>
            <p v-if="row.estimatedDeliveryDate" class="text-ink-500">Deliver by {{ formatDate(row.estimatedDeliveryDate) }}</p>
            <ZButton v-if="NEXT[row.status]" size="sm" class="mt-2" :loading="busy === row.shipmentId" @click="advance(row)">
              <Truck class="size-4" /> {{ NEXT[row.status]!.label }}
            </ZButton>
          </div>
        </div>
      </li>
    </ul>
    <ZPagination v-if="data" class="mt-8" :page="data.page" :total-pages="data.totalPages" @update:page="(p) => (page = p)" />

    <ZDialog v-model:open="shipDialog" title="Mark as shipped" description="Enter the carrier and tracking number so the customer can follow their package." size="sm">
      <FormAlert v-if="shipError" :message="shipError" class="mb-4" />
      <form id="ship-form" class="space-y-4" @submit.prevent="confirmShip">
        <ZInput v-model="ship.carrier" label="Carrier" placeholder="BlueDart, Delhivery, Ekart..." />
        <ZInput v-model="ship.trackingNumber" label="Tracking number" placeholder="AWB123456789" />
      </form>
      <template #footer>
        <ZButton variant="ghost" @click="shipDialog = false">Cancel</ZButton>
        <ZButton type="submit" form="ship-form" :loading="busy !== null">Confirm shipment</ZButton>
      </template>
    </ZDialog>
  </div>
</template>
