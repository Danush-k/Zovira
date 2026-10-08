import { onBeforeUnmount, onMounted } from 'vue';
export function useClickOutside(targets, handler) {
    function listener(event) {
        const path = event.composedPath();
        if (targets.some((t) => t.value && path.includes(t.value)))
            return;
        handler(event);
    }
    onMounted(() => document.addEventListener('pointerdown', listener, true));
    onBeforeUnmount(() => document.removeEventListener('pointerdown', listener, true));
}
