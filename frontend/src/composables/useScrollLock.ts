import { onBeforeUnmount, watch, type Ref } from 'vue'

let locks = 0
let savedOverflow = ''
let savedPaddingRight = ''

function lock() {
  if (locks++ > 0) return
  const scrollbar = window.innerWidth - document.documentElement.clientWidth
  savedOverflow = document.body.style.overflow
  savedPaddingRight = document.body.style.paddingRight
  document.body.style.overflow = 'hidden'
  if (scrollbar > 0) document.body.style.paddingRight = `${scrollbar}px`
}

function unlock() {
  if (locks === 0 || --locks > 0) return
  document.body.style.overflow = savedOverflow
  document.body.style.paddingRight = savedPaddingRight
}

/** Reference-counted body scroll lock so nested overlays do not fight each other. */
export function useScrollLock(active: Ref<boolean>) {
  let held = false
  watch(
    active,
    (value) => {
      if (value && !held) {
        lock()
        held = true
      } else if (!value && held) {
        unlock()
        held = false
      }
    },
    { immediate: true },
  )
  onBeforeUnmount(() => {
    if (held) unlock()
  })
}
