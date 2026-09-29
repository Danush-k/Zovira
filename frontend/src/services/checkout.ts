import { http } from './http'
import type { CheckoutPreview, DeliveryOption, OrderDetail, PaymentIntent, PaymentMethod, PaymentResult, PlaceOrderResponse } from '@/types/order'

export interface CheckoutInput {
  addressId: number
  deliveryOption: DeliveryOption
  paymentMethod?: PaymentMethod
}

export const checkoutApi = {
  preview: (input: CheckoutInput) => http.post<CheckoutPreview>('/checkout/preview', input).then((r) => r.data),
  place: (input: CheckoutInput, idempotencyKey: string) =>
    http.post<PlaceOrderResponse>('/orders', input, { headers: { 'Idempotency-Key': idempotencyKey } }).then((r) => r.data),
  paymentConfig: () => http.get<{ provider: string; keyId: string | null; sandbox: boolean }>('/payments/config').then((r) => r.data),
  verifyRazorpay: (body: { razorpayOrderId: string; razorpayPaymentId: string; razorpaySignature: string }) =>
    http.post<PaymentResult>('/payments/razorpay/verify', body).then((r) => r.data),
  completeSandbox: (paymentId: number, success: boolean, method: PaymentMethod) =>
    http.post<PaymentResult>(`/payments/sandbox/${paymentId}/complete`, { success, method }).then((r) => r.data),
  retry: (orderNumber: string, method: PaymentMethod) =>
    http.post<PaymentIntent>(`/payments/orders/${orderNumber}/retry`, { method }).then((r) => r.data),
  order: (orderNumber: string) => http.get<OrderDetail>(`/orders/${encodeURIComponent(orderNumber)}`).then((r) => r.data),
}
