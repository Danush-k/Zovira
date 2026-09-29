<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ShieldCheck, ShoppingBag, Truck } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import CartLine from '@/components/cart/CartLine.vue'
import OrderSummary from '@/components/cart/OrderSummary.vue'
import CouponBox from '@/components/cart/CouponBox.vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { errorMessage } from '@/services/errors'
import { formatPrice } from '@/utils/format'
import type { CartItem } from '@/types/cart'

const cart = useCartStore()
const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()

onMounted(() => cart.load().catch((e) => toast.error("Couldn't load your cart", errorMessage(e))))

const data = computed(() => cart.cart)
const busy = (item: CartItem) => cart.pending.has(item.id ?? item.variantId) || cart.pending.has(item.variantId)
const blocked = computed(() => !!data.value?.items.some((i) => !i.purchasable))

async function run(fn: () => Promise<unknown>, failure: string) {
  try {
    await fn()
  } catch (e) {
    toast.error(failure, errorMessage(e))
  }
}

function checkout() {
  void router.push(auth.isAuthenticated ? '/checkout' : '/login?redirect=/checkout')
}
</script>

<template>
  <div class="container-page py-6 sm:py-10">
    <h1 class="text-2xl font-semibold tracking-tight">Shopping cart</h1>

    <div v-if="cart.loading && !data" class="mt-6 grid gap-8 lg:grid-cols-[minmax(0,1fr)_380px]">
      <div class="surface space-y-4 p-5"><div v-for="i in 3" :key="i" class="skeleton h-24" /></div>
      <div class="skeleton h-72 rounded-xl" />
    </div>

    <div v-else-if="!data || (!data.items.length && !data.savedForLater.length)" class="surface mt-6">
      <ZEmptyState :icon="ShoppingBag" title="Your cart is empty" description="Browse deals, best sellers and new arrivals to find something you'll love.">
        <ZButton to="/">Start shopping</ZButton>
        <ZButton v-if="auth.isAuthenticated" to="/wishlist" variant="outline">View wishlist</ZButton>
      </ZEmptyState>
    </div>

    <div v-else class="mt-6 grid items-start gap-8 lg:grid-cols-[minmax(0,1fr)_380px]">
      <div class="space-y-6">
        <section class="surface px-5" aria-label="Items in your cart">
          <p v-if="data.summary.amountToFreeShipping > 0 && data.items.length" class="-mx-5 flex items-center gap-2 border-b border-ink-100 bg-brand-50 px-5 py-3 text-sm text-brand-800">
            <Truck class="size-4" /> Add {{ formatPrice(data.summary.amountToFreeShipping) }} more for free delivery
          </p>
          <p v-else-if="data.items.length" class="-mx-5 flex items-center gap-2 border-b border-ink-100 bg-success-soft px-5 py-3 text-sm font-semibold text-success">
            <Truck class="size-4" /> Your order qualifies for free delivery
          </p>
          <p v-if="!data.items.length" class="py-8 text-center text-sm text-ink-500">No items in your cart. Items you saved for later are below.</p>
          <div class="divide-y divide-ink-100">
            <CartLine
v-for="item in data.items" :key="item.variantId" :item="item" :busy="busy(item)" :can-save="auth.isAuthenticated"
              @quantity="(q) => run(() => cart.setQuantity(item, q), 'Couldn\'t update quantity')"
              @remove="run(() => cart.remove(item), 'Couldn\'t remove item')"
              @save="run(() => cart.saveForLater(item.id!), 'Couldn\'t save item')" />
          </div>
        </section>

        <section v-if="data.savedForLater.length" class="surface px-5 pt-5" aria-labelledby="saved-title">
          <h2 id="saved-title" class="text-base font-semibold">Saved for later ({{ data.savedForLater.length }})</h2>
          <div class="divide-y divide-ink-100">
            <CartLine
v-for="item in data.savedForLater" :key="item.variantId" :item="item" saved :busy="busy(item)"
              @move="run(() => cart.moveToCart(item.id!), 'Couldn\'t move item')"
              @remove="run(() => cart.remove(item), 'Couldn\'t remove item')" />
          </div>
        </section>
      </div>

      <aside class="space-y-4 lg:sticky lg:top-40">
        <CouponBox v-if="auth.isAuthenticated && data.items.length" />
        <OrderSummary v-if="data.items.length" :summary="data.summary">
          <ZButton block size="lg" class="mt-5" :disabled="blocked || !data.summary.itemCount" @click="checkout">Proceed to checkout</ZButton>
          <p v-if="blocked" class="mt-2 text-center text-[13px] text-danger">Resolve the highlighted items to continue.</p>
          <p class="mt-3 flex items-center justify-center gap-1.5 text-xs text-ink-500"><ShieldCheck class="size-3.5" /> Safe and secure payments</p>
        </OrderSummary>
      </aside>
    </div>
  </div>
</template>
