import { defineStore } from 'pinia';
import { ref } from 'vue';
let nextId = 1;
export const useToastStore = defineStore('toast', () => {
    const toasts = ref([]);
    const timers = new Map();
    function dismiss(id) {
        toasts.value = toasts.value.filter((t) => t.id !== id);
        clearTimeout(timers.get(id));
        timers.delete(id);
    }
    function push(toast) {
        const id = nextId++;
        const duration = toast.duration ?? (toast.tone === 'error' ? 6000 : 4000);
        toasts.value = [...toasts.value.slice(-3), { ...toast, id, duration }];
        if (duration > 0)
            timers.set(id, setTimeout(() => dismiss(id), duration));
        return id;
    }
    const success = (title, message, action) => push({ tone: 'success', title, message, action });
    const error = (title, message) => push({ tone: 'error', title, message });
    const info = (title, message, action) => push({ tone: 'info', title, message, action });
    const warning = (title, message) => push({ tone: 'warning', title, message });
    return { toasts, push, dismiss, success, error, info, warning };
});
