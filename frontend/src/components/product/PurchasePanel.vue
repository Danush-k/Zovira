<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ShoppingBag, Tag, Zap } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZQuantityStepper from '@/components/ui/ZQuantityStepper.vue'
import WishlistButton from './WishlistButton.vue'
import { useCartStore } from '@/stores/cart'
import { useAuthStore } from '@/stores/auth'
import { useToastStore } from '@/stores/toast'
import { cartApi } from '@/services/cart'
import { errorMessage } from '@/services/errors'
import type { ProductDetail, ProductVariant } from '@/types/catalog'
import type { CouponOffer } from '@/types/cart'
import { formatPrice } from '@/utils/format'

const props = defineProps<{ product: ProductDetail; variant: ProductVariant | null }>()

const cart = useCartStore()
const auth = useAuthStore()
const toast = useToastStore()
const router = useRouter()
const quantity = ref(1)
const adding = ref<'cart' | 'buy' | null>(null)
const offers = ref<CouponOffer[]>([])

watch(
  () => props.variant?.id,
  () => (quantity.value = 1),
)

onMounted(async () => {
  offers.value = (await cartApi.offers().catch(() => [])).slice(0, 3)
})

const canBuy = computed(
  () => props.product.status === 'ACTIVE' && !!props.variant && props.variant.stockStatus !== 'OUT_OF_STOCK',
)
const max = computed(() => Math.max(1, props.variant?.maxPurchasable ?? 1))

async function add(mode: 'cart' | 'buy') {
  if (!props.variant) return
  adding.value = mode
  try {
    await cart.add(props.variant.id, quantity.value)
    if (mode === 'buy') {
      await router.push(auth.isAuthenticated ? '/checkout' : '/login?redirect=/checkout')
    } else {
      toast.success('Added to cart', `${props.product.title} (${props.variant.name})`, { label: 'View cart', to: '/cart' })
    }
  } catch (e) {
    toast.error("Couldn't add to cart", errorMessage(e))
  } finally {
    adding.value = null
  }
}

function offerText(o: CouponOffer) {
  const value = o.discountType === 'PERCENTAGE' ? `${o.discountValue}% off` : `${formatPrice(o.discountValue)} off`
  return `${value}${o.minOrderValue > 0 ? ` on orders above ${formatPrice(o.minOrderValue)}` : ''}`
}
</script>

<template>
  <div class="mt-6">
    <div v-if="offers.length" class="mb-6 rounded-xl border border-ink-150 p-4">
      <p class="flex items-center gap-2 text-sm font-semibold text-ink-900">
        <Tag class="size-4 text-brand-700" />
        Available offers
      </p>
      <ul class="mt-3 space-y-2 text-[13px] text-ink-700">
        <li v-for="o in offers" :key="o.code" class="flex gap-2">
          <span class="shrink-0 rounded border border-dashed border-brand-300 bg-brand-50 px-1.5 font-mono text-[11px] font-bold text-brand-800">
            {{ o.code }}
          </span>
          <span>{{ offerText(o) }}<span v-if="o.maxDiscount" class="text-ink-500"> (max {{ formatPrice(o.maxDiscount) }})</span></span>
        </li>
      </ul>
    </div>

    <div v-if="canBuy" class="flex items-center gap-3">
      <span class="text-sm text-ink-600">Quantity</span>
      <ZQuantityStepper v-model="quantity" :min="1" :max="max" />
    </div>

    <div class="mt-4 flex gap-3">
      <ZButton size="lg" class="flex-1" :disabled="!canBuy" :loading="adding === 'cart'" @click="add('cart')">
        <ShoppingBag class="size-5" />
        Add to cart
      </ZButton>
      <ZButton size="lg" variant="accent" class="flex-1" :disabled="!canBuy" :loading="adding === 'buy'" @click="add('buy')">
        <Zap class="size-5" />
        Buy now
      </ZButton>
      <WishlistButton :product-id="product.id" :variant-id="variant?.id" :title="product.title" size="md" />
    </div>
  </div>
</template>
