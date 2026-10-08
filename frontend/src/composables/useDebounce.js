import { onBeforeUnmount, ref, watch } from 'vue';
export function useDebouncedRef(source, delay = 250) {
    const debounced = ref(source.value);
    let timer;
    watch(source, (value) => {
        clearTimeout(timer);
        timer = setTimeout(() => (debounced.value = value), delay);
    });
    onBeforeUnmount(() => clearTimeout(timer));
    return debounced;
}
export function debounce(fn, delay = 250) {
    let timer;
    const wrapped = (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
    wrapped.cancel = () => clearTimeout(timer);
    return wrapped;
}
