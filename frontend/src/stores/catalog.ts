import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { catalogApi } from '@/services/catalog'
import type { CategoryNode, PlatformSettings } from '@/types/catalog'

/** Long-lived storefront reference data: the category tree and public platform settings. */
export const useCatalogStore = defineStore('catalog', () => {
  const categories = ref<CategoryNode[]>([])
  const settings = ref<PlatformSettings | null>(null)
  let categoriesPromise: Promise<CategoryNode[]> | null = null
  let settingsPromise: Promise<PlatformSettings | null> | null = null

  function loadCategories(): Promise<CategoryNode[]> {
    if (!categoriesPromise) {
      categoriesPromise = catalogApi
        .categories()
        .then((tree) => (categories.value = tree))
        .catch((e) => {
          categoriesPromise = null
          throw e
        })
    }
    return categoriesPromise
  }

  function loadSettings(): Promise<PlatformSettings | null> {
    if (!settingsPromise) {
      settingsPromise = catalogApi
        .settings()
        .then((s) => (settings.value = s))
        .catch(() => {
          settingsPromise = null
          return null
        })
    }
    return settingsPromise
  }

  const flat = computed(() => {
    const out: (CategoryNode & { parent: CategoryNode | null })[] = []
    const walk = (nodes: CategoryNode[], parent: CategoryNode | null) =>
      nodes.forEach((n) => {
        out.push({ ...n, parent })
        walk(n.children, n)
      })
    walk(categories.value, null)
    return out
  })

  function findBySlug(slug: string) {
    return flat.value.find((c) => c.slug === slug) ?? null
  }

  return { categories, settings, loadCategories, loadSettings, flat, findBySlug }
})
