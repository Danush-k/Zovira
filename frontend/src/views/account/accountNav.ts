import type { Component } from 'vue'

export interface AccountNavItem {
  label: string
  to: string
  icon: Component
  exact?: boolean
}

/** Feature sections (orders, wishlist, notifications, ...) register their account links here. */
export const accountNav: AccountNavItem[] = []
