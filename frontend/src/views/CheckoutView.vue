<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Banknote, Building2, Check, CreditCard, Lock, MapPin, Plus, Smartphone, Truck, Wallet, Zap } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import AddressCard from '@/components/account/AddressCard.vue'
import AddressFormDialog from '@/components/account/AddressFormDialog.vue'
import EmailVerificationNotice from '@/components/account/EmailVerificationNotice.vue'
import OrderSummary from '@/components/cart/OrderSummary.vue'
import SandboxPaymentDialog from '@/components/checkout/SandboxPaymentDialog.vue'
import FormAlert from '@/components/auth/FormAlert.vue'
import { accountApi } from '@/services/account'
import { checkoutApi } from '@/services/checkout'
import { errorMessage, toApiError } from '@/services/errors'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import { useToastStore } from '@/stores/toast'
import { PaymentDismissedError, usePaymentFlow } from '@/composables/usePaymentFlow'
import { formatDate, formatPrice } from '@/utils/format'
import type { Address } from '@/types/api'
import type { CheckoutPreview, DeliveryOption, PaymentMethod } from '@/types/order'

const auth = useAuthStore()
const cart = useCartStore()
const toast = useToastStore()
const router = useRouter()
const payment = usePaymentFlow()

const addresses = ref<Address[]>([])
const addressId = ref<number | null>(null)
const delivery = ref<DeliveryOption>('STANDARD')
const method = ref<PaymentMethod>('UPI')
const preview = ref<CheckoutPreview | null>(null)
const loading = ref(true)
const loadError = ref<unknown>(null)
const placing = ref(false)
const placeError = ref<string | null>(null)
const addressDialog = ref(false)
const step = ref<1 | 2 | 3>(1)
let idempotencyKey = crypto.randomUUID()

const address = computed(() => addresses.value.find((a) => a.id === addressId.value) ?? null)

async function loadAddresses(selectId?: number) {
  addresses.value = await accountApi.addresses()
  addressId.value = selectId ?? addresses.value.find((a) => a.isDefault)?.id ?? addresses.value[0]?.id ?? null
  if (addressId.value) step.value = 2
}

async function refreshPreview() {
  if (!addressId.value) return
  try {
    preview.value = await checkoutApi.preview({ addressId: addressId.value, deliveryOption: delivery.value })
    if (!preview.value.expressAvailable && delivery.value === 'EXPRESS') delivery.value = 'STANDARD'
    if (!preview.value.codAvailable && method.value === 'COD') method.value = 'UPI'
  } catch (e) {
    loadError.value = e
  }
}

onMounted(async () => {
  try {
    await loadAddresses()
    await refreshPreview()
    if (preview.value && !preview.value.cart.items.length) void router.replace('/cart')
  } catch (e) {
    loadError.value = e
  } finally {
    loading.value = false
  }
})

watch([addressId, delivery], refreshPreview)

const methods = computed(() => [
  { value: 'UPI' as const, label: 'UPI', detail: 'Google Pay, PhonePe, Paytm and any UPI app', icon: Smartphone },
  { value: 'CARD' as const, label: 'Credit or debit card', detail: 'Visa, Mastercard, RuPay and Amex', icon: CreditCard },
  { value: 'NETBANKING' as const, label: 'Net banking', detail: 'All major Indian banks', icon: Building2 },
  { value: 'WALLET' as const, label: 'Wallets', detail: 'Paytm, Amazon Pay, MobiKwik and more', icon: Wallet },
  {
    value: 'COD' as const,
    label: 'Cash on delivery',
    detail: preview.value?.codAvailable ? 'Pay in cash or UPI when your order arrives' : preview.value?.codUnavailableReason ?? '',
    icon: Banknote,
    disabled: !preview.value?.codAvailable,
  },
])

