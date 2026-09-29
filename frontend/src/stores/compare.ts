import { defineStore } from 'pinia'
import { computed, ref, watch } from 'vue'

export interface CompareItem {
  slug: string
  title: string
  imageUrl: string | null
  categorySlug: string
}

const KEY = 'zovira.compare'
export const COMPARE_LIMIT = 4

function load(): CompareItem[] {
  try {
    const raw = localStorage.getItem(KEY)
    return raw ? (JSON.parse(raw) as CompareItem[]).slice(0, COMPARE_LIMIT) : []
  } catch {
    return []
  }
}

/** Up to four products shortlisted for side-by-side comparison, kept on this device. */
export const useCompareStore = defineStore('compare', () => {
  const items = ref<CompareItem[]>(load())

  watch(
    items,
    (value) => {
      try {
        localStorage.setItem(KEY, JSON.stringify(value))
      } catch {
        // ignore persistence failures
      }
    },
    { deep: true },
  )

  const count = computed(() => items.value.length)
  const has = (slug: string) => items.value.some((i) => i.slug === slug)

  /** Returns false when the list is full. */
  function toggle(item: CompareItem): boolean {
    if (has(item.slug)) {
      items.value = items.value.filter((i) => i.slug !== item.slug)
      return true
    }
    if (items.value.length >= COMPARE_LIMIT) return false
    items.value = [...items.value, item]
    return true
  }

  function remove(slug: string) {
    items.value = items.value.filter((i) => i.slug !== slug)
  }

  function clear() {
    items.value = []
  }

  return { items, count, has, toggle, remove, clear }
})
