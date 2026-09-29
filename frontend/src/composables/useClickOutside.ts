import { onBeforeUnmount, onMounted, type Ref } from 'vue'

export function useClickOutside(targets: Ref<HTMLElement | null>[], handler: (event: PointerEvent) => void) {
  function listener(event: PointerEvent) {
    const path = event.composedPath()
    if (targets.some((t) => t.value && path.includes(t.value))) return
    handler(event)
  }
  onMounted(() => document.addEventListener('pointerdown', listener, true))
  onBeforeUnmount(() => document.removeEventListener('pointerdown', listener, true))
}
