import { onBeforeUnmount, ref } from 'vue'

export function useMediaQuery(query: string) {
  const matches = ref(typeof window !== 'undefined' && window.matchMedia?.(query).matches)
  if (typeof window === 'undefined' || !window.matchMedia) return matches

  const mql = window.matchMedia(query)
  const update = (e: MediaQueryListEvent) => (matches.value = e.matches)
  mql.addEventListener('change', update)
  onBeforeUnmount(() => mql.removeEventListener('change', update))
  return matches
}

export const useIsDesktop = () => useMediaQuery('(min-width: 1024px)')
