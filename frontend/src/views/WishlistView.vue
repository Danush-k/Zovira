<script setup lang="ts">
import { ref } from 'vue'
import { Heart, TrendingDown } from '@lucide/vue'
import ZButton from '@/components/ui/ZButton.vue'
import ZEmptyState from '@/components/ui/ZEmptyState.vue'
import ZErrorState from '@/components/ui/ZErrorState.vue'
import ZPrice from '@/components/ui/ZPrice.vue'
import { wishlistApi } from '@/services/cart'
import { errorMessage } from '@/services/errors'
import { useAsync } from '@/composables/useAsync'
import { useCartStore } from '@/stores/cart'
import { useWishlistStore } from '@/stores/wishlist'
import { useToastStore } from '@/stores/toast'
import { formatPrice } from '@/utils/format'

const { data: items, loading, error, execute } = useAsync(wishlistApi.list, { immediate: true })
const cart = useCartStore()
const wishlist = useWishlistStore()
const toast = useToastStore()
const busy = ref<number | null>(null)

async function moveToCart(productId: number) {
  busy.value = productId
  try {
    items.value = await wishlistApi.moveToCart(productId)
    wishlist.forget(productId)
    await cart.load()
    toast.success('Moved to cart', undefined, { label: 'View cart', to: '/cart' })
  } catch (e) {
    toast.error("Couldn't move to cart", errorMessage(e))
  } finally {
    busy.value = null
  }
}

async function remove(productId: number) {
  busy.value = productId
  try {
    items.value = await wishlistApi.remove(productId)
    wishlist.forget(productId)
  } catch (e) {
    toast.error("Couldn't remove item", errorMessage(e))
  } finally {
    busy.value = null
  }
}
</script>

<template>
  <div class="container-page py-6 sm:py-10">
    <h1 class="text-2xl font-semibold tracking-tight">Your wishlist</h1>
    <ZErrorState v-if="error" :error="error" @retry="execute" />
    <div v-else-if="loading && !items" class="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4"><div v-for="i in 4" :key="i" class="skeleton aspect-[3/4] rounded-xl" /></div>
    <div v-else-if="!items?.length" class="surface mt-6">
      <ZEmptyState :icon="Heart" title="Your wishlist is empty" description="Tap the heart on any product to save it here. We'll show you when prices drop.">
        <ZButton to="/">Discover products</ZButton>
      </ZEmptyState>
    </div>
    <ul v-else class="mt-6 grid grid-cols-2 gap-3 sm:gap-4 md:grid-cols-3 xl:grid-cols-4">
      <li v-for="item in items" :key="item.productId" class="surface flex flex-col overflow-hidden">
        <RouterLink :to="`/p/${item.slug}`" class="relative block aspect-square bg-ink-50">
          <img v-if="item.imageUrl" :src="item.imageUrl" :alt="item.title" class="size-full object-contain p-5 mix-blend-multiply" />
          <span v-if="item.priceDrop > 0" class="absolute top-2.5 left-2.5 inline-flex items-center gap-1 rounded-md bg-success px-1.5 py-0.5 text-[11px] font-bold text-white">
            <TrendingDown class="size-3" /> {{ formatPrice(item.priceDrop) }} lower
          </span>
        </RouterLink>
        <div class="flex flex-1 flex-col p-3.5">
          <p v-if="item.brand" class="text-[11px] font-semibold tracking-wide text-ink-500 uppercase">{{ item.brand }}</p>
          <RouterLink :to="`/p/${item.slug}`" class="mt-1 line-clamp-2 text-sm font-medium hover:text-brand-800">{{ item.title }}</RouterLink>
          <ZPrice :price="item.price" :mrp="item.mrp" size="sm" class="mt-2" />
          <p v-if="!item.available" class="mt-1 text-xs font-semibold text-ink-500">Currently unavailable</p>
          <p v-else-if="!item.inStock" class="mt-1 text-xs font-semibold text-danger">Out of stock</p>
          <div class="mt-auto flex gap-2 pt-3">
            <ZButton size="sm" class="flex-1" :disabled="!item.inStock || !item.available" :loading="busy === item.productId" @click="moveToCart(item.productId)">Move to cart</ZButton>
            <ZButton size="sm" variant="ghost" :disabled="busy === item.productId" @click="remove(item.productId)">Remove</ZButton>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>
