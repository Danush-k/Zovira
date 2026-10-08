import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { cartApi } from '@/services/cart';
import { useAuthStore } from './auth';
const GUEST_KEY = 'zovira.guestCart';
const MAX_PER_LINE = 10;
function readGuest() {
    try {
        const raw = localStorage.getItem(GUEST_KEY);
        return raw ? JSON.parse(raw).filter((l) => l.variantId && l.quantity > 0) : [];
    }
    catch {
        return [];
    }
}
function writeGuest(lines) {
    try {
        if (lines.length)
            localStorage.setItem(GUEST_KEY, JSON.stringify(lines));
        else
            localStorage.removeItem(GUEST_KEY);
    }
    catch {
        // storage unavailable
    }
}
/**
 * Signed-in shoppers use the persistent server cart. Guests keep lines in localStorage, priced by
 * the server's preview endpoint, and those lines are merged into the account on sign-in.
 */
export const useCartStore = defineStore('cart', () => {
    const auth = useAuthStore();
    const cart = ref(null);
    const guestLines = ref(readGuest());
    const loading = ref(false);
    const loaded = ref(false);
    const pending = ref(new Set());
    const count = computed(() => auth.isAuthenticated
        ? (cart.value?.items.reduce((n, i) => n + i.quantity, 0) ?? 0)
        : guestLines.value.reduce((n, l) => n + l.quantity, 0));
    async function load() {
        loading.value = true;
        try {
            if (auth.isAuthenticated)
                cart.value = await cartApi.get();
            else
                cart.value = guestLines.value.length ? await cartApi.preview(guestLines.value) : null;
            loaded.value = true;
        }
        finally {
            loading.value = false;
        }
    }
    function setGuest(lines) {
        guestLines.value = lines;
        writeGuest(lines);
    }
    async function add(variantId, quantity = 1) {
        if (auth.isAuthenticated) {
            cart.value = await cartApi.add(variantId, quantity);
            return;
        }
        const lines = [...guestLines.value];
        const existing = lines.find((l) => l.variantId === variantId);
        if (existing)
            existing.quantity = Math.min(MAX_PER_LINE, existing.quantity + quantity);
        else
            lines.unshift({ variantId, quantity });
        const priced = await cartApi.preview(lines);
        const line = priced.items.find((i) => i.variantId === variantId);
        if (line && !line.purchasable) {
            throw new Error(line.issues.includes('LIMITED_STOCK') ? `Only ${line.maxQuantity} left in stock.` : 'This item is out of stock.');
        }
        setGuest(lines);
        cart.value = priced;
    }
    async function setQuantity(key, quantity) {
        return track(key.variantId, async () => {
            if (auth.isAuthenticated && key.id)
                cart.value = await cartApi.update(key.id, { quantity });
            else {
                setGuest(guestLines.value.map((l) => (l.variantId === key.variantId ? { ...l, quantity } : l)));
                cart.value = await cartApi.preview(guestLines.value);
            }
        });
    }
    async function changeVariant(itemId, variantId) {
        cart.value = await cartApi.update(itemId, { variantId });
    }
    async function remove(key) {
        return track(key.variantId, async () => {
            if (auth.isAuthenticated && key.id)
                cart.value = await cartApi.remove(key.id);
            else {
                setGuest(guestLines.value.filter((l) => l.variantId !== key.variantId));
                cart.value = guestLines.value.length ? await cartApi.preview(guestLines.value) : null;
            }
        });
    }
    async function saveForLater(itemId) {
        return track(itemId, async () => void (cart.value = await cartApi.saveForLater(itemId)));
    }
    async function moveToCart(itemId) {
        return track(itemId, async () => void (cart.value = await cartApi.moveToCart(itemId)));
    }
    async function applyCoupon(code) {
        cart.value = await cartApi.applyCoupon(code);
    }
    async function removeCoupon() {
        cart.value = await cartApi.removeCoupon();
    }
    async function track(key, fn) {
        pending.value = new Set(pending.value).add(key);
        try {
            await fn();
        }
        finally {
            const next = new Set(pending.value);
            next.delete(key);
            pending.value = next;
        }
    }
    /** Called when the signed-in user changes: merge guest lines on sign-in, reset on sign-out. */
    async function onAuthChange(signedIn) {
        if (signedIn && guestLines.value.length) {
            try {
                cart.value = await cartApi.merge(guestLines.value);
                setGuest([]);
                return;
            }
            catch {
                // fall through to a plain load
            }
        }
        cart.value = null;
        await load().catch(() => undefined);
    }
    return {
        cart,
        count,
        loading,
        loaded,
        pending,
        load,
        add,
        setQuantity,
        changeVariant,
        remove,
        saveForLater,
        moveToCart,
        applyCoupon,
        removeCoupon,
        onAuthChange,
    };
});
