import type { Component } from 'vue'
import { Heart, Package, RotateCcw } from '@lucide/vue'

export interface AccountNavItem {
  label: string
  to: string
  icon: Component
  exact?: boolean
}

/** Feature sections (orders, wishlist, notifications, ...) register their account links here. */
export const accountNav: AccountNavItem[] = [
  { label: 'Orders', to: '/account/orders', icon: Package },
  { label: 'Returns', to: '/account/returns', icon: RotateCcw },
  { label: 'Wishlist', to: '/wishlist', icon: Heart },
]
