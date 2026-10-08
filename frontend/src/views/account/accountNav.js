import { Heart, Package, RotateCcw } from '@lucide/vue';
/** Feature sections (orders, wishlist, notifications, ...) register their account links here. */
export const accountNav = [
    { label: 'Orders', to: '/account/orders', icon: Package },
    { label: 'Returns', to: '/account/returns', icon: RotateCcw },
    { label: 'Wishlist', to: '/wishlist', icon: Heart },
];
