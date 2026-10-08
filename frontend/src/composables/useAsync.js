import { shallowRef, ref } from 'vue';
/**
 * Tracks loading / error / data for an async call. Only the latest invocation may commit its
 * result, so rapid re-fetches (filters, pagination) never render stale data.
 */
export function useAsync(fn, options = {}) {
    const data = shallowRef(options.initial ?? null);
    const error = ref(null);
    const loading = ref(false);
    let callId = 0;
    async function execute(...args) {
        const id = ++callId;
        loading.value = true;
        error.value = null;
        try {
            const result = await fn(...args);
            if (id === callId)
                data.value = result;
            return result;
        }
        catch (e) {
            if (id === callId)
                error.value = e;
            return null;
        }
        finally {
            if (id === callId)
                loading.value = false;
        }
    }
    if (options.immediate)
        void execute(...[]);
    return { data, error, loading, execute };
}
