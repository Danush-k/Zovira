<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { Heart } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'
import { useWishlistStore } from '@/stores/wishlist'
import { useToastStore } from '@/stores/toast'
import { errorMessage } from '@/services/errors'

const props = withDefaults(
  defineProps<{ productId: number; variantId?: number | null; title: string; size?: 'sm' | 'md' }>(),
  { size: 'sm' },
)

const auth = useAuthStore()
const wishlist = useWishlistStore()
const toast = useToastStore()
const route = useRoute()
const busy = ref(false)
const saved = computed(() => wishlist.has(props.productId))

async function toggle() {
  if (!auth.isAuthenticated) {
    toast.info('Sign in to save items', 'Your wishlist is kept on your account.', {
      label: 'Sign in',
      to: `/login?redirect=${encodeURIComponent(route.fullPath)}`,
    })
    return
  }
  busy.value = true
  try {
    const added = await wishlist.toggle(props.productId, props.variantId)
    toast.success(added ? 'Saved to wishlist' : 'Removed from wishlist', undefined, added ? { label: 'View wishlist', to: '/wishlist' } : undefined)
  } catch (e) {
    toast.error("Couldn't update wishlist", errorMessage(e))
  } finally {
    busy.value = false
  }
}

</script>

<template>
  <button
    type="button"
    :aria-pressed="saved"
    :aria-label="saved ? `Remove ${title} from wishlist` : `Save ${title} to wishlist`"
    :disabled="busy"
    :class="[
      'grid place-items-center rounded-full border transition-colors',
      size === 'sm' ? 'size-9 border-transparent bg-white/90 shadow-card backdrop-blur hover:bg-white' : 'size-12 border-ink-200 bg-white hover:border-ink-300',
    ]"
    @click.prevent.stop="toggle"
  >
    <Heart
      :class="[size === 'sm' ? 'size-[18px]' : 'size-5', saved ? 'fill-deal text-deal' : 'text-ink-600']"
      :stroke-width="saved ? 2 : 1.75"
    />
  </button>
</template>
