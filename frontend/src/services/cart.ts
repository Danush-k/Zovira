import { http } from './http'
import type { Cart, CouponOffer, GuestLine, WishlistItem } from '@/types/cart'

export const cartApi = {
  get: () => http.get<Cart>('/cart').then((r) => r.data),
  add: (variantId: number, quantity: number) => http.post<Cart>('/cart/items', { variantId, quantity }).then((r) => r.data),
  update: (itemId: number, patch: { quantity?: number; variantId?: number }) =>
    http.patch<Cart>(`/cart/items/${itemId}`, patch).then((r) => r.data),
  remove: (itemId: number) => http.delete<Cart>(`/cart/items/${itemId}`).then((r) => r.data),
  saveForLater: (itemId: number) => http.post<Cart>(`/cart/items/${itemId}/save-for-later`).then((r) => r.data),
  moveToCart: (itemId: number) => http.post<Cart>(`/cart/items/${itemId}/move-to-cart`).then((r) => r.data),
  applyCoupon: (code: string) => http.post<Cart>('/cart/coupon', { code }).then((r) => r.data),
  removeCoupon: () => http.delete<Cart>('/cart/coupon').then((r) => r.data),
  merge: (items: GuestLine[]) => http.post<Cart>('/cart/merge', { items }).then((r) => r.data),
  preview: (items: GuestLine[]) => http.post<Cart>('/cart/preview', { items }).then((r) => r.data),
  offers: () => http.get<CouponOffer[]>('/coupons/offers').then((r) => r.data),
  availableCoupons: () => http.get<CouponOffer[]>('/coupons/available').then((r) => r.data),
}

export const wishlistApi = {
  list: () => http.get<WishlistItem[]>('/wishlist').then((r) => r.data),
  ids: () => http.get<number[]>('/wishlist/ids').then((r) => r.data),
  add: (productId: number, variantId?: number | null) =>
    http.post<WishlistItem[]>('/wishlist', { productId, variantId }).then((r) => r.data),
  remove: (productId: number) => http.delete<WishlistItem[]>(`/wishlist/${productId}`).then((r) => r.data),
  moveToCart: (productId: number) => http.post<WishlistItem[]>(`/wishlist/${productId}/move-to-cart`).then((r) => r.data),
}
