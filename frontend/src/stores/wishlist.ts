import { defineStore } from 'pinia'
import { ref } from 'vue'
import { wishlistApi } from '@/services/cart'
import { useAuthStore } from './auth'

/** Product ids the signed-in shopper has saved, for instant heart-icon state across the app. */
export const useWishlistStore = defineStore('wishlist', () => {
  const auth = useAuthStore()
  const ids = ref<Set<number>>(new Set())

  async function load() {
    ids.value = auth.isAuthenticated ? new Set(await wishlistApi.ids()) : new Set()
  }

  const has = (productId: number) => ids.value.has(productId)

  /** Optimistic toggle; returns the new state. Callers must ensure the user is signed in. */
  async function toggle(productId: number, variantId?: number | null): Promise<boolean> {
    const adding = !has(productId)
    const next = new Set(ids.value)
    if (adding) next.add(productId)
    else next.delete(productId)
    ids.value = next
    try {
      if (adding) await wishlistApi.add(productId, variantId)
      else await wishlistApi.remove(productId)
      return adding
    } catch (e) {
      await load().catch(() => undefined)
      throw e
    }
  }

  function forget(productId: number) {
    const next = new Set(ids.value)
    next.delete(productId)
    ids.value = next
  }

  return { ids, load, has, toggle, forget }
})
