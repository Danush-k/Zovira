<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { AlertCircle, CheckCircle2, Clock } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import SandboxPaymentDialog from '@/components/checkout/SandboxPaymentDialog.vue'
import { checkoutApi } from '@/services/checkout'
import { errorMessage } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { PaymentDismissedError, usePaymentFlow } from '@/composables/usePaymentFlow'
import { useToastStore } from '@/stores/toast'
import { formatDate, formatPrice } from '@/utils/format'
import type { PaymentMethod } from '@/types/order'

const route = useRoute()
const toast = useToastStore()
const payment = usePaymentFlow()
const number = computed(() => String(route.params.number))
const { data: order, loading, error, execute } = useAsync((n: string) => checkoutApi.order(n))
const retrying = ref(false)

watch(number, (n) => void execute(n), { immediate: true })

const paid = computed(() => order.value && !order.value.paymentPending && order.value.status !== 'PAYMENT_FAILED')

async function retry() {
  if (!order.value) return
  retrying.value = true
  const method: PaymentMethod = order.value.paymentMethod === 'COD' ? 'UPI' : order.value.paymentMethod
  try {
    const intent = await checkoutApi.retry(order.value.orderNumber, method)
    await payment.pay(intent, method)
    toast.success('Payment received')
  } catch (e) {
    if (!(e instanceof PaymentDismissedError)) toast.error('Payment failed', errorMessage(e))
  } finally {
    retrying.value = false
    await execute(number.value)
  }
}
</script>

<template>
  <div class="container-page max-w-3xl py-10 sm:py-16">
    <ZErrorState v-if="error" :error="error" @retry="execute(number)" />
    <div v-else-if="loading && !order" class="skeleton h-96 rounded-2xl" />
    <div v-else-if="order" class="surface overflow-hidden">
      <div :class="['px-6 py-10 text-center sm:px-10', paid ? 'bg-success-soft' : 'bg-warning-soft']">
        <CheckCircle2 v-if="paid" class="mx-auto size-14 animate-pop-in text-success" stroke-width="1.5" />
        <AlertCircle v-else-if="order.status === 'PAYMENT_FAILED'" class="mx-auto size-14 text-danger" stroke-width="1.5" />
        <Clock v-else class="mx-auto size-14 text-warning" stroke-width="1.5" />
        <h1 class="mt-4 text-2xl font-semibold tracking-tight">
          {{ paid ? 'Thank you, your order is confirmed' : order.status === 'PAYMENT_FAILED' ? 'Payment was not completed' : 'Your order is waiting for payment' }}
        </h1>
        <p class="mt-2 text-ink-600">
          Order <span class="font-mono font-semibold text-ink-900">{{ order.orderNumber }}</span>
          <template v-if="paid && order.estimatedDeliveryDate">
            · Arriving by <span class="font-semibold text-ink-900">{{ formatDate(order.estimatedDeliveryDate, { weekday: 'long', day: 'numeric', month: 'long' }) }}</span>
          </template>
        </p>
        <p v-if="order.paymentPending" class="mx-auto mt-3 max-w-md text-sm text-ink-600">
          Complete the payment within 30 minutes to keep your items reserved.
        </p>
        <div class="mt-6 flex flex-wrap justify-center gap-3">
          <ZButton v-if="order.paymentPending" :loading="retrying" @click="retry">Complete payment of {{ formatPrice(order.totalAmount) }}</ZButton>
          <ZButton :to="`/account/orders/${order.orderNumber}`" :variant="order.paymentPending ? 'outline' : 'primary'">View order details</ZButton>
          <ZButton to="/" variant="outline">Continue shopping</ZButton>
        </div>
      </div>
      <div class="p-6 sm:p-8">
        <ul class="divide-y divide-ink-100">
          <li v-for="item in order.items" :key="item.id" class="flex items-center gap-4 py-3">
            <img v-if="item.imageUrl" :src="item.imageUrl" alt="" class="size-16 rounded-lg bg-ink-50 object-contain p-1 mix-blend-multiply" />
            <div class="min-w-0 flex-1 text-sm">
              <p class="line-clamp-1 font-medium">{{ item.title }}</p>
              <p class="text-ink-500">Qty {{ item.quantity }} · Sold by {{ item.sellerName }}</p>
            </div>
            <p class="tabular text-sm font-semibold">{{ formatPrice(item.lineTotal) }}</p>
          </li>
        </ul>
        <div class="mt-4 grid gap-6 border-t border-ink-100 pt-5 text-sm sm:grid-cols-2">
          <div>
            <p class="font-semibold">Delivering to</p>
            <p class="mt-1 text-ink-600">
              {{ order.shippingAddress.fullName }}<br />{{ order.shippingAddress.line1 }}<br />
              {{ order.shippingAddress.city }}, {{ order.shippingAddress.state }} {{ order.shippingAddress.pincode }}
            </p>
          </div>
          <dl class="space-y-1.5">
            <div class="flex justify-between"><dt class="text-ink-500">Payment</dt><dd>{{ order.paymentMethod === 'COD' ? 'Cash on delivery' : order.paymentMethod }}</dd></div>
            <div v-if="order.couponDiscount > 0" class="flex justify-between"><dt class="text-ink-500">Coupon savings</dt><dd class="text-success">-{{ formatPrice(order.couponDiscount) }}</dd></div>
            <div class="flex justify-between"><dt class="text-ink-500">Delivery</dt><dd>{{ order.shippingFee > 0 ? formatPrice(order.shippingFee) : 'Free' }}</dd></div>
            <div class="flex justify-between text-base font-semibold"><dt>Total</dt><dd class="tabular">{{ formatPrice(order.totalAmount) }}</dd></div>
          </dl>
        </div>
      </div>
    </div>
    <SandboxPaymentDialog
      :intent="payment.sandboxIntent.value"
      :method="payment.sandboxMethod.value"
      @complete="payment.completeSandbox"
      @dismiss="payment.dismissSandbox"
    />
  </div>
</template>