const codFee = computed(() => (method.value === 'COD' ? (preview.value?.codFee ?? 0) : 0))
const blocked = computed(
  () => !preview.value || !address.value || !preview.value.serviceable || preview.value.cart.summary.hasIssues || !preview.value.emailVerified,
)

async function placeOrder() {
  if (!addressId.value || blocked.value) return
  placing.value = true
  placeError.value = null
  try {
    const order = await checkoutApi.place(
      { addressId: addressId.value, deliveryOption: delivery.value, paymentMethod: method.value },
      idempotencyKey,
    )
    idempotencyKey = crypto.randomUUID()
    void cart.load()
    if (order.paymentRequired && order.payment) {
      try {
        await payment.pay(order.payment, method.value)
      } catch (e) {
        if (!(e instanceof PaymentDismissedError)) toast.error('Payment failed', errorMessage(e))
        await router.push(`/order-confirmation/${order.orderNumber}`)
        return
      }
    }
    await router.push(`/order-confirmation/${order.orderNumber}`)
  } catch (e) {
    const err = toApiError(e)
    placeError.value = err.message
    if (err.code === 'CART_HAS_ISSUES' || err.code === 'OUT_OF_STOCK') await refreshPreview()
  } finally {
    placing.value = false
  }
}

async function onAddressSaved(saved: Address) {
  await loadAddresses(saved.id)
}

const deliveryDate = (iso: string | null) => (iso ? formatDate(iso, { weekday: 'short', day: 'numeric', month: 'short' }) : '')
</script>

