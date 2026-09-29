import type { Component } from 'vue'
import { Heart } from '@lucide/vue'

export interface AccountNavItem {
  label: string
  to: string
  icon: Component
  exact?: boolean
}

/** Feature sections (orders, wishlist, notifications, ...) register their account links here. */
export const accountNav: AccountNavItem[] = [{ label: 'Wishlist', to: '/wishlist', icon: Heart }]
