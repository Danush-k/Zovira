import { http } from './http'
import type { Page } from '@/types/api'
import type { OrderDetail, OrderSummary } from '@/types/order'

export interface ReturnItem {
  id: number
  returnNumber: string
  orderNumber: string
  itemTitle: string
  imageUrl: string | null
  productSlug: string
  quantity: number
  reason: string
  comments: string | null
  status: 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'PICKED_UP' | 'REFUNDED'
  refundAmount: number
  resolutionNote: string | null
  customerName: string
  sellerName: string
  createdAt: string
  resolvedAt: string | null
}

export const RETURN_REASONS = [
  { value: 'DAMAGED', label: 'Arrived damaged' },
  { value: 'DEFECTIVE', label: "Defective or doesn't work" },
  { value: 'WRONG_ITEM', label: 'Received the wrong item' },
  { value: 'NOT_AS_DESCRIBED', label: 'Not as described' },
  { value: 'SIZE_FIT', label: "Size or fit isn't right" },
  { value: 'CHANGED_MIND', label: 'No longer needed' },
  { value: 'OTHER', label: 'Other' },
] as const

export const ordersApi = {
  list: (filter: string, page = 0) =>
    http.get<Page<OrderSummary>>('/orders', { params: { filter, page, size: 10 } }).then((r) => r.data),
  detail: (orderNumber: string) => http.get<OrderDetail>(`/orders/${encodeURIComponent(orderNumber)}`).then((r) => r.data),
  cancel: (orderNumber: string, reason: string) =>
    http.post<OrderDetail>(`/orders/${encodeURIComponent(orderNumber)}/cancel`, { reason }).then((r) => r.data),
  reorder: (orderNumber: string) =>
    http.post<{ added: number; unavailable: string[] }>(`/orders/${encodeURIComponent(orderNumber)}/reorder`).then((r) => r.data),
  async downloadInvoice(orderNumber: string) {
    const res = await http.get<Blob>(`/orders/${encodeURIComponent(orderNumber)}/invoice`, { responseType: 'blob' })
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = `Zovira-invoice-${orderNumber}.pdf`
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  },
  requestReturn: (orderNumber: string, itemId: number, body: { quantity: number; reason: string; comments?: string }) =>
    http.post<ReturnItem>(`/orders/${encodeURIComponent(orderNumber)}/items/${itemId}/return`, body).then((r) => r.data),
  returns: (page = 0) => http.get<Page<ReturnItem>>('/returns', { params: { page } }).then((r) => r.data),
}