<template>
  <div class="container-page py-6 sm:py-10">
    <div class="flex items-center justify-between">
      <h1 class="text-2xl font-semibold tracking-tight">Checkout</h1>
      <p class="flex items-center gap-1.5 text-[13px] font-medium text-ink-500"><Lock class="size-4" /> Secure checkout</p>
    </div>

    <ZErrorState v-if="loadError && !preview" :error="loadError" @retry="$router.go(0)" />
    <div v-else-if="loading" class="mt-6 grid gap-8 lg:grid-cols-[minmax(0,1fr)_380px]">
      <div class="space-y-4"><div v-for="i in 3" :key="i" class="skeleton h-28 rounded-xl" /></div>
      <div class="skeleton h-80 rounded-xl" />
    </div>

    <div v-else class="mt-6 grid items-start gap-8 lg:grid-cols-[minmax(0,1fr)_380px]">
      <div class="space-y-4">
        <EmailVerificationNotice />

        <!-- 1. Address -->
        <section class="surface" aria-labelledby="step-address">
          <header class="flex items-center justify-between gap-4 p-5">
            <h2 id="step-address" class="flex items-center gap-3 text-base font-semibold">
              <span :class="['grid size-7 place-items-center rounded-full text-sm', step > 1 ? 'bg-success text-white' : 'bg-ink-900 text-white']">
                <Check v-if="step > 1" class="size-4" /><template v-else>1</template>
              </span>
              Delivery address
            </h2>
            <button v-if="step > 1" type="button" class="text-sm font-semibold text-brand-700 hover:underline" @click="step = 1">Change</button>
          </header>
          <div v-if="step > 1 && address" class="border-t border-ink-100 px-5 py-4 pl-15">
            <AddressCard :address="address" />
          </div>
          <div v-else class="border-t border-ink-100 p-5">
            <p v-if="!addresses.length" class="mb-4 text-sm text-ink-600">Add an address to see delivery options.</p>
            <ul class="grid gap-3 sm:grid-cols-2">
              <li v-for="a in addresses" :key="a.id">
                <label :class="['flex h-full cursor-pointer gap-3 rounded-xl border p-4 transition-colors', addressId === a.id ? 'border-brand-600 bg-brand-50/60 ring-1 ring-brand-600' : 'border-ink-200 hover:border-ink-300']">
                  <input v-model="addressId" type="radio" name="address" :value="a.id" class="mt-1 accent-brand-700" />
                  <AddressCard :address="a" />
                </label>
              </li>
            </ul>
            <div class="mt-4 flex flex-wrap gap-2">
              <ZButton variant="outline" @click="addressDialog = true"><Plus class="size-4" /> Add new address</ZButton>
              <ZButton v-if="addressId" @click="step = 2">Deliver here</ZButton>
            </div>
          </div>
        </section>

        <!-- 2. Delivery -->
        <section class="surface" aria-labelledby="step-delivery">
          <header class="flex items-center justify-between gap-4 p-5">
            <h2 id="step-delivery" :class="['flex items-center gap-3 text-base font-semibold', step < 2 && 'text-ink-400']">
              <span :class="['grid size-7 place-items-center rounded-full text-sm', step > 2 ? 'bg-success text-white' : step === 2 ? 'bg-ink-900 text-white' : 'bg-ink-100 text-ink-500']">
                <Check v-if="step > 2" class="size-4" /><template v-else>2</template>
              </span>
              Delivery speed
            </h2>
            <button v-if="step > 2" type="button" class="text-sm font-semibold text-brand-700 hover:underline" @click="step = 2">Change</button>
          </header>
          <div v-if="step > 2" class="border-t border-ink-100 px-5 py-4 pl-15 text-sm">
            {{ delivery === 'EXPRESS' ? 'Express' : 'Standard' }} delivery ·
            <span class="font-semibold">arrives {{ deliveryDate(delivery === 'EXPRESS' ? preview?.expressDeliveryDate ?? null : preview?.standardDeliveryDate ?? null) }}</span>
          </div>
          <div v-else-if="step === 2 && preview" class="border-t border-ink-100 p-5">
            <p v-if="!preview.serviceable" class="text-sm text-danger">We don't deliver to this PIN code yet. Choose another address.</p>
            <div v-else class="grid gap-3 sm:grid-cols-2">
              <label :class="['flex cursor-pointer gap-3 rounded-xl border p-4', delivery === 'STANDARD' ? 'border-brand-600 bg-brand-50/60 ring-1 ring-brand-600' : 'border-ink-200 hover:border-ink-300']">
                <input v-model="delivery" type="radio" name="delivery" value="STANDARD" class="mt-1 accent-brand-700" />
                <span class="text-sm">
                  <span class="flex items-center gap-2 font-semibold"><Truck class="size-4 text-ink-500" /> Standard</span>
                  <span class="mt-1 block text-ink-600">Arrives {{ deliveryDate(preview.standardDeliveryDate) }}</span>
                  <span class="mt-0.5 block text-ink-500">Free over {{ formatPrice(preview.cart.summary.freeShippingThreshold) }}</span>
                </span>
              </label>
              <label :class="['flex gap-3 rounded-xl border p-4', !preview.expressAvailable ? 'cursor-not-allowed opacity-50' : 'cursor-pointer', delivery === 'EXPRESS' ? 'border-brand-600 bg-brand-50/60 ring-1 ring-brand-600' : 'border-ink-200 hover:border-ink-300']">
                <input v-model="delivery" type="radio" name="delivery" value="EXPRESS" class="mt-1 accent-brand-700" :disabled="!preview.expressAvailable" />
                <span class="text-sm">
                  <span class="flex items-center gap-2 font-semibold"><Zap class="size-4 text-accent-600" /> Express</span>
                  <span class="mt-1 block text-ink-600">{{ preview.expressAvailable ? `Arrives ${deliveryDate(preview.expressDeliveryDate)}` : 'Not available for this PIN code' }}</span>
                </span>
              </label>
            </div>
            <ZButton v-if="preview.serviceable" class="mt-4" @click="step = 3">Continue to payment</ZButton>
          </div>
        </section>

        <!-- 3. Payment -->
        <section class="surface" aria-labelledby="step-payment">
          <header class="p-5">
            <h2 id="step-payment" :class="['flex items-center gap-3 text-base font-semibold', step < 3 && 'text-ink-400']">
              <span :class="['grid size-7 place-items-center rounded-full text-sm', step === 3 ? 'bg-ink-900 text-white' : 'bg-ink-100 text-ink-500']">3</span>
              Payment method
            </h2>
          </header>
          <fieldset v-if="step === 3" class="space-y-2 border-t border-ink-100 p-5">
            <legend class="sr-only">Payment method</legend>
            <label
              v-for="m in methods"
              :key="m.value"
              :class="['flex items-center gap-3 rounded-xl border p-4', m.disabled ? 'cursor-not-allowed opacity-55' : 'cursor-pointer', method === m.value ? 'border-brand-600 bg-brand-50/60 ring-1 ring-brand-600' : 'border-ink-200 hover:border-ink-300']"
            >
              <input v-model="method" type="radio" name="payment" :value="m.value" class="accent-brand-700" :disabled="m.disabled" />
              <component :is="m.icon" class="size-5 text-ink-600" stroke-width="1.75" />
              <span class="text-sm">
                <span class="block font-semibold">{{ m.label }}</span>
                <span class="block text-ink-500">{{ m.detail }}</span>
              </span>
            </label>
            <p class="flex items-center gap-1.5 pt-2 text-xs text-ink-500"><Lock class="size-3.5" /> Card and bank details are entered securely with our payment partner and never stored by Zovira.</p>
          </fieldset>
        </section>

        <section v-if="preview" class="surface p-5" aria-labelledby="review-items">
          <h2 id="review-items" class="flex items-center gap-2 text-base font-semibold"><MapPin class="size-4 text-ink-500" /> Items in this order</h2>
          <ul class="mt-3 divide-y divide-ink-100">
            <li v-for="item in preview.cart.items" :key="item.variantId" class="flex items-center gap-3 py-3">
              <img v-if="item.imageUrl" :src="item.imageUrl" alt="" class="size-14 rounded-lg bg-ink-50 object-contain p-1 mix-blend-multiply" />
              <div class="min-w-0 flex-1 text-sm">
                <p class="line-clamp-1 font-medium">{{ item.title }}</p>
                <p class="text-ink-500">{{ item.variantName !== 'Standard' ? `${item.variantName} · ` : '' }}Qty {{ item.quantity }}</p>
                <p v-if="!item.purchasable" class="text-danger">Unavailable in the requested quantity</p>
              </div>
              <p class="tabular text-sm font-semibold">{{ formatPrice(item.lineTotal) }}</p>
            </li>
          </ul>
        </section>
      </div>

      <aside class="space-y-4 lg:sticky lg:top-40">
        <OrderSummary v-if="preview" :summary="preview.cart.summary" :cod-fee="codFee">
          <FormAlert v-if="placeError" class="mt-4" :message="placeError" />
          <ZButton block size="lg" class="mt-5" :disabled="blocked || step < 3" :loading="placing" @click="placeOrder">
            {{ method === 'COD' ? 'Place order' : `Pay ${formatPrice(preview.cart.summary.total + codFee)}` }}
          </ZButton>
          <p v-if="step < 3" class="mt-2 text-center text-xs text-ink-500">Complete the steps to place your order</p>
          <p v-else-if="!preview.emailVerified" class="mt-2 text-center text-xs text-danger">Verify your email to place orders</p>
          <p class="mt-3 text-center text-xs text-ink-500">
            By placing this order you agree to Zovira's <RouterLink to="/help/terms" class="underline">terms</RouterLink>.
          </p>
        </OrderSummary>
      </aside>
    </div>

    <AddressFormDialog v-model:open="addressDialog" :default-name="auth.user?.fullName" :default-phone="auth.user?.phone" @saved="onAddressSaved" />
    <SandboxPaymentDialog
      :intent="payment.sandboxIntent.value"
      :method="payment.sandboxMethod.value"
      @complete="payment.completeSandbox"
      @dismiss="payment.dismissSandbox"
    />
  </div>
</template>
