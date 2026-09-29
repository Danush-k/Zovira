import { onBeforeUnmount, ref, watch, type Ref } from 'vue'

export function useDebouncedRef<T>(source: Ref<T>, delay = 250): Ref<T> {
  const debounced = ref(source.value) as Ref<T>
  let timer: ReturnType<typeof setTimeout> | undefined
  watch(source, (value) => {
    clearTimeout(timer)
    timer = setTimeout(() => (debounced.value = value), delay)
  })
  onBeforeUnmount(() => clearTimeout(timer))
  return debounced
}

export function debounce<A extends unknown[]>(fn: (...args: A) => void, delay = 250) {
  let timer: ReturnType<typeof setTimeout> | undefined
  const wrapped = (...args: A) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }
  wrapped.cancel = () => clearTimeout(timer)
  return wrapped
}
