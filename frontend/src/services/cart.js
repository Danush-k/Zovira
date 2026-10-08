import { http } from './http';
export const cartApi = {
    get: () => http.get('/cart').then((r) => r.data),
    add: (variantId, quantity) => http.post('/cart/items', { variantId, quantity }).then((r) => r.data),
    update: (itemId, patch) => http.patch(`/cart/items/${itemId}`, patch).then((r) => r.data),
    remove: (itemId) => http.delete(`/cart/items/${itemId}`).then((r) => r.data),
    saveForLater: (itemId) => http.post(`/cart/items/${itemId}/save-for-later`).then((r) => r.data),
    moveToCart: (itemId) => http.post(`/cart/items/${itemId}/move-to-cart`).then((r) => r.data),
    applyCoupon: (code) => http.post('/cart/coupon', { code }).then((r) => r.data),
    removeCoupon: () => http.delete('/cart/coupon').then((r) => r.data),
    merge: (items) => http.post('/cart/merge', { items }).then((r) => r.data),
    preview: (items) => http.post('/cart/preview', { items }).then((r) => r.data),
    offers: () => http.get('/coupons/offers').then((r) => r.data),
    availableCoupons: () => http.get('/coupons/available').then((r) => r.data),
};
export const wishlistApi = {
    list: () => http.get('/wishlist').then((r) => r.data),
    ids: () => http.get('/wishlist/ids').then((r) => r.data),
    add: (productId, variantId) => http.post('/wishlist', { productId, variantId }).then((r) => r.data),
    remove: (productId) => http.delete(`/wishlist/${productId}`).then((r) => r.data),
    moveToCart: (productId) => http.post(`/wishlist/${productId}/move-to-cart`).then((r) => r.data),
};
