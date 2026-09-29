import { shallowRef, ref, type Ref, type ShallowRef } from 'vue'

export interface AsyncState<T, A extends unknown[]> {
  data: ShallowRef<T | null>
  error: Ref<unknown>
  loading: Ref<boolean>
  execute: (...args: A) => Promise<T | null>
}

/**
 * Tracks loading / error / data for an async call. Only the latest invocation may commit its
 * result, so rapid re-fetches (filters, pagination) never render stale data.
 */
export function useAsync<T, A extends unknown[] = []>(
  fn: (...args: A) => Promise<T>,
  options: { immediate?: boolean; initial?: T | null } = {},
): AsyncState<T, A> {
  const data = shallowRef<T | null>(options.initial ?? null)
  const error = ref<unknown>(null)
  const loading = ref(false)
  let callId = 0

  async function execute(...args: A): Promise<T | null> {
    const id = ++callId
    loading.value = true
    error.value = null
    try {
      const result = await fn(...args)
      if (id === callId) data.value = result
      return result
    } catch (e) {
      if (id === callId) error.value = e
      return null
    } finally {
      if (id === callId) loading.value = false
    }
  }

  if (options.immediate) void execute(...([] as unknown as A))

  return { data, error, loading, execute }
}
