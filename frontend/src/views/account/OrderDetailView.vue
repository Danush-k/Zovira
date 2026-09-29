<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Download, MapPin, RotateCcw, Truck } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZDialog from '@/components/ui/ZDialog.vue'
import ZSelect from '@/components/ui/ZSelect.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import OrderStatusBadge from '@/components/order/OrderStatusBadge.vue'
import TrackingTimeline from '@/components/order/TrackingTimeline.vue'
import ReturnDialog from '@/components/order/ReturnDialog.vue'
import { ordersApi } from '@/services/orders'
import { errorMessage } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import { formatDate, formatDateTime, formatPrice } from '@/utils/format'
import type { OrderItem } from '@/types/order'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const toast = useToastStore()
const number = computed(() => String(route.params.number))
const { data: order, loading, error, execute } = useAsync((n: string) => ordersApi.detail(n))
watch(number, (n) => void execute(n), { immediate: true })

const cancelOpen = ref(false)
const cancelReason = ref('')
const cancelling = ref(false)
const returnItem = ref<OrderItem | null>(null)
const returnOpen = ref(false)
const downloading = ref(false)

const reasons = [
  'Ordered by mistake',
  'Found a better price elsewhere',
  'Delivery date is too late',
  'Want to change the address or items',
  'Other',
].map((r) => ({ value: r, label: r }))

const canInvoice = computed(() => order.value && !['PENDING_PAYMENT', 'PLACED', 'CANCELLED', 'PAYMENT_FAILED'].includes(order.value.status))

async function cancel() {
  if (!cancelReason.value) return
  cancelling.value = true
  try {
    order.value = await ordersApi.cancel(number.value, cancelReason.value)
    cancelOpen.value = false
    toast.success('Order cancelled', order.value.paymentStatus === 'REFUNDED' ? 'Your refund has been issued.' : undefined)
  } catch (e) {
    toast.error("Couldn't cancel the order", errorMessage(e))
  } finally {
    cancelling.value = false
  }
}

async function invoice() {
  downloading.value = true
  try {
    await ordersApi.downloadInvoice(number.value)
  } catch (e) {
    toast.error("Couldn't download invoice", errorMessage(e))
  } finally {
    downloading.value = false
  }
}

async function buyAgain() {
  try {
    const r = await ordersApi.reorder(number.value)
    await cart.load()
    if (r.unavailable.length) toast.warning('Some items are unavailable', r.unavailable.join(', '))
    if (r.added) await router.push('/cart')
  } catch (e) {
    toast.error("Couldn't add items to cart", errorMessage(e))
  }
}

function startReturn(item: OrderItem) {
  returnItem.value = item
  returnOpen.value = true
}

async function onReturned() {
  toast.success('Return requested', 'The seller will review it shortly.', { label: 'View returns', to: '/account/returns' })
  await execute(number.value)
}
</script>

