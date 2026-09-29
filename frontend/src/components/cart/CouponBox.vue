<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { TicketPercent, X } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import { useCartStore } from '@/stores/cart'
import { cartApi } from '@/services/cart'
import { errorMessage } from '@/services/errors'
import type { CouponOffer } from '@/types/cart'
import { formatPrice } from '@/utils/format'

const cart = useCartStore()
const code = ref('')
const error = ref<string | null>(null)
const applying = ref(false)
const offers = ref<CouponOffer[]>([])

async function loadOffers() {
  offers.value = await cartApi.availableCoupons().catch(() => [])
}

onMounted(loadOffers)
watch(() => cart.cart?.summary.subtotal, loadOffers)

async function apply(value = code.value) {
  error.value = null
  if (!value.trim()) {
    error.value = 'Enter a coupon code'
    return
  }
  applying.value = true
  try {
    await cart.applyCoupon(value.trim())
    code.value = ''
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    applying.value = false
  }
}
</script>

<template>
  <section class="surface p-5" aria-labelledby="coupon-title">
    <h2 id="coupon-title" class="flex items-center gap-2 text-base font-semibold text-ink-900">
      <TicketPercent class="size-5 text-brand-700" />
      Coupons
    </h2>

    <div
v-if="cart.cart?.summary.couponCode" class="mt-4 flex items-start justify-between gap-3 rounded-lg border border-dashed p-3"
      :class="cart.cart.summary.couponApplied ? 'border-success/40 bg-success-soft' : 'border-warning/40 bg-warning-soft'">
      <div class="text-sm">
        <p class="font-mono font-bold">{{ cart.cart.summary.couponCode }}</p>
        <p :class="cart.cart.summary.couponApplied ? 'text-success' : 'text-warning'">
          {{ cart.cart.summary.couponApplied ? `You save ${formatPrice(cart.cart.summary.couponDiscount)}` : cart.cart.summary.couponMessage }}
        </p>
      </div>
      <button type="button" class="grid size-8 place-items-center rounded-md text-ink-500 hover:bg-white/60" aria-label="Remove coupon" @click="cart.removeCoupon()">
        <X class="size-4" />
      </button>
    </div>

    <form v-else class="mt-4 flex gap-2" novalidate @submit.prevent="apply()">
      <label for="coupon-code" class="sr-only">Coupon code</label>
      <input
id="coupon-code" v-model="code" placeholder="Enter coupon code" autocomplete="off"
        class="field-control h-10 flex-1 font-mono uppercase placeholder:font-sans placeholder:normal-case" :aria-invalid="error ? 'true' : undefined" />
      <ZButton type="submit" variant="outline" :loading="applying">Apply</ZButton>
    </form>
    <p v-if="error" class="mt-2 text-[13px] text-danger" role="alert">{{ error }}</p>

    <ul v-if="offers.length && !cart.cart?.summary.couponCode" class="mt-4 space-y-2">
      <li v-for="o in offers" :key="o.code" class="flex items-start justify-between gap-3 rounded-lg border border-ink-150 p-3">
        <div class="min-w-0 text-[13px]">
          <p class="font-mono text-sm font-bold text-ink-900">{{ o.code }}</p>
          <p class="text-ink-600">{{ o.description }}</p>
          <p v-if="o.applicable && o.estimatedSaving" class="font-semibold text-success">Save {{ formatPrice(o.estimatedSaving) }}</p>
          <p v-else-if="o.message" class="text-ink-500">{{ o.message }}</p>
        </div>
        <ZButton size="sm" variant="secondary" :disabled="!o.applicable" @click="apply(o.code)">Apply</ZButton>
      </li>
    </ul>
  </section>
</template>
