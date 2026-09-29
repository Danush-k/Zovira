import { http } from './http'
import type {
  Brand,
  CategoryNode,
  DeliveryEstimate,
  HomeData,
  PlatformSettings,
  ProductDetail,
  Store,
} from '@/types/catalog'

export const catalogApi = {
  home: () => http.get<HomeData>('/home').then((r) => r.data),

  categories: () => http.get<CategoryNode[]>('/categories').then((r) => r.data),

  brands: () => http.get<Brand[]>('/brands').then((r) => r.data),

  brand: (slug: string) => http.get<Brand>(`/brands/${encodeURIComponent(slug)}`).then((r) => r.data),

  product: (slug: string) => http.get<ProductDetail>(`/products/${encodeURIComponent(slug)}`).then((r) => r.data),

  compare: (slugs: string[]) =>
    http.get<ProductDetail[]>('/products/compare', { params: { slugs: slugs.join(',') } }).then((r) => r.data),

  store: (slug: string) => http.get<Store>(`/stores/${encodeURIComponent(slug)}`).then((r) => r.data),

  deliveryEstimate: (pincode: string) =>
    http.get<DeliveryEstimate>('/delivery/estimate', { params: { pincode } }).then((r) => r.data),

  settings: () => http.get<PlatformSettings>('/settings/public').then((r) => r.data),
}