<template>
  <div>
    <RouterLink to="/account/orders" class="inline-flex items-center gap-1.5 text-sm font-medium text-ink-600 hover:text-ink-900">
      <ArrowLeft class="size-4" /> All orders
    </RouterLink>

    <ZErrorState v-if="error" :error="error" @retry="execute(number)" />
    <div v-else-if="loading && !order" class="mt-5 space-y-4"><div class="skeleton h-24 rounded-xl" /><div class="skeleton h-80 rounded-xl" /></div>

    <template v-else-if="order">
      <header class="mt-4 flex flex-wrap items-start justify-between gap-4">
        <div>
          <h1 class="flex flex-wrap items-center gap-3 text-2xl font-semibold tracking-tight">
            Order <span class="font-mono text-xl">{{ order.orderNumber }}</span>
            <OrderStatusBadge :status="order.status" />
          </h1>
          <p class="mt-1 text-sm text-ink-500">Placed {{ formatDateTime(order.placedAt) }}</p>
        </div>
        <div class="flex flex-wrap gap-2">
          <ZButton v-if="order.paymentPending" :to="`/order-confirmation/${order.orderNumber}`" size="sm">Complete payment</ZButton>
          <ZButton v-if="canInvoice" variant="outline" size="sm" :loading="downloading" @click="invoice"><Download class="size-4" /> Invoice</ZButton>
          <ZButton v-if="['DELIVERED', 'CANCELLED', 'RETURNED'].includes(order.status)" variant="outline" size="sm" @click="buyAgain">Buy again</ZButton>
          <ZButton v-if="order.cancellable" variant="ghost" size="sm" @click="cancelOpen = true">Cancel order</ZButton>
        </div>
      </header>

      <div class="mt-6 grid gap-6 xl:grid-cols-[minmax(0,1fr)_340px]">
        <div class="space-y-6">
          <section class="surface p-5">
            <h2 class="flex items-center gap-2 font-semibold"><Truck class="size-4 text-ink-500" /> Tracking</h2>
            <p v-if="order.estimatedDeliveryDate && !['DELIVERED', 'CANCELLED', 'PAYMENT_FAILED', 'RETURNED'].includes(order.status)" class="mt-1 text-sm text-ink-600">
              Expected by <span class="font-semibold text-ink-900">{{ formatDate(order.estimatedDeliveryDate, { weekday: 'long', day: 'numeric', month: 'long' }) }}</span>
            </p>
            <TrackingTimeline :order="order" class="mt-5" />
          </section>

          <section v-for="s in order.shipments" :key="s.id" class="surface overflow-hidden">
            <header class="flex flex-wrap items-center justify-between gap-2 border-b border-ink-100 bg-ink-50/70 px-5 py-3 text-[13px]">
              <p>Shipment <span class="font-mono font-semibold">{{ s.shipmentNumber }}</span> · Sold by {{ s.sellerName }}</p>
              <p v-if="s.carrier" class="text-ink-600">{{ s.carrier }} <span class="font-mono">{{ s.trackingNumber }}</span></p>
            </header>
            <ul class="divide-y divide-ink-100 px-5">
              <li v-for="item in order.items.filter((i) => s.itemIds.includes(i.id))" :key="item.id" class="flex flex-wrap items-center gap-4 py-4">
                <RouterLink :to="`/p/${item.productSlug}`" class="size-16 shrink-0 overflow-hidden rounded-lg bg-ink-50">
                  <img v-if="item.imageUrl" :src="item.imageUrl" alt="" class="size-full object-contain p-1.5 mix-blend-multiply" />
                </RouterLink>
                <div class="min-w-0 flex-1 text-sm">
                  <RouterLink :to="`/p/${item.productSlug}`" class="line-clamp-1 font-medium hover:text-brand-800">{{ item.title }}</RouterLink>
                  <p class="text-ink-500">{{ item.variantName && item.variantName !== 'Standard' ? `${item.variantName} · ` : '' }}Qty {{ item.quantity }} · {{ formatPrice(item.lineTotal) }}</p>
                  <p v-if="item.status !== 'ACTIVE'" class="mt-0.5"><OrderStatusBadge :status="item.status === 'RETURN_REQUESTED' ? 'REQUESTED' : item.status" /></p>
                  <p v-else-if="item.returnable" class="mt-0.5 text-[13px] text-ink-500">Returnable until {{ formatDate(item.returnableUntil) }}</p>
                </div>
                <div class="flex gap-2">
                  <ZButton v-if="item.returnable" variant="outline" size="sm" @click="startReturn(item)"><RotateCcw class="size-3.5" /> Return</ZButton>
                  <ZButton v-if="item.reviewable" variant="ghost" size="sm" :to="`/p/${item.productSlug}#reviews`">Write a review</ZButton>
                </div>
              </li>
            </ul>
            <details v-if="s.events.length" class="border-t border-ink-100 px-5 py-3 text-[13px]">
              <summary class="cursor-pointer font-semibold text-brand-700">Shipment updates ({{ s.events.length }})</summary>
              <ul class="mt-3 space-y-2">
                <li v-for="(e, i) in [...s.events].reverse()" :key="i" class="flex gap-3">
                  <span class="w-36 shrink-0 text-ink-500">{{ formatDateTime(e.at) }}</span>
                  <span>{{ e.description }}<span v-if="e.location" class="text-ink-500"> · {{ e.location }}</span></span>
                </li>
              </ul>
            </details>
          </section>
        </div>

        <aside class="space-y-4">
          <section class="surface p-5 text-sm">
            <h2 class="flex items-center gap-2 font-semibold"><MapPin class="size-4 text-ink-500" /> Delivery address</h2>
            <p class="mt-2 text-ink-700">
              {{ order.shippingAddress.fullName }}<br />{{ order.shippingAddress.line1 }}<template v-if="order.shippingAddress.line2">, {{ order.shippingAddress.line2 }}</template><br />
              {{ order.shippingAddress.city }}, {{ order.shippingAddress.state }} {{ order.shippingAddress.pincode }}<br />Phone {{ order.shippingAddress.phone }}
            </p>
          </section>
          <section class="surface p-5 text-sm">
            <h2 class="font-semibold">Payment summary</h2>
            <dl class="mt-3 space-y-2">
              <div class="flex justify-between"><dt class="text-ink-500">Method</dt><dd>{{ order.paymentMethod === 'COD' ? 'Cash on delivery' : order.paymentMethod }}</dd></div>
              <div class="flex justify-between"><dt class="text-ink-500">Status</dt><dd><OrderStatusBadge :status="order.paymentStatus === 'PAID' ? 'DELIVERED' : order.paymentStatus === 'REFUNDED' ? 'REFUNDED' : 'PENDING_PAYMENT'" /></dd></div>
              <div class="flex justify-between"><dt class="text-ink-500">Items</dt><dd class="tabular">{{ formatPrice(order.subtotal) }}</dd></div>
              <div v-if="order.couponDiscount > 0" class="flex justify-between"><dt class="text-ink-500">Coupon {{ order.couponCode }}</dt><dd class="tabular text-success">-{{ formatPrice(order.couponDiscount) }}</dd></div>
              <div class="flex justify-between"><dt class="text-ink-500">Delivery</dt><dd class="tabular">{{ order.shippingFee > 0 ? formatPrice(order.shippingFee) : 'Free' }}</dd></div>
              <div v-if="order.codFee > 0" class="flex justify-between"><dt class="text-ink-500">COD fee</dt><dd class="tabular">{{ formatPrice(order.codFee) }}</dd></div>
              <div class="flex justify-between border-t border-ink-100 pt-2 text-base font-semibold"><dt>Total</dt><dd class="tabular">{{ formatPrice(order.totalAmount) }}</dd></div>
              <div v-if="order.refundedAmount > 0" class="flex justify-between text-success"><dt>Refunded</dt><dd class="tabular">{{ formatPrice(order.refundedAmount) }}</dd></div>
            </dl>
          </section>
        </aside>
      </div>

      <ZDialog v-model:open="cancelOpen" title="Cancel this order?" description="Paid orders are refunded to the original payment method." size="sm">
        <ZSelect v-model="cancelReason" label="Reason" placeholder="Select a reason" :options="reasons" />
        <template #footer>
          <ZButton variant="ghost" @click="cancelOpen = false">Keep order</ZButton>
          <ZButton variant="danger" :disabled="!cancelReason" :loading="cancelling" @click="cancel">Cancel order</ZButton>
        </template>
      </ZDialog>
      <ReturnDialog v-model:open="returnOpen" :order-number="order.orderNumber" :item="returnItem" @done="onReturned" />
    </template>
  </div>
</template>
