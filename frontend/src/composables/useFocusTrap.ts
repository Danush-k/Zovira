import { nextTick, onBeforeUnmount, watch, type Ref } from 'vue'

const FOCUSABLE = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled]):not([type="hidden"])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(',')

/**
 * Keeps keyboard focus inside `container` while `active` is true and restores focus to the
 * previously focused element when it deactivates.
 */
export function useFocusTrap(container: Ref<HTMLElement | null>, active: Ref<boolean>) {
  let previouslyFocused: HTMLElement | null = null

  function focusables(): HTMLElement[] {
    if (!container.value) return []
    return Array.from(container.value.querySelectorAll<HTMLElement>(FOCUSABLE)).filter(
      (el) => el.offsetParent !== null || el === document.activeElement,
    )
  }

  function onKeydown(e: KeyboardEvent) {
    if (e.key !== 'Tab' || !container.value) return
    const items = focusables()
    if (items.length === 0) {
      e.preventDefault()
      container.value.focus()
      return
    }
    const first = items[0]!
    const last = items[items.length - 1]!
    if (e.shiftKey && document.activeElement === first) {
      e.preventDefault()
      last.focus()
    } else if (!e.shiftKey && document.activeElement === last) {
      e.preventDefault()
      first.focus()
    }
  }

  watch(
    active,
    async (isActive) => {
      if (isActive) {
        previouslyFocused = document.activeElement as HTMLElement | null
        await nextTick()
        const autofocus = container.value?.querySelector<HTMLElement>('[autofocus],[data-autofocus]')
        ;(autofocus ?? focusables()[0] ?? container.value)?.focus()
        document.addEventListener('keydown', onKeydown)
      } else {
        document.removeEventListener('keydown', onKeydown)
        previouslyFocused?.focus?.()
        previouslyFocused = null
      }
    },
    { immediate: true },
  )

  onBeforeUnmount(() => document.removeEventListener('keydown', onKeydown))
}
