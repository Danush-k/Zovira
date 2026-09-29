import { http } from './http'
import type { Page } from '@/types/api'
import type { ReturnItem } from './orders'
import type {
  InventoryRow,
  SellerApplication,
  SellerDashboard,
  SellerOrderRow,
  SellerProductInput,
  SellerProductSummary,
  SellerProfile,
} from '@/types/seller'

export const sellerApi = {
  application: () => http.get<SellerProfile | ''>('/sell/application').then((r) => r.data || null),
  apply: (body: SellerApplication) => http.post<SellerProfile>('/sell/application', body).then((r) => r.data),
  profile: () => http.get<SellerProfile>('/seller/profile').then((r) => r.data),
  updateProfile: (body: SellerApplication) => http.put<SellerProfile>('/seller/profile', body).then((r) => r.data),
  dashboard: () => http.get<SellerDashboard>('/seller/dashboard').then((r) => r.data),
  products: (params: { status?: string; q?: string; page?: number; size?: number }) =>
    http.get<Page<SellerProductSummary>>('/seller/products', { params }).then((r) => r.data),
  createProduct: (body: SellerProductInput) => http.post<SellerProductSummary>('/seller/products', body).then((r) => r.data),
  updateProduct: (id: number, body: SellerProductInput) =>
    http.put<SellerProductSummary>(`/seller/products/${id}`, body).then((r) => r.data),
  setProductStatus: (id: number, status: string) =>
    http.patch<SellerProductSummary>(`/seller/products/${id}/status`, { status }).then((r) => r.data),
  inventory: (lowOnly = false) => http.get<InventoryRow[]>('/seller/inventory', { params: { lowOnly } }).then((r) => r.data),
  updateStock: (rows: { variantId: number; available: number; lowStockThreshold?: number }[]) =>
    http.put<InventoryRow[]>('/seller/inventory', { rows }).then((r) => r.data),
  orders: (filter: string, page = 0) =>
    http.get<Page<SellerOrderRow>>('/seller/orders', { params: { filter, page, size: 20 } }).then((r) => r.data),
  updateShipment: (id: number, body: { status: string; carrier?: string; trackingNumber?: string; location?: string; note?: string }) =>
    http.patch<SellerOrderRow>(`/seller/shipments/${id}`, body).then((r) => r.data),
  returns: (openOnly = true, page = 0) =>
    http.get<Page<ReturnItem>>('/seller/returns', { params: { openOnly, page } }).then((r) => r.data),
  decideReturn: (id: number, decision: 'APPROVE' | 'REJECT' | 'RECEIVE', note?: string) =>
    http.post<ReturnItem>(`/seller/returns/${id}/decision`, { decision, note }).then((r) => r.data),
}
