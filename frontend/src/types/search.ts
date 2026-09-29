import type { ProductSummary } from './catalog'
import type { Page } from './api'

export interface FacetValue {
  slug: string
  name: string
  count: number
}

export interface CountBucket {
  value: number
  count: number
}

export interface SearchFacets {
  categories: FacetValue[]
  brands: FacetValue[]
  minPrice: number | null
  maxPrice: number | null
  ratings: CountBucket[]
  discounts: CountBucket[]
  inStockCount: number
  with3dCount: number
}

export interface SearchResult {
  results: Page<ProductSummary>
  facets: SearchFacets
  sort: string
}

export interface SearchParams {
  q?: string
  category?: string
  brand?: string
  seller?: string
  minPrice?: number
  maxPrice?: number
  rating?: number
  discount?: number
  inStock?: boolean
  has3d?: boolean
  sort?: string
  page?: number
  size?: number
}

export interface Suggestions {
  products: { slug: string; title: string; imageUrl: string | null; price: number }[]
  categories: { slug: string; name: string }[]
  brands: { slug: string; name: string }[]
  queries: string[]
}
