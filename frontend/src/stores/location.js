import { defineStore } from 'pinia';
import { ref, watch } from 'vue';
import { catalogApi } from '@/services/catalog';
const KEY = 'zovira.pincode';
function load() {
    try {
        return localStorage.getItem(KEY);
    }
    catch {
        return null;
    }
}
/** The shopper's delivery PIN code, remembered on this device and used for delivery estimates. */
export const useLocationStore = defineStore('location', () => {
    const pincode = ref(load());
    const estimate = ref(null);
    const loading = ref(false);
    const error = ref(null);
    watch(pincode, (value) => {
        try {
            if (value)
                localStorage.setItem(KEY, value);
            else
                localStorage.removeItem(KEY);
        }
        catch {
            // storage unavailable; keep in memory only
        }
    });
    async function setPincode(value) {
        loading.value = true;
        error.value = null;
        try {
            const result = await catalogApi.deliveryEstimate(value);
            pincode.value = value;
            estimate.value = result;
            return result;
        }
        finally {
            loading.value = false;
        }
    }
    async function refresh() {
        if (!pincode.value || estimate.value?.pincode === pincode.value)
            return estimate.value;
        try {
            estimate.value = await catalogApi.deliveryEstimate(pincode.value);
        }
        catch {
            estimate.value = null;
        }
        return estimate.value;
    }
    return { pincode, estimate, loading, error, setPincode, refresh };
});
